package ai.kaios

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Clock
import java.time.Duration
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

data class HttpSyscallRequest(
    val method: String,
    val uri: URI,
    val body: String,
    val contentType: String,
    val timeout: Duration,
)

data class HttpSyscallResponse(
    val statusCode: Int,
    val body: String,
)

fun interface HttpSyscallTransport {
    fun send(request: HttpSyscallRequest): HttpSyscallResponse
}

class JdkHttpSyscallTransport(
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build(),
) : HttpSyscallTransport {
    override fun send(request: HttpSyscallRequest): HttpSyscallResponse {
        val builder = HttpRequest.newBuilder(request.uri)
            .timeout(request.timeout)
            .header("User-Agent", "KAI-OS-Agent-Runtime")

        val httpRequest = when (request.method) {
            "GET" -> builder.GET().build()
            "HEAD" -> builder.method("HEAD", HttpRequest.BodyPublishers.noBody()).build()
            "POST" -> builder
                .header("Content-Type", request.contentType)
                .POST(HttpRequest.BodyPublishers.ofString(request.body))
                .build()
            else -> error("Unsupported HTTP method '${request.method}'.")
        }

        val response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())
        return HttpSyscallResponse(response.statusCode(), response.body())
    }
}

class HttpTool(
    allowlist: Iterable<String> = emptyList(),
    private val transport: HttpSyscallTransport = JdkHttpSyscallTransport(),
    private val timeout: Duration = Duration.ofSeconds(10),
    private val maxResponseChars: Int = 20_000,
) : Tool {
    override val name: String = "http"
    override val description: String = "Performs allowlisted HTTP GET, HEAD, and POST requests."
    override val permission: ToolPermission = ToolPermission.NETWORK

    private val rules = allowlist
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map(::HttpAllowRule)

    override fun call(call: ToolCall): ToolResult {
        if (rules.isEmpty()) {
            return ToolResult.failure(name, "HTTP syscall denied. Set KAIOS_HTTP_ALLOWLIST to enable real network hosts.")
        }

        val method = (call.arguments["method"] ?: "GET").uppercase()
        if (method !in allowedMethods) {
            return ToolResult.failure(name, "Unsupported HTTP method '$method'. Use GET, HEAD, or POST.")
        }

        val rawUrl = call.arguments["url"] ?: return ToolResult.failure(name, "HTTP syscall requires a url argument.")
        val uri = runCatching { URI(rawUrl) }.getOrNull()
            ?: return ToolResult.failure(name, "Invalid HTTP URL.")

        if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) {
            return ToolResult.failure(name, "HTTP syscall only supports absolute http(s) URLs.")
        }

        if (rules.none { it.matches(uri) }) {
            return ToolResult.failure(name, "HTTP syscall denied for host '${uri.host}'. Not in KAIOS_HTTP_ALLOWLIST.")
        }

        return runCatching {
            val response = transport.send(
                HttpSyscallRequest(
                    method = method,
                    uri = uri,
                    body = call.arguments["body"].orEmpty(),
                    contentType = call.arguments["contentType"] ?: "application/json",
                    timeout = timeout,
                ),
            )
            val body = response.body.take(maxResponseChars)
            val suffix = if (response.body.length > maxResponseChars) "\n[truncated at $maxResponseChars chars]" else ""
            ToolResult.success(name, "HTTP ${response.statusCode}\n$body$suffix".trimEnd())
        }.getOrElse { error ->
            ToolResult.failure(name, error.message ?: "HTTP syscall failed.")
        }
    }

    private companion object {
        val allowedMethods = setOf("GET", "HEAD", "POST")
    }
}

private class HttpAllowRule(rawRule: String) {
    private val rule = rawRule.trim().trimEnd('/')
    private val uri = rule.takeIf { "://" in it }?.let { value -> runCatching { URI(value) }.getOrNull() }
    private val hostRule = uri?.host ?: rule.substringBefore('/')
    private val scheme = uri?.scheme
    private val hostPattern = hostRule.substringBefore(':').lowercase()
    private val port = uri?.port?.takeIf { it >= 0 } ?: hostRule.substringAfter(':', "").toIntOrNull()
    private val pathPrefix = uri?.rawPath?.takeIf { it.isNotBlank() && it != "/" }
        ?: rule.takeIf { "://" !in it && "/" in it }?.substringAfter('/')?.let { "/$it" }

    fun matches(uri: URI): Boolean {
        if (scheme != null && uri.scheme != scheme) return false
        if (port != null && uri.port != port) return false
        if (!matchesHost(uri.host.lowercase())) return false
        if (pathPrefix != null && !uri.rawPath.orEmpty().ifBlank { "/" }.startsWith(pathPrefix)) return false
        return true
    }

    private fun matchesHost(host: String): Boolean =
        when {
            hostPattern.startsWith("*.") -> {
                val suffix = hostPattern.removePrefix("*.")
                host == suffix || host.endsWith(".$suffix")
            }
            else -> host == hostPattern
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
    httpAllowlist: Iterable<String> = emptyList(),
): ToolRegistry =
    ToolRegistry(
        listOf(
            EchoTool(),
            ClockTool(clock),
            MockHttpTool(),
            HttpTool(httpAllowlist),
            ScopedFileTool(fileRoot),
        ),
    )
