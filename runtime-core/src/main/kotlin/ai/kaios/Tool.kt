package ai.kaios

enum class ToolPermission {
    ECHO,
    READ_CLOCK,
    NETWORK,
    FILE,
}

data class ToolCall(
    val tool: String,
    val arguments: Map<String, String> = emptyMap(),
)

data class ToolResult(
    val tool: String,
    val ok: Boolean,
    val output: String,
    val error: String? = null,
) {
    companion object {
        fun success(tool: String, output: String): ToolResult = ToolResult(tool = tool, ok = true, output = output)

        fun failure(tool: String, error: String): ToolResult =
            ToolResult(tool = tool, ok = false, output = "", error = error)
    }
}

interface Tool {
    val name: String
    val description: String
    val permission: ToolPermission

    fun call(call: ToolCall): ToolResult
}

class ToolRegistry(
    tools: Iterable<Tool> = emptyList(),
) {
    private val toolsByName: Map<String, Tool> = tools.associateBy { it.name }

    val names: Set<String>
        get() = toolsByName.keys

    fun get(name: String): Tool? = toolsByName[name]

    fun execute(agent: AgentSpec, call: ToolCall): ToolResult {
        val tool = toolsByName[call.tool]
            ?: return ToolResult.failure(call.tool, "Tool '${call.tool}' is not registered.")

        if (call.tool !in agent.allowedTools) {
            return ToolResult.failure(call.tool, "Agent '${agent.id}' is not allowed to call '${call.tool}'.")
        }

        if (tool.permission !in agent.permissions) {
            return ToolResult.failure(call.tool, "Agent '${agent.id}' lacks permission '${tool.permission}'.")
        }

        return tool.call(call)
    }

    companion object {
        val Empty: ToolRegistry = ToolRegistry()
    }
}
