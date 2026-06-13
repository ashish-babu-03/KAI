package ai.kaios

import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkflowSchedulerTest {
    @Test
    fun `scheduler requests a parallel executor for ready nodes`() {
        var largestReadyBatch = 0
        val scheduler = WorkflowScheduler(
            runtime = AgentRuntime(),
            modelProvider = MockModelProvider(),
            executorFactory = { size ->
                largestReadyBatch = max(largestReadyBatch, size)
                Executors.newFixedThreadPool(size)
            },
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
}
