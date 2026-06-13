package ai.kaios

import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.readText
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

    @Test
    fun `scoped file tool writes reads lists and checks files inside root`() {
        val root = Files.createTempDirectory("kaios-file-tool")
        val tool = ScopedFileTool(root)

        val write = tool.call(
            ToolCall(
                "file",
                mapOf(
                    "op" to "write",
                    "path" to "notes/plan.txt",
                    "content" to "agent file syscall",
                ),
            ),
        )
        val read = tool.call(ToolCall("file", mapOf("op" to "read", "path" to "notes/plan.txt")))
        val exists = tool.call(ToolCall("file", mapOf("op" to "exists", "path" to "notes/plan.txt")))
        val list = tool.call(ToolCall("file", mapOf("op" to "list", "path" to "notes")))

        assertTrue(write.ok)
        assertTrue(root.resolve("notes/plan.txt").exists())
        assertEquals("agent file syscall", root.resolve("notes/plan.txt").readText())
        assertEquals("agent file syscall", read.output)
        assertEquals("true", exists.output)
        assertEquals("plan.txt", list.output)
    }

    @Test
    fun `scoped file tool rejects path traversal and absolute paths`() {
        val root = Files.createTempDirectory("kaios-file-tool-scope")
        val tool = ScopedFileTool(root)

        val traversal = tool.call(ToolCall("file", mapOf("op" to "read", "path" to "../secret.txt")))
        val absolute = tool.call(ToolCall("file", mapOf("op" to "read", "path" to root.resolve("file.txt").toString())))

        assertFalse(traversal.ok)
        assertTrue(traversal.error.orEmpty().contains("escapes scoped root"))
        assertFalse(absolute.ok)
        assertTrue(absolute.error.orEmpty().contains("escapes scoped root"))
    }

    @Test
    fun `scoped file tool reports missing files`() {
        val root = Files.createTempDirectory("kaios-file-tool-missing")
        val tool = ScopedFileTool(root)

        val result = tool.call(ToolCall("file", mapOf("op" to "read", "path" to "missing.txt")))

        assertFalse(result.ok)
        assertTrue(result.error.orEmpty().contains("does not exist"))
    }

    @Test
    fun `registry denies file syscall without file permission`() {
        val agent = AgentSpec(
            id = AgentId("agent"),
            allowedTools = setOf("file"),
            permissions = emptySet(),
        )
        val result = ToolRegistry(listOf(ScopedFileTool(Files.createTempDirectory("kaios-deny")))).execute(
            agent,
            ToolCall("file", mapOf("op" to "exists", "path" to "x.txt")),
        )

        assertFalse(result.ok)
        assertTrue(result.error.orEmpty().contains("lacks permission"))
    }
}
