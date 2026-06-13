package ai.kaios

import java.time.Duration
import java.time.Instant
import kotlin.math.max

enum class ProcessState {
    CREATED,
    RUNNING,
    SUSPENDED,
    SUCCEEDED,
    FAILED,
    CANCELLED,
}

val ProcessState.isTerminal: Boolean
    get() = this == ProcessState.SUCCEEDED || this == ProcessState.FAILED || this == ProcessState.CANCELLED

data class TokenUsage(
    val input: Int = 0,
    val output: Int = 0,
) {
    val total: Int
        get() = input + output
}

data class AgentSpec(
    val id: AgentId,
    val displayName: String = id.value,
    val instruction: String = "",
    val allowedTools: Set<String> = emptySet(),
    val permissions: Set<ToolPermission> = emptySet(),
    val memoryEnabled: Boolean = true,
)

data class AgentProcess(
    val pid: ProcessId,
    val runId: RunId,
    val agent: AgentId,
    val state: ProcessState,
    val tokenUsage: TokenUsage = TokenUsage(),
    val contextSize: Int = 0,
    val syscallCount: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val failure: String? = null,
) {
    val durationMillis: Long
        get() {
            val start = startedAt ?: createdAt
            val end = completedAt ?: updatedAt
            return max(0, Duration.between(start, end).toMillis())
        }
}

enum class RuntimeEventType {
    SPAWNED,
    STARTED,
    SUSPENDED,
    RESUMED,
    TOOL_CALLED,
    MEMORY_APPENDED,
    SUCCEEDED,
    FAILED,
    CANCELLED,
}

data class RuntimeEvent(
    val runId: RunId,
    val pid: ProcessId,
    val agent: AgentId,
    val type: RuntimeEventType,
    val message: String,
    val timestamp: Instant,
)
