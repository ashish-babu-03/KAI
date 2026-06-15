package ai.kaios.cli

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText

internal data class WorkspaceIndex(
    val root: Path,
    val files: List<WorkspaceIndexFile>,
    val truncated: Boolean,
    val maxFiles: Int,
    val ignorePatternCount: Int,
) {
    val totalBytes: Long
        get() = files.sumOf { it.bytes }

    val totalLines: Int
        get() = files.sumOf { it.lines }

    val languageStats: List<WorkspaceLanguageStat>
        get() = files
            .groupBy { it.language }
            .map { (language, files) ->
                WorkspaceLanguageStat(
                    language = language,
                    files = files.size,
                    bytes = files.sumOf { it.bytes },
                    lines = files.sumOf { it.lines },
                )
            }
            .sortedWith(compareByDescending<WorkspaceLanguageStat> { it.files }.thenByDescending { it.bytes })

    val directoryStats: List<WorkspaceDirectoryStat>
        get() = files
            .groupBy { it.directory }
            .map { (directory, files) ->
                WorkspaceDirectoryStat(
                    directory = directory,
                    files = files.size,
                    bytes = files.sumOf { it.bytes },
                )
            }
            .sortedWith(compareByDescending<WorkspaceDirectoryStat> { it.files }.thenByDescending { it.bytes })

    val notableFiles: List<WorkspaceIndexFile>
        get() = files
            .sortedWith(compareBy<WorkspaceIndexFile> { notableRank(it.path) }.thenBy { it.path })
            .filter { notableRank(it.path) < Int.MAX_VALUE }
            .take(20)

    val largestFiles: List<WorkspaceIndexFile>
        get() = files.sortedByDescending { it.bytes }.take(12)

    fun render(): String = buildString {
        appendLine("WORKSPACE INDEX")
        appendLine("root: $root")
        appendLine("files: ${files.size}")
        appendLine("bytes: $totalBytes")
        appendLine("lines: $totalLines")
        appendLine("truncated: $truncated")
        if (ignorePatternCount > 0) {
            appendLine("ignore: .kaiosignore ($ignorePatternCount pattern(s))")
        }
        appendLine()

        appendLine("LANGUAGES")
        appendLine(formatLanguageHeader())
        languageStats.forEach { appendLine(formatLanguageStat(it)) }
        appendLine()

        appendLine("TOP DIRECTORIES")
        appendLine(formatDirectoryHeader())
        directoryStats.take(12).forEach { appendLine(formatDirectoryStat(it)) }
        appendLine()

        appendLine("NOTABLE FILES")
        notableFiles.ifEmpty { largestFiles.take(8) }.forEach { file ->
            appendLine("- ${file.path} (${file.language}, ${countLabel(file.lines, "line")}, ${countLabel(file.bytes, "byte")})")
        }
        appendLine()

        appendLine("LARGEST TEXT FILES")
        largestFiles.forEach { file ->
            appendLine("- ${file.path} (${file.language}, ${countLabel(file.lines, "line")}, ${countLabel(file.bytes, "byte")})")
        }
    }.trimEnd()

    fun promptBlock(): String = buildString {
        appendLine("root: $root")
        appendLine("files: ${files.size}, lines: $totalLines, bytes: $totalBytes")
        if (truncated) appendLine("truncated: true (maxFiles=$maxFiles)")
        if (ignorePatternCount > 0) appendLine("ignore: .kaiosignore ($ignorePatternCount pattern(s))")
        appendLine()
        appendLine("languages:")
        languageStats.take(12).forEach { stat ->
            appendLine("- ${stat.language}: ${stat.files} files, ${stat.lines} lines")
        }
        appendLine()
        appendLine("notable files:")
        notableFiles.ifEmpty { largestFiles.take(8) }.forEach { file ->
            appendLine("- ${file.path} (${file.language}, ${file.lines} lines)")
        }
        appendLine()
        appendLine("top directories:")
        directoryStats.take(8).forEach { stat ->
            appendLine("- ${stat.directory}: ${stat.files} files")
        }
    }.trimEnd()

    fun summary(): String {
        val languages = languageStats.take(4).joinToString(", ") { "${it.language}:${it.files}" }
        return "${files.size} files, $totalLines lines${if (languages.isBlank()) "" else ", $languages"}"
    }

    private fun formatLanguageHeader(): String =
        listOf("LANGUAGE".padEnd(14), "FILES".padEnd(7), "LINES".padEnd(8), "BYTES").joinToString("  ")

    private fun formatLanguageStat(stat: WorkspaceLanguageStat): String =
        listOf(
            stat.language.padEnd(14),
            stat.files.toString().padEnd(7),
            stat.lines.toString().padEnd(8),
            stat.bytes.toString(),
        ).joinToString("  ")

    private fun formatDirectoryHeader(): String =
        listOf("DIR".padEnd(28), "FILES".padEnd(7), "BYTES").joinToString("  ")

    private fun formatDirectoryStat(stat: WorkspaceDirectoryStat): String =
        listOf(
            stat.directory.take(28).padEnd(28),
            stat.files.toString().padEnd(7),
            stat.bytes.toString(),
        ).joinToString("  ")

    private fun countLabel(count: Int, singular: String, plural: String = "${singular}s"): String =
        countLabel(count.toLong(), singular, plural)

    private fun countLabel(count: Long, singular: String, plural: String = "${singular}s"): String =
        "$count ${if (count == 1L) singular else plural}"

    companion object {
        val Empty: WorkspaceIndex = WorkspaceIndex(
            root = Path.of("."),
            files = emptyList(),
            truncated = false,
            maxFiles = 0,
            ignorePatternCount = 0,
        )
    }
}

internal data class WorkspaceIndexFile(
    val path: String,
    val language: String,
    val bytes: Long,
    val lines: Int,
) {
    val directory: String
        get() = path.substringBefore('/', missingDelimiterValue = ".").ifBlank { "." }
}

internal data class WorkspaceLanguageStat(
    val language: String,
    val files: Int,
    val bytes: Long,
    val lines: Int,
)

internal data class WorkspaceDirectoryStat(
    val directory: String,
    val files: Int,
    val bytes: Long,
)

internal class WorkspaceIndexer(
    private val workingDir: Path,
    private val maxFiles: Int = 500,
) {
    private val ignore = ContextIgnore.load(workingDir)

    fun index(paths: List<Path>): WorkspaceIndex {
        val roots = paths.ifEmpty { listOf(workingDir) }
        val candidates = roots
            .flatMap { expand(it) }
            .distinct()
            .sortedBy { displayPath(it) }

        require(candidates.isNotEmpty()) { "No workspace text files found." }

        val files = candidates
            .take(maxFiles)
            .mapNotNull(::indexFile)

        require(files.isNotEmpty()) { "No readable workspace text files found." }

        return WorkspaceIndex(
            root = workingDir,
            files = files,
            truncated = candidates.size > maxFiles,
            maxFiles = maxFiles,
            ignorePatternCount = ignore.rules.size,
        )
    }

    private fun expand(path: Path): List<Path> {
        val normalized = path.toAbsolutePath().normalize()
        require(normalized.exists()) { "Workspace path '$path' was not found." }
        require(normalized.startsWith(workingDir)) { "Workspace path '$path' must stay inside $workingDir." }

        if (shouldSkip(normalized)) return emptyList()
        if (normalized.isRegularFile()) {
            return if (WorkspaceFileRules.hasTextExtension(normalized)) listOf(normalized) else emptyList()
        }
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

    private fun indexFile(path: Path): WorkspaceIndexFile? {
        if (Files.size(path) > 1_000_000L) return null
        val text = runCatching { path.readText() }.getOrNull() ?: return null
        if ('\u0000' in text) return null

        return WorkspaceIndexFile(
            path = displayPath(path),
            language = languageFor(path),
            bytes = Files.size(path),
            lines = lineCount(text),
        )
    }

    private fun shouldSkip(path: Path): Boolean {
        val relative = if (path.startsWith(workingDir)) workingDir.relativize(path) else path
        return WorkspaceFileRules.shouldSkip(relative, path.isDirectory(), ignore)
    }

    private fun displayPath(path: Path): String =
        if (path.startsWith(workingDir)) workingDir.relativize(path).toString() else path.toString()
}

private fun lineCount(text: String): Int =
    if (text.isEmpty()) 0 else text.count { it == '\n' } + if (text.endsWith('\n')) 0 else 1

private fun languageFor(path: Path): String {
    val name = path.name
    if (name == "Dockerfile") return "Dockerfile"
    if (name == "Makefile") return "Makefile"
    if (name.equals("README", ignoreCase = true) || name.endsWith(".md", ignoreCase = true) || name.endsWith(".markdown", ignoreCase = true)) return "Markdown"
    if (name == "LICENSE") return "Text"
    if (name == ".gitignore") return "Git Ignore"

    return when (name.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
        "cfg" -> "Config"
        "css" -> "CSS"
        "gradle" -> "Gradle"
        "html" -> "HTML"
        "java" -> "Java"
        "js" -> "JavaScript"
        "json" -> "JSON"
        "kt", "kts" -> "Kotlin"
        "properties" -> "Properties"
        "py" -> "Python"
        "rb" -> "Ruby"
        "sh" -> "Shell"
        "toml" -> "TOML"
        "ts" -> "TypeScript"
        "txt" -> "Text"
        "xml" -> "XML"
        "yaml", "yml" -> "YAML"
        else -> "Text"
    }
}

private fun notableRank(path: String): Int {
    val name = path.substringAfterLast('/')
    val lower = path.lowercase()

    return when {
        name.equals("README.md", ignoreCase = true) || name.equals("README.markdown", ignoreCase = true) || name.equals("README", ignoreCase = true) -> 0
        name.equals("build.gradle.kts", ignoreCase = true) -> 1
        name.equals("settings.gradle.kts", ignoreCase = true) -> 2
        name.equals("gradle.properties", ignoreCase = true) -> 3
        name.equals("kaios.json", ignoreCase = true) -> 4
        name.equals("CONTRIBUTING.md", ignoreCase = true) -> 5
        name.equals("SECURITY.md", ignoreCase = true) -> 6
        name.equals("ROADMAP.md", ignoreCase = true) -> 7
        lower.startsWith("docs/") && (lower.endsWith(".md") || lower.endsWith(".markdown")) -> 8
        lower.startsWith("src/main/") || "/src/main/" in lower -> 9
        lower.startsWith("src/test/") || "/src/test/" in lower -> 10
        lower.endsWith(".md") || lower.endsWith(".markdown") -> 11
        else -> Int.MAX_VALUE
    }
}
