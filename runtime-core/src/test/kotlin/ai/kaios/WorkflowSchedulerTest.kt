package ai.kaios

import kotlinx.coroutines.CancellationException
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkflowSchedulerTest {
    @Test
    fun `scheduler runs ready nodes in a parallel coroutine batch`() {
        var largestReadyBatch = 0
        val scheduler = WorkflowScheduler(
            runtime = AgentRuntime(),
            modelProvider = MockModelProvider(),
            onReadyBatch = { size -> largestReadyBatch = maxOf(largestReadyBatch, size) },
        )

        val result = scheduler.run(
            Workflow(
                name = "parallel",
                nodes = listOf(
                    WorkflowNode("alpha", AgentSpec(AgentId("alpha"))),
                    WorkflowNode("beta", AgentSpec(AgentId("beta"))),
                ),
            ),
            input = "parallel work",
            runId = RunId("run-parallel"),
        )

        assertTrue(result.success)
        assertEquals(2, largestReadyBatch)
        assertEquals(setOf("alpha", "beta"), result.outputs.keys)
    }

    @Test
    fun `node failure propagates when no fallback exists`() {
        val scheduler = WorkflowScheduler(
            runtime = AgentRuntime(),
            modelProvider = FailingModelProvider("primary"),
        )

        val result = scheduler.run(
            Workflow(
                name = "failure",
                nodes = listOf(WorkflowNode("primary", AgentSpec(AgentId("primary")))),
            ),
            input = "fail",
            runId = RunId("run-failure"),
        )

        assertFalse(result.success)
        assertTrue(result.finalOutput.contains("planned failure"))
        assertEquals(ProcessState.FAILED, result.processes.single().state)
    }

    @Test
    fun `fallback node can recover a failed node and unblock dependents`() {
        val scheduler = WorkflowScheduler(
            runtime = AgentRuntime(),
            modelProvider = FailingModelProvider("primary", delegate = MockModelProvider()),
        )

        val result = scheduler.run(
            Workflow(
                name = "fallback",
                nodes = listOf(
                    WorkflowNode(
                        id = "primary",
                        agent = AgentSpec(AgentId("primary")),
                        fallback = "backup",
                    ),
                    WorkflowNode(
                        id = "backup",
                        agent = AgentSpec(AgentId("backup")),
                        fallbackOnly = true,
                    ),
                    WorkflowNode(
                        id = "consumer",
                        agent = AgentSpec(AgentId("consumer")),
                        dependencies = setOf("primary"),
                    ),
                ),
            ),
            input = "recover",
            runId = RunId("run-fallback"),
        )

        assertTrue(result.success)
        assertEquals("backup", result.outputs.getValue("primary").fallbackNodeId)
        assertEquals(ProcessState.FAILED, result.processes.first { it.agent.value == "primary" }.state)
        assertEquals(ProcessState.SUCCEEDED, result.processes.first { it.agent.value == "backup" }.state)
        assertEquals(ProcessState.SUCCEEDED, result.processes.first { it.agent.value == "consumer" }.state)
    }

    @Test
    fun `node timeout is recorded as a cancelled process`() {
        val scheduler = WorkflowScheduler(
            runtime = AgentRuntime(),
            modelProvider = SleepingModelProvider(),
            nodeTimeout = Duration.ofMillis(100),
        )

        val result = scheduler.run(
            Workflow(
                name = "timeout",
                nodes = listOf(WorkflowNode("slow", AgentSpec(AgentId("slow")))),
            ),
            input = "timeout",
            runId = RunId("run-timeout"),
        )

        assertFalse(result.success)
        assertEquals(ProcessState.CANCELLED, result.processes.single().state)
        assertTrue(result.events.any { it.type == RuntimeEventType.CANCELLED })
    }

    @Test
    fun `failure cancels running sibling node jobs`() {
        val slowStarted = CountDownLatch(1)
        val scheduler = WorkflowScheduler(
            runtime = AgentRuntime(),
            modelProvider = FailingAfterSlowStartsModelProvider(slowStarted),
        )

        val result = scheduler.run(
            Workflow(
                name = "cancel-siblings",
                nodes = listOf(
                    WorkflowNode("fail", AgentSpec(AgentId("fail"))),
                    WorkflowNode("slow", AgentSpec(AgentId("slow"))),
                ),
            ),
            input = "cancel",
            runId = RunId("run-cancel-sibling"),
        )

        assertFalse(result.success)
        assertEquals(ProcessState.FAILED, result.processes.first { it.agent.value == "fail" }.state)
        assertEquals(ProcessState.CANCELLED, result.processes.first { it.agent.value == "slow" }.state)
    }

    private class FailingModelProvider(
        private val failingAgent: String,
        private val delegate: ModelProvider = MockModelProvider(),
    ) : ModelProvider {
        override fun complete(request: ModelRequest): ModelResponse {
            if (request.agent.id.value == failingAgent) {
                error("planned failure from ${request.agent.id.value}")
            }
            return delegate.complete(request)
        }
    }

    private class SleepingModelProvider : ModelProvider {
        override fun complete(request: ModelRequest): ModelResponse {
            try {
                Thread.sleep(5_000)
            } catch (error: InterruptedException) {
                throw CancellationException("sleeping model cancelled")
            }
            return ModelResponse("slow output", TokenUsage(input = 1, output = 1))
        }
    }

    private class FailingAfterSlowStartsModelProvider(
        private val slowStarted: CountDownLatch,
    ) : ModelProvider {
        override fun complete(request: ModelRequest): ModelResponse {
            return when (request.agent.id.value) {
                "fail" -> {
                    slowStarted.await(1, TimeUnit.SECONDS)
                    error("planned sibling failure")
                }
                "slow" -> {
                    slowStarted.countDown()
                    try {
                        Thread.sleep(5_000)
                    } catch (error: InterruptedException) {
                        throw CancellationException("slow sibling cancelled")
                    }
                    ModelResponse("slow output", TokenUsage(input = 1, output = 1))
                }
                else -> ModelResponse("ok", TokenUsage(input = 1, output = 1))
            }
        }
    }
}
