package ai.kaios

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
)

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
    private val executorFactory: (Int) -> ExecutorService = { size -> Executors.newFixedThreadPool(size.coerceAtLeast(1)) },
) {
    fun run(workflow: Workflow, input: String, runId: RunId = RunId.new()): WorkflowResult {
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

            val executor = executorFactory(ready.size)
            try {
                val futures = ready.associateWith { node ->
                    executor.submit(Callable { executeNode(node, input, completed.toMap(), runId) })
                }

                for ((node, future) in futures) {
                    val result = runCatching { future.get() }
                        .getOrElse { error ->
                            val root = error.cause ?: error
                            val fallback = node.fallback
                            if (fallback == null) {
                                success = false
                                failureOutput = root.message ?: "Node '${node.id}' failed."
                                NodeResult(
                                    nodeId = node.id,
                                    agent = node.agent.id,
                                    pid = ProcessId(1),
                                    output = "",
                                    success = false,
                                    error = failureOutput,
                                )
                            } else {
                                val fallbackNode = nodesById.getValue(fallback)
                                val fallbackResult = executeNode(
                                    node = fallbackNode,
                                    input = "$input\nfallback from ${node.id}: ${root.message}",
                                    completed = completed.toMap(),
                                    runId = runId,
                                )
                                fallbackResult.copy(nodeId = node.id, fallbackNodeId = fallback)
                            }
                        }

                    if (success || result.success) {
                        completed[node.id] = result
                    }
                    pending.remove(node.id)
                }
            } finally {
                executor.shutdownNow()
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

    private fun executeNode(
        node: WorkflowNode,
        input: String,
        completed: Map<String, NodeResult>,
        runId: RunId,
    ): NodeResult {
        val process = runtime.spawn(node.agent, runId)
        runtime.start(process.pid)

        return runCatching {
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
        }.getOrElse { error ->
            runtime.fail(process.pid, error.message ?: "Node '${node.id}' failed.")
            throw error
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
