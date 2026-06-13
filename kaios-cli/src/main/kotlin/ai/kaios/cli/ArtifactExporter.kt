package ai.kaios.cli

import ai.kaios.StoredRunSnapshot

class ArtifactExporter {
    fun render(snapshot: StoredRunSnapshot): String =
        buildString {
            appendLine("# KAI OS Run ${snapshot.runId}")
            appendLine()
            appendLine("- Workflow: `${snapshot.workflowName}`")
            appendLine("- Success: `${snapshot.success}`")
            appendLine("- Task: ${snapshot.task}")
            appendLine()
            appendLine("## Final Output")
            appendLine()
            appendLine(snapshot.finalOutput.ifBlank { "(empty)" })
            appendLine()
            appendLine("## Process Table")
            appendLine()
            appendLine("| PID | Agent | State | Tokens | Memory | Syscalls | Duration |")
            appendLine("| ---: | --- | --- | ---: | ---: | ---: | ---: |")
            snapshot.processes.forEach { process ->
                appendLine(
                    "| ${process.pid} | ${escapeCell(process.agent)} | ${process.state} | " +
                        "${process.tokens} | ${process.contextSize}b | ${process.syscallCount} | ${process.durationMillis}ms |",
                )
            }
            appendLine()
            appendLine("## Lifecycle Events")
            appendLine()
            if (snapshot.events.isEmpty()) {
                appendLine("(no events)")
            } else {
                snapshot.events.forEach { event ->
                    appendLine(
                        "- `${event.timestamp}` pid=${event.pid} agent=${escapeInline(event.agent)} " +
                            "${event.type}: ${event.message}",
                    )
                }
            }
            appendLine()
        }

    private fun escapeCell(value: String): String =
        value.replace("|", "\\|")

    private fun escapeInline(value: String): String =
        value.replace("`", "'")
}
