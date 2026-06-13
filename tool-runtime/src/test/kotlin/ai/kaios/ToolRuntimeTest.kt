package ai.kaios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolRuntimeTest {
    @Test
    fun `registered tool call succeeds when agent has permission`() {
        val agent = AgentSpec(
            id = AgentId("agent"),
            allowedTools = setOf("echo"),
            permissions = setOf(ToolPermission.ECHO),
        )
        val result = ToolRegistry(listOf(EchoTool())).execute(
            agent,
            ToolCall("echo", mapOf("message" to "hello")),
        )

        assertTrue(result.ok)
        assertEquals("hello", result.output)
    }

    @Test
    fun `tool call is denied without permission`() {
        val agent = AgentSpec(
            id = AgentId("agent"),
            allowedTools = setOf("echo"),
            permissions = emptySet(),
        )
        val result = ToolRegistry(listOf(EchoTool())).execute(
            agent,
            ToolCall("echo", mapOf("message" to "hello")),
        )

        assertFalse(result.ok)
        assertTrue(result.error.orEmpty().contains("lacks permission"))
    }

    @Test
    fun `runtime syscall count tracks denied syscalls too`() {
        val runtime = AgentRuntime()
        val process = runtime.spawn(AgentSpec(AgentId("agent")), RunId("run-tools"))
        runtime.start(process.pid)
        runtime.recordSyscall(process.pid, ToolResult.failure("echo", "permission denied"))

        assertEquals(1, runtime.process(process.pid)?.syscallCount)
        assertEquals(RuntimeEventType.TOOL_CALLED, runtime.events(RunId("run-tools")).last().type)
    }
}
