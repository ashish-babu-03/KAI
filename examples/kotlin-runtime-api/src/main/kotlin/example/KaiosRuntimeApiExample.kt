package example

import ai.kaios.AgentRuntime
import ai.kaios.ClockTool
import ai.kaios.EchoTool
import ai.kaios.LocalWorkerExecutorBackend
import ai.kaios.MemoryIsolation
import ai.kaios.MockHttpTool
import ai.kaios.MockModelProvider
import ai.kaios.ProcessRecoveryPolicy
import ai.kaios.RuntimeEventType
import ai.kaios.ScopedFileTool
import ai.kaios.SessionMemoryStore
import ai.kaios.ToolCapabilityLimits
import ai.kaios.ToolCostProfile
import ai.kaios.ToolPermission
import ai.kaios.ToolRegistry
import ai.kaios.WorkflowScheduler
import ai.kaios.agent
import ai.kaios.workflow
import java.nio.file.Paths

fun main() {
    val memory = SessionMemoryStore()
    val runtime = AgentRuntime()
    val tools = ToolRegistry(
        listOf(
            EchoTool(),
            ClockTool(),
            MockHttpTool(),
            ScopedFileTool(Paths.get(".").toAbsolutePath().normalize()),
        ),
    )

    val researcher = agent("researcher") {
        instruction("Gather deterministic context for the change.")
        memory(memory)
        capability(
            tool = "mock-http",
            permission = ToolPermission.NETWORK,
            scope = "mock://kaios/research",
            limits = ToolCapabilityLimits(maxCalls = 1),
            cost = ToolCostProfile(estimatedMicros = 0),
        )
    }

    val inspector = agent("inspector") {
        instruction("Inspect local code surfaces through a scoped file syscall.")
        memory(memory)
        capability(
            tool = "file",
            permission = ToolPermission.FILE,
            scope = ".",
            limits = ToolCapabilityLimits(maxCalls = 1),
        )
    }

    val reviewer = agent("reviewer") {
        instruction("Prioritize review risks from researcher and inspector evidence.")
        memory(memory)
        capability(
            tool = "echo",
            permission = ToolPermission.ECHO,
            scope = "*",
            limits = ToolCapabilityLimits(maxCalls = 1),
        )
    }

    val validator = agent("validator") {
        instruction("Validate the review output and emit a final evidence handoff.")
        memory(memory)
        capability(
            tool = "echo",
            permission = ToolPermission.ECHO,
            scope = "*",
            limits = ToolCapabilityLimits(maxCalls = 1),
        )
    }

    val reviewWorkflow = workflow("kotlin-runtime-api-review") {
        node("researcher", researcher)
            .priority(20)
            .recovery(ProcessRecoveryPolicy(maxRestarts = 1, memoryIsolation = MemoryIsolation.PROCESS))

        node("inspector", inspector)
            .priority(10)

        node("reviewer", reviewer)
            .dependsOn("researcher", "inspector")
            .priority(5)

        node("validator", validator)
            .dependsOn("reviewer")
            .triggeredBy(RuntimeEventType.SUCCEEDED, agent = reviewer.id)
    }

    val scheduler = WorkflowScheduler(
        runtime = runtime,
        modelProvider = MockModelProvider(),
        tools = tools,
        memory = memory,
        executorBackend = LocalWorkerExecutorBackend(parallelism = 2),
    )

    val result = scheduler.run(
        workflow = reviewWorkflow,
        input = "Review a payment retry change for runtime evidence, syscall safety, and CI gating.",
    )

    println("KAI OS Kotlin Runtime API demo")
    println("run=${result.runId.value} workflow=${result.workflowName} success=${result.success}")
    println(
        "scheduler backend=${result.scheduler.executorBackend} " +
            "priority=${result.scheduler.priorityEnabled} recovery=${result.scheduler.recoveryEnabled} triggers=${result.scheduler.triggerCount}",
    )
    println()
    println("PID  AGENT       STATE      TOKENS  MEM  SYSCALLS  TOOL_MS  COST  WORKER")
    result.processes.sortedBy { it.pid.value }.forEach { process ->
        println(
            listOf(
                process.pid.value.toString().padEnd(4),
                process.agent.value.padEnd(11),
                process.state.name.padEnd(10),
                process.tokenUsage.total.toString().padEnd(7),
                "${process.contextSize}b".padEnd(5),
                process.syscallCount.toString().padEnd(9),
                process.toolTimeMillis.toString().padEnd(8),
                process.estimatedCostMicros.toString().padEnd(5),
                (process.workerId ?: "-"),
            ).joinToString(" "),
        )
    }
    println()
    println("SYSCALL LEDGER")
    result.syscalls.forEach { record ->
        println("${record.callId} pid=${record.pid?.value} agent=${record.agent.value} tool=${record.tool} allowed=${record.allowed} args=${record.redactedArguments}")
    }
    println()
    println("FINAL OUTPUT")
    println(result.finalOutput)
}
