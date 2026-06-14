package ai.kaios

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import java.time.Duration

data class Workflow(
    val name: String,
    val nodes: List<WorkflowNode>,
) {
    init {
        require(name.isNotBlank()) { "Workflow name cannot be blank." }
        require(nodes.isNotEmpty()) { "Workflow must contain at least one node." }

        val ids = nodes.map { it.id }
        require(ids.size == ids.toSet().size) { "Workflow node ids must be unique." }

        val known = ids.toSet()
        nodes.forEach { node ->
            require(node.dependencies.all { it in known }) { "Node '${node.id}' depends on an unknown node." }
            node.fallback?.let { require(it in known) { "Node '${node.id}' references an unknown fallback node '$it'." } }
        }
    }
}

data class WorkflowNode(
    val id: String,
    val agent: AgentSpec,
    val dependencies: Set<String> = emptySet(),
    val fallback: String? = null,
    val fallbackOnly: Boolean = false,
    val maxAttempts: Int = 1,
    val retryBackoff: Duration = Duration.ZERO,
) {
    init {
        require(maxAttempts >= 1) { "Workflow node '$id' must have at least one attempt." }
        require(!retryBackoff.isNegative) { "Workflow node '$id' retry backoff cannot be negative." }
    }
}

data class NodeResult(
    val nodeId: String,
    val agent: AgentId,
    val pid: ProcessId,
    val output: String,
    val success: Boolean,
    val error: String? = null,
    val fallbackNodeId: String? = null,
)

data class WorkflowResult(
    val runId: RunId,
    val workflowName: String,
    val success: Boolean,
    val outputs: Map<String, NodeResult>,
    val finalOutput: String,
    val processes: List<AgentProcess>,
    val events: List<RuntimeEvent>,
)

class WorkflowScheduler(
    private val runtime: AgentRuntime,
    private val modelProvider: ModelProvider,
    private val tools: ToolRegistry = ToolRegistry.Empty,
    private val memory: MemoryStore = NoopMemoryStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val nodeTimeout: Duration? = null,
    private val onReadyBatch: (Int) -> Unit = {},
) {
    fun run(workflow: Workflow, input: String, runId: RunId = RunId.new()): WorkflowResult =
        runBlocking {
            runSuspend(workflow, input, runId)
        }

    suspend fun runSuspend(workflow: Workflow, input: String, runId: RunId = RunId.new()): WorkflowResult {
        val nodesById = workflow.nodes.associateBy { it.id }
        val pending = workflow.nodes.filterNot { it.fallbackOnly }.mapTo(linkedSetOf()) { it.id }
        val completed = linkedMapOf<String, NodeResult>()
        var success = true
        var failureOutput = ""

        while (pending.isNotEmpty() && success) {
            val ready = pending
                .map { nodesById.getValue(it) }
                .filter { node -> node.dependencies.all { it in completed } }

            if (ready.isEmpty()) {
                success = false
                failureOutput = "Workflow '${workflow.name}' has unresolved dependencies or a cycle."
                break
            }

            onReadyBatch(ready.size)
            val batchResults = executeReadyBatch(ready, nodesById, input, completed.toMap(), runId)

            for ((node, result) in batchResults) {
                if (!result.success) {
                    success = false
                    failureOutput = result.error ?: "Node '${node.id}' failed."
                    break
                }

                completed[node.id] = result
                pending.remove(node.id)
            }
        }

        val finalOutput = when {
            success && completed.isNotEmpty() -> completed.values.last().output
            failureOutput.isNotBlank() -> failureOutput
            else -> "Workflow '${workflow.name}' did not produce output."
        }

        return WorkflowResult(
            runId = runId,
            workflowName = workflow.name,
            success = success,
            outputs = completed.toMap(),
            finalOutput = finalOutput,
            processes = runtime.processes(runId),
            events = runtime.events(runId),
        )
    }

    private suspend fun executeReadyBatch(
        ready: List<WorkflowNode>,
        nodesById: Map<String, WorkflowNode>,
        input: String,
        completed: Map<String, NodeResult>,
        runId: RunId,
    ): Map<WorkflowNode, NodeResult> = supervisorScope {
        val deferreds: Map<WorkflowNode, Deferred<NodeResult>> = ready.associateWith { node ->
            async {
                executeNodeWithPolicy(node, input, completed, runId)
            }
        }
        val results = linkedMapOf<WorkflowNode, NodeResult>()

        for ((node, deferred) in deferreds) {
            val result = try {
                deferred.await()
            } catch (error: Throwable) {
                val fallback = node.fallback
                if (fallback == null) {
                    val message = error.message ?: "Node '${node.id}' failed."
                    cancelRemaining(deferreds, deferred)
                    NodeResult(
                        nodeId = node.id,
                        agent = node.agent.id,
                        pid = ProcessId(1),
                        output = "",
                        success = false,
                        error = message,
                    )
                } else {
                    val fallbackNode = nodesById.getValue(fallback)
                    val fallbackResult = executeNodeWithPolicy(
                        node = fallbackNode,
                        input = "$input\nfallback from ${node.id}: ${error.message}",
                        completed = completed,
                        runId = runId,
                    )
                    fallbackResult.copy(nodeId = node.id, fallbackNodeId = fallback)
                }
            }

            results[node] = result
            if (!result.success) break
        }

        results
    }

    private suspend fun executeNodeWithPolicy(
        node: WorkflowNode,
        input: String,
        completed: Map<String, NodeResult>,
        runId: RunId,
    ): NodeResult {
        var attempt = 1
        while (true) {
            val block: suspend () -> NodeResult = {
                runInterruptible(dispatcher) {
                    executeNode(node, input, completed, runId)
                }
            }

            try {
                return if (nodeTimeout == null) {
                    block()
                } else {
                    withTimeout(nodeTimeout.toMillis()) {
                        block()
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: NodeExecutionException) {
                if (attempt >= node.maxAttempts) throw error
                val nextAttempt = attempt + 1
                runtime.recordRetry(error.pid, nextAttempt, node.maxAttempts, error.message ?: "node failed")
                if (!node.retryBackoff.isZero) {
                    delay(node.retryBackoff.toMillis())
                }
                attempt = nextAttempt
            }
        }
    }

    private suspend fun cancelRemaining(
        deferreds: Map<WorkflowNode, Deferred<NodeResult>>,
        completedDeferred: Deferred<NodeResult>,
    ) {
        deferreds.values
            .filter { deferred -> deferred !== completedDeferred && !deferred.isCompleted }
            .forEach { deferred -> deferred.cancelAndJoin() }
    }

    private fun executeNode(
        node: WorkflowNode,
        input: String,
        completed: Map<String, NodeResult>,
        runId: RunId,
    ): NodeResult {
        val process = runtime.spawn(node.agent, runId)
        runtime.start(process.pid)

        return try {
            appendMemory(process.pid, runId, node.agent, "user", input)

            val dependencyContext = node.dependencies.associateWith { dependency ->
                completed[dependency]?.output.orEmpty()
            }
            val history = if (node.agent.memoryEnabled) memory.read(runId, node.agent.id) else emptyList()

            val response = modelProvider.complete(
                ModelRequest(
                    runId = runId,
                    agent = node.agent,
                    input = input,
                    dependencyContext = dependencyContext,
                    memory = history,
                    availableTools = node.agent.allowedTools.intersect(tools.names),
                ),
            )

            val toolResults = response.toolCalls.map { call ->
                tools.execute(node.agent, call).also { result -> runtime.recordSyscall(process.pid, result) }
            }

            val failedTool = toolResults.firstOrNull { !it.ok }
            if (failedTool != null) {
                error(failedTool.error ?: "Tool '${failedTool.tool}' failed.")
            }

            val output = buildString {
                append(response.content)
                if (toolResults.isNotEmpty()) {
                    appendLine()
                    toolResults.forEach { result -> appendLine("syscall ${result.tool}: ${result.output}") }
                }
            }.trim()

            appendMemory(process.pid, runId, node.agent, "assistant", output)

            val contextSize = history.sumOf { it.content.length } +
                input.length +
                dependencyContext.values.sumOf { it.length } +
                output.length

            runtime.succeed(process.pid, response.tokenUsage, contextSize)

            NodeResult(
                nodeId = node.id,
                agent = node.agent.id,
                pid = process.pid,
                output = output,
                success = true,
            )
        } catch (error: CancellationException) {
            runtime.cancel(process.pid)
            throw error
        } catch (error: Throwable) {
            val message = error.message ?: "Node '${node.id}' failed."
            runtime.fail(process.pid, message)
            throw NodeExecutionException(process.pid, message, error)
        }
    }

    private fun appendMemory(pid: ProcessId, runId: RunId, agent: AgentSpec, role: String, content: String) {
        if (!agent.memoryEnabled) return

        val entry = MemoryEntry(
            runId = runId,
            agent = agent.id,
            role = role,
            content = content,
            timestamp = java.time.Instant.now(),
        )
        memory.append(entry)
        runtime.recordMemory(pid, entry)
    }
}

private class NodeExecutionException(
    val pid: ProcessId,
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)
