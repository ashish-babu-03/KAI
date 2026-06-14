package ai.kaios.cli

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText

internal data class ContextBundle(
    val sources: List<ContextSource>,
    val truncated: Boolean,
    val maxChars: Int,
    val ignorePatternCount: Int = 0,
) {
    val totalChars: Int
        get() = sources.sumOf { it.content.length }

    fun inputFor(task: String): String {
        if (sources.isEmpty()) return task

        return buildString {
            append(task.trim())
            appendLine()
            appendLine()
            appendLine("[KAIOS_CONTEXT]")
            sources.forEach { source ->
                appendLine("### ${source.path}")
                appendLine("```")
                appendLine(source.content.trimEnd())
                appendLine("```")
            }
            if (truncated) {
                appendLine("Context was truncated at $maxChars characters.")
            }
            appendLine("[/KAIOS_CONTEXT]")
        }
    }

    fun taskSummary(task: String): String {
        if (sources.isEmpty()) return task

        return buildString {
            appendLine(task)
            appendLine()
            appendLine("Context:")
            sources.forEach { source ->
                val suffix = if (source.truncated) ", truncated from ${source.originalChars}" else ""
                appendLine("- ${source.path} (${source.content.length} chars$suffix)")
            }
            if (truncated) {
                appendLine("- total context truncated at $maxChars chars")
            }
        }.trimEnd()
    }

    companion object {
        val Empty: ContextBundle = ContextBundle(emptyList(), truncated = false, maxChars = 0)
    }
}

internal data class ContextSource(
    val path: String,
    val content: String,
    val originalChars: Int,
    val truncated: Boolean,
)

internal class ContextLoader(
    private val workingDir: Path,
    private val maxChars: Int = 80_000,
    private val maxFileChars: Int = 20_000,
    private val maxFiles: Int = 40,
) {
    private val ignore = ContextIgnore.load(workingDir)

    fun load(paths: List<Path>): ContextBundle {
        if (paths.isEmpty()) return ContextBundle.Empty

        val files = paths
            .flatMap { expand(it) }
            .distinct()
            .sortedBy { displayPath(it) }

        require(files.isNotEmpty()) { "No context files found." }

        val sources = mutableListOf<ContextSource>()
        var remaining = maxChars
        var truncated = false

        files.take(maxFiles).forEach { file ->
            if (remaining <= 0) {
                truncated = true
                return@forEach
            }

            val text = readTextFile(file) ?: return@forEach
            val limit = minOf(maxFileChars, remaining, text.length)
            val content = text.take(limit)
            sources += ContextSource(
                path = displayPath(file),
                content = content,
                originalChars = text.length,
                truncated = limit < text.length,
            )
            remaining -= content.length
            if (limit < text.length) truncated = true
        }

        if (files.size > maxFiles) truncated = true
        require(sources.isNotEmpty()) { "No readable text context files found." }

        return ContextBundle(sources, truncated, maxChars, ignorePatternCount = ignore.rules.size)
    }

    private fun expand(path: Path): List<Path> {
        val normalized = path.toAbsolutePath().normalize()
        require(normalized.exists()) { "Context path '$path' was not found." }
        require(normalized.startsWith(workingDir)) { "Context path '$path' must stay inside $workingDir." }

        if (shouldSkip(normalized)) return emptyList()
        if (normalized.isRegularFile()) return listOf(normalized)
        if (!normalized.isDirectory()) return emptyList()

        val files = mutableListOf<Path>()
        collectFiles(normalized, files)
        return files
    }

    private fun collectFiles(path: Path, files: MutableList<Path>) {
        if (shouldSkip(path)) return
        when {
            path.isRegularFile() && WorkspaceFileRules.hasTextExtension(path) -> files.add(path)
            path.isDirectory() -> runCatching {
                Files.list(path).use { stream ->
                    stream.toList().forEach { candidate -> collectFiles(candidate, files) }
                }
            }
        }
    }

    private fun readTextFile(path: Path): String? {
        if (!WorkspaceFileRules.hasTextExtension(path)) return null
        if (Files.size(path) > 1_000_000L) return null

        val text = runCatching { path.readText() }.getOrNull() ?: return null
        if ('\u0000' in text) return null
        return text
    }

    private fun shouldSkip(path: Path): Boolean {
        val relative = if (path.startsWith(workingDir)) workingDir.relativize(path) else path
        return WorkspaceFileRules.shouldSkip(relative, path.isDirectory(), ignore)
    }

    private fun displayPath(path: Path): String =
        if (path.startsWith(workingDir)) workingDir.relativize(path).toString() else path.toString()

}

internal object WorkspaceFileRules {
    private val skippedSegments = setOf(
        ".git",
        ".gradle",
        ".idea",
        ".kaios",
        "artifacts",
        "build",
        "node_modules",
        "out",
        "target",
    )
    private val textFileNames = setOf(".gitignore", "Dockerfile", "LICENSE", "README", "Makefile")
    private val textExtensions = setOf(
        "cfg",
        "css",
        "gradle",
        "html",
        "java",
        "js",
        "json",
        "kt",
        "kts",
        "md",
        "properties",
        "py",
        "rb",
        "sh",
        "toml",
        "ts",
        "txt",
        "xml",
        "yaml",
        "yml",
    )

    fun hasTextExtension(path: Path): Boolean {
        val name = path.name
        if (name in textFileNames) return true

        val extension = name.substringAfterLast('.', missingDelimiterValue = "")
        if (extension.isBlank()) return false
        return extension.lowercase() in textExtensions
    }

    fun shouldSkip(relativePath: Path, directory: Boolean, ignore: ContextIgnore): Boolean =
        relativePath.iterator().asSequence().any { segment -> segment.toString() in skippedSegments } ||
            ignore.isIgnored(relativePath, directory)
}

internal data class ContextIgnore(
    val rules: List<ContextIgnoreRule>,
) {
    fun isIgnored(relativePath: Path, directory: Boolean): Boolean {
        val normalized = relativePath
            .normalize()
            .joinToString("/")
            .trim('/')
        if (normalized.isBlank()) return false

        var ignored = false
        rules.forEach { rule ->
            if (rule.matches(normalized, directory)) {
                ignored = !rule.negated
            }
        }
        return ignored
    }

    companion object {
        fun load(workingDir: Path): ContextIgnore {
            val path = workingDir.resolve(".kaiosignore").normalize()
            if (!path.exists() || !path.isRegularFile()) return ContextIgnore(emptyList())

            val rules = path.readText()
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .map(::ContextIgnoreRule)
                .toList()
            return ContextIgnore(rules)
        }
    }
}

internal class ContextIgnoreRule(rawPattern: String) {
    val negated: Boolean = rawPattern.startsWith("!")
    private val pattern = rawPattern.removePrefix("!").trim().trimStart('/')
    private val directoryOnly = pattern.endsWith("/")
    private val body = pattern.trimEnd('/').ifBlank { rawPattern.trim() }
    private val hasSlash = "/" in body
    private val regex = globToRegex(body)

    fun matches(path: String, directory: Boolean): Boolean {
        if (directoryOnly) {
            return pathPrefixes(path, includeSelf = directory).any { regex.matches(it) }
        }

        if (hasSlash) {
            return regex.matches(path)
        }

        return path.split('/').any { segment -> regex.matches(segment) }
    }

    private fun pathPrefixes(path: String, includeSelf: Boolean): List<String> {
        val segments = path.split('/').filter { it.isNotBlank() }
        val limit = if (includeSelf) segments.size else segments.size - 1
        if (limit <= 0) return emptyList()
        return (1..limit).map { count -> segments.take(count).joinToString("/") }
    }

    private fun globToRegex(glob: String): Regex {
        val builder = StringBuilder("^")
        var index = 0
        while (index < glob.length) {
            val char = glob[index]
            when (char) {
                '*' -> {
                    if (glob.getOrNull(index + 1) == '*') {
                        builder.append(".*")
                        index += 1
                    } else {
                        builder.append("[^/]*")
                    }
                }
                '?' -> builder.append("[^/]")
                '.', '(', ')', '+', '|', '^', '$', '@', '%', '{', '}', '[', ']', '\\' -> builder.append('\\').append(char)
                else -> builder.append(char)
            }
            index += 1
        }
        builder.append('$')
        return Regex(builder.toString())
    }
}
