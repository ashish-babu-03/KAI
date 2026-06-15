package ai.kaios

data class ModelRequest(
    val runId: RunId,
    val agent: AgentSpec,
    val input: String,
    val dependencyContext: Map<String, String> = emptyMap(),
    val memory: List<MemoryEntry> = emptyList(),
    val availableTools: Set<String> = emptySet(),
)

data class ModelResponse(
    val content: String,
    val tokenUsage: TokenUsage,
    val toolCalls: List<ToolCall> = emptyList(),
)

interface ModelProvider {
    fun complete(request: ModelRequest): ModelResponse
}

class MockModelProvider : ModelProvider {
    override fun complete(request: ModelRequest): ModelResponse {
        val rawInput = request.input.trim()
        val inputDigest = MockInputDigest.parse(rawInput).copy(runIdHint = request.runId.value)
        val agentName = request.agent.id.value.lowercase()
        val fingerprint = stableFingerprint("${request.agent.id.value}:$rawInput:${request.dependencyContext.values.joinToString("|")}")
        val dependencySummary = if (request.dependencyContext.isEmpty()) {
            "no dependencies"
        } else {
            request.dependencyContext.keys.joinToString(prefix = "after ", separator = ", ")
        }

        val content = when {
            "planner" in agentName -> plannerContent(fingerprint, inputDigest)
            "researcher" in agentName -> "research:$fingerprint gathered context for '${inputDigest.shortTask()}' $dependencySummary"
            "synthesizer" in agentName -> "synthesize:$fingerprint distilled answer for '${inputDigest.shortTask()}' $dependencySummary"
            "inspector" in agentName -> "inspect:$fingerprint mapped code surfaces for '${inputDigest.shortTask()}' $dependencySummary"
            "reviewer" in agentName -> "review:$fingerprint prioritized risks for '${inputDigest.shortTask()}' $dependencySummary"
            "executor" in agentName -> executorContent(fingerprint, inputDigest, dependencySummary)
            "verifier" in agentName -> "verify:$fingerprint checked release evidence from $dependencySummary"
            "announcer" in agentName -> "announce:$fingerprint prepared release notes from $dependencySummary"
            "validator" in agentName -> validatorContent(fingerprint, inputDigest, request.dependencyContext, dependencySummary)
            else -> "agent:$fingerprint processed '${inputDigest.shortTask()}'"
        }

        val toolCalls = when {
            "planner" in agentName && "echo" in request.availableTools -> listOf(
                ToolCall("echo", mapOf("message" to "planning:$fingerprint")),
            )
            "executor" in agentName && "mock-http" in request.availableTools -> listOf(
                ToolCall("mock-http", mapOf("method" to "GET", "url" to "mock://kaios/tasks/$fingerprint")),
            )
            "researcher" in agentName && "mock-http" in request.availableTools -> listOf(
                ToolCall("mock-http", mapOf("method" to "GET", "url" to "mock://kaios/research/$fingerprint")),
            )
            ("inspector" in agentName || "verifier" in agentName) && "file" in request.availableTools -> listOf(
                ToolCall("file", mapOf("op" to "exists", "path" to ".")),
            )
            "reviewer" in agentName && "echo" in request.availableTools -> listOf(
                ToolCall("echo", mapOf("message" to "reviewed:$fingerprint")),
            )
            "announcer" in agentName && "echo" in request.availableTools -> listOf(
                ToolCall("echo", mapOf("message" to "announced:$fingerprint")),
            )
            "validator" in agentName && "echo" in request.availableTools -> listOf(
                ToolCall("echo", mapOf("message" to "validated:$fingerprint")),
            )
            else -> emptyList()
        }

        return ModelResponse(
            content = content,
            tokenUsage = TokenUsage(
                input = estimateTokens(rawInput + request.dependencyContext.values.joinToString(" ") + request.memory.joinToString(" ") { it.content }),
                output = estimateTokens(content + toolCalls.joinToString(" ") { it.tool }),
            ),
            toolCalls = toolCalls,
        )
    }

    private fun stableFingerprint(value: String): String {
        val hash = value.fold(7) { acc, char -> (acc * 31 + char.code) and 0x7fffffff }
        return hash.toString(16).padStart(8, '0').takeLast(8)
    }

    private fun plannerContent(fingerprint: String, input: MockInputDigest): String =
        if (input.hasProjectInput()) {
            "plan:$fingerprint inspect ${input.projectShape()}, summarize ${input.languageSummary()}, validate artifact handoff"
        } else {
            "plan:$fingerprint inspect task, execute with tools, validate output"
        }

    private fun executorContent(fingerprint: String, input: MockInputDigest, dependencySummary: String): String =
        if (input.hasProjectInput()) {
            buildProjectSummary(prefix = "execute:$fingerprint project summary", input = input)
        } else {
            "execute:$fingerprint synthesized result for '${input.shortTask()}' $dependencySummary"
        }

    private fun validatorContent(
        fingerprint: String,
        input: MockInputDigest,
        dependencyContext: Map<String, String>,
        dependencySummary: String,
    ): String =
        if (input.hasProjectInput()) {
            buildString {
                appendLine("validate:$fingerprint accepted project summary from $dependencySummary")
                appendLine()
                append(buildProjectSummary(prefix = "Project summary", input = input))
                val executorOutput = dependencyContext.values.firstOrNull { output -> output.contains("project summary", ignoreCase = true) }
                if (executorOutput != null) {
                    appendLine()
                    appendLine("- Dependency checked: executor produced a bounded project summary.")
                }
            }.trim()
        } else {
            "validate:$fingerprint accepted result from $dependencySummary"
        }

    private fun buildProjectSummary(prefix: String, input: MockInputDigest): String = buildString {
        appendLine("$prefix for '${input.shortTask()}'")
        appendLine("- Shape: ${input.projectShape()}.")
        appendLine("- Languages: ${input.languageSummary()}.")
        appendLine("- Notable files: ${input.notableSummary()}.")
        appendLine("- Context used: ${input.contextSummary()}.")
        append("- Next: inspect process telemetry with `kaios ps ${input.runIdHint}` and validate trace evidence with `kaios trace ${input.runIdHint} --check`.")
    }

    private fun estimateTokens(text: String): Int =
        text.trim()
            .split(Regex("\\s+"))
            .count { it.isNotBlank() }
            .coerceAtLeast(1)

    private data class MockInputDigest(
        val task: String,
        val workspaceIndex: WorkspaceIndexDigest?,
        val contextFiles: List<String>,
        val runIdHint: String = "latest",
    ) {
        fun hasProjectInput(): Boolean = workspaceIndex != null || contextFiles.isNotEmpty()

        fun shortTask(): String =
            task.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { "empty task" }

        fun projectShape(): String =
            workspaceIndex?.let { index -> "${countLabel(index.files, "file")}, ${countLabel(index.lines, "line")}" }
                ?: "no workspace index"

        fun languageSummary(): String =
            workspaceIndex
                ?.languages
                ?.take(4)
                ?.joinToString(", ") { language -> "${language.name}:${language.files}" }
                ?.ifBlank { "not detected" }
                ?: "not detected"

        fun notableSummary(): String =
            workspaceIndex
                ?.notableFiles
                ?.take(5)
                ?.joinToString(", ")
                ?.ifBlank { "none detected" }
                ?: "none detected"

        fun contextSummary(): String =
            contextFiles
                .take(5)
                .joinToString(", ")
                .ifBlank { "workspace index only" }

        companion object {
            fun parse(rawInput: String): MockInputDigest {
                val workspaceBlock = sectionBetween(rawInput, "[KAIOS_WORKSPACE_INDEX]", "[/KAIOS_WORKSPACE_INDEX]")
                val contextBlock = sectionBetween(rawInput, "[KAIOS_CONTEXT]", "[/KAIOS_CONTEXT]")
                val task = rawInput
                    .removeSection("[KAIOS_WORKSPACE_INDEX]", "[/KAIOS_WORKSPACE_INDEX]")
                    .removeSection("[KAIOS_CONTEXT]", "[/KAIOS_CONTEXT]")
                    .trim()

                return MockInputDigest(
                    task = task,
                    workspaceIndex = workspaceBlock?.let(::parseWorkspaceIndex),
                    contextFiles = parseContextFiles(contextBlock.orEmpty()),
                )
            }

            private fun sectionBetween(input: String, start: String, end: String): String? {
                val startIndex = input.indexOf(start)
                if (startIndex < 0) return null
                val contentStart = startIndex + start.length
                val endIndex = input.indexOf(end, startIndex = contentStart)
                if (endIndex < 0) return null
                return input.substring(contentStart, endIndex).trim()
            }

            private fun String.removeSection(start: String, end: String): String {
                val startIndex = indexOf(start)
                if (startIndex < 0) return this
                val endIndex = indexOf(end, startIndex = startIndex + start.length)
                if (endIndex < 0) return this
                return removeRange(startIndex, endIndex + end.length)
            }

            private fun parseWorkspaceIndex(block: String): WorkspaceIndexDigest {
                val lines = block.lines().map { line -> line.trim() }
                val totals = parseTotals(lines.firstOrNull { line -> line.startsWith("files:") }.orEmpty())
                val languages = lines
                    .dropWhile { line -> line != "languages:" }
                    .drop(1)
                    .takeWhile { line -> line.isNotBlank() }
                    .mapNotNull(::parseLanguage)
                val notableFiles = lines
                    .dropWhile { line -> line != "notable files:" }
                    .drop(1)
                    .takeWhile { line -> line.isNotBlank() }
                    .mapNotNull { line -> line.removePrefix("- ").substringBefore(" (").trim().ifBlank { null } }

                return WorkspaceIndexDigest(
                    files = totals.files,
                    lines = totals.lines,
                    languages = languages,
                    notableFiles = notableFiles,
                )
            }

            private fun parseTotals(line: String): WorkspaceTotals {
                val parts = line.removePrefix("files:")
                    .split(",")
                    .map { part -> part.trim() }
                val files = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val lines = parts
                    .firstOrNull { part -> part.startsWith("lines:") }
                    ?.removePrefix("lines:")
                    ?.trim()
                    ?.toIntOrNull()
                    ?: 0
                return WorkspaceTotals(files = files, lines = lines)
            }

            private fun parseLanguage(line: String): WorkspaceLanguageDigest? {
                if (!line.startsWith("- ")) return null
                val name = line.removePrefix("- ").substringBefore(":").trim()
                val files = line.substringAfter(":", missingDelimiterValue = "")
                    .trim()
                    .substringBefore(" ")
                    .toIntOrNull()
                    ?: return null
                return WorkspaceLanguageDigest(name = name, files = files)
            }

            private fun parseContextFiles(block: String): List<String> =
                block.lines()
                    .map { line -> line.trim() }
                    .filter { line -> line.startsWith("### ") }
                    .map { line -> line.removePrefix("### ").trim() }
                    .filter { path -> path.isNotBlank() }
        }
    }

    private data class WorkspaceIndexDigest(
        val files: Int,
        val lines: Int,
        val languages: List<WorkspaceLanguageDigest>,
        val notableFiles: List<String>,
    )

    private data class WorkspaceLanguageDigest(
        val name: String,
        val files: Int,
    )

    private data class WorkspaceTotals(
        val files: Int,
        val lines: Int,
    )

}

private fun countLabel(count: Int, singular: String, plural: String = "${singular}s"): String =
    "$count ${if (count == 1) singular else plural}"
