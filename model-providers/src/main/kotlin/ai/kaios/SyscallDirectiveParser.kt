package ai.kaios

internal data class ParsedProviderContent(
    val content: String,
    val toolCalls: List<ToolCall>,
)

internal object SyscallDirectiveParser {
    private const val PREFIX = "KAIOS_SYSCALL"

    fun parse(content: String): ParsedProviderContent {
        val toolCalls = mutableListOf<ToolCall>()
        val outputLines = mutableListOf<String>()

        content.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith(PREFIX)) {
                parseDirective(trimmed)?.let(toolCalls::add)
            } else {
                outputLines.add(line)
            }
        }

        val output = outputLines.joinToString("\n").trim().ifBlank {
            if (toolCalls.isNotEmpty()) "requested ${toolCalls.size} syscall(s)" else content.trim()
        }

        return ParsedProviderContent(output, toolCalls)
    }

    private fun parseDirective(line: String): ToolCall? {
        val parts = tokenize(line.removePrefix(PREFIX).trim())
        val tool = parts.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val arguments = parts
            .drop(1)
            .mapNotNull { token ->
                val key = token.substringBefore("=", missingDelimiterValue = "")
                val value = token.substringAfter("=", missingDelimiterValue = "")
                if (key.isBlank()) null else key to value
            }
            .toMap()

        return ToolCall(tool, arguments)
    }

    private fun tokenize(value: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var escaped = false

        value.forEach { char ->
            when {
                escaped -> {
                    current.append(char)
                    escaped = false
                }
                char == '\\' -> escaped = true
                quote != null && char == quote -> quote = null
                quote != null -> current.append(char)
                char == '"' || char == '\'' -> quote = char
                char.isWhitespace() -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(char)
            }
        }

        if (escaped) current.append('\\')
        if (current.isNotEmpty()) tokens.add(current.toString())
        return tokens
    }
}
