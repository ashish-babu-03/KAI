package ai.kaios

import java.time.Clock

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

fun builtInToolRegistry(clock: Clock = Clock.systemUTC()): ToolRegistry =
    ToolRegistry(
        listOf(
            EchoTool(),
            ClockTool(clock),
            MockHttpTool(),
        ),
    )
