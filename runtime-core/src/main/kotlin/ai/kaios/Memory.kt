package ai.kaios

import java.time.Instant

data class MemoryEntry(
    val runId: RunId,
    val agent: AgentId,
    val role: String,
    val content: String,
    val timestamp: Instant,
)

interface MemoryStore {
    fun append(entry: MemoryEntry)

    fun read(runId: RunId, agent: AgentId? = null): List<MemoryEntry>

    fun clear(runId: RunId)
}

object NoopMemoryStore : MemoryStore {
    override fun append(entry: MemoryEntry) = Unit

    override fun read(runId: RunId, agent: AgentId?): List<MemoryEntry> = emptyList()

    override fun clear(runId: RunId) = Unit
}
