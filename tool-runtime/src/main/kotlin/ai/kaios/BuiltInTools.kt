package ai.kaios

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Clock
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

class EchoTool : Tool {
    override val name: String = "echo"
    override val description: String = "Returns the supplied message."
    override val permission: ToolPermission = ToolPermission.ECHO

    override fun call(call: ToolCall): ToolResult {
        val message = call.arguments["message"]
            ?: call.arguments["text"]
            ?: call.arguments.entries.joinToString(", ") { "${it.key}=${it.value}" }
        return ToolResult.success(name, message)
    }
}

class ClockTool(
    private val clock: Clock = Clock.systemUTC(),
) : Tool {
    override val name: String = "clock"
    override val description: String = "Returns the current UTC timestamp."
    override val permission: ToolPermission = ToolPermission.READ_CLOCK

    override fun call(call: ToolCall): ToolResult = ToolResult.success(name, clock.instant().toString())
}

class MockHttpTool : Tool {
    override val name: String = "mock-http"
    override val description: String = "Returns a deterministic mocked HTTP response."
    override val permission: ToolPermission = ToolPermission.NETWORK

    override fun call(call: ToolCall): ToolResult {
        val method = call.arguments["method"] ?: "GET"
        val url = call.arguments["url"] ?: "mock://kaios"
        return ToolResult.success(name, "$method $url -> 200 OK (mock)")
    }
}

class ScopedFileTool(
    root: Path = Paths.get(".kaios", "files"),
) : Tool {
    override val name: String = "file"
    override val description: String = "Reads, writes, lists, and checks files within a scoped root."
    override val permission: ToolPermission = ToolPermission.FILE

    private val root: Path = root.toAbsolutePath().normalize()

    override fun call(call: ToolCall): ToolResult {
        val operation = call.arguments["op"] ?: call.arguments["operation"] ?: "read"
        val pathValue = call.arguments["path"] ?: ""

        if (pathValue.isBlank()) {
            return ToolResult.failure(name, "File syscall requires a non-blank path.")
        }

        val target = scopedPath(pathValue)
            ?: return ToolResult.failure(name, "File path escapes scoped root.")

        return runCatching {
            when (operation) {
                "read" -> read(target)
                "write" -> write(target, call.arguments["content"].orEmpty())
                "list" -> list(target)
                "exists" -> exists(target)
                else -> ToolResult.failure(name, "Unsupported file operation '$operation'.")
            }
        }.getOrElse { error ->
            ToolResult.failure(name, error.message ?: "File syscall failed.")
        }
    }

    private fun scopedPath(path: String): Path? {
        val relative = Paths.get(path)
        if (relative.isAbsolute) return null
        val target = root.resolve(relative).normalize()
        return target.takeIf { it.startsWith(root) }
    }

    private fun read(target: Path): ToolResult {
        if (!target.exists()) return ToolResult.failure(name, "File '${root.relativize(target)}' does not exist.")
        if (!target.isRegularFile()) return ToolResult.failure(name, "File '${root.relativize(target)}' is not a regular file.")
        return ToolResult.success(name, target.readText())
    }

    private fun write(target: Path, content: String): ToolResult {
        Files.createDirectories(target.parent ?: root)
        target.writeText(content)
        return ToolResult.success(name, "wrote ${content.length} bytes to ${root.relativize(target)}")
    }

    private fun list(target: Path): ToolResult {
        if (!target.exists()) return ToolResult.failure(name, "Directory '${root.relativize(target)}' does not exist.")
        if (!target.isDirectory()) return ToolResult.failure(name, "Path '${root.relativize(target)}' is not a directory.")
        val entries = Files.list(target).use { paths ->
            paths
                .toList()
                .sortedBy { it.name }
                .joinToString("\n") { entry ->
                    val suffix = if (entry.isDirectory()) "/" else ""
                    entry.name + suffix
                }
        }
        return ToolResult.success(name, entries)
    }

    private fun exists(target: Path): ToolResult =
        ToolResult.success(name, target.exists().toString())
}

fun builtInToolRegistry(
    clock: Clock = Clock.systemUTC(),
    fileRoot: Path = Paths.get(".kaios", "files"),
): ToolRegistry =
    ToolRegistry(
        listOf(
            EchoTool(),
            ClockTool(clock),
            MockHttpTool(),
            ScopedFileTool(fileRoot),
        ),
    )
