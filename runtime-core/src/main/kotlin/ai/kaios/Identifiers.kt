package ai.kaios

import java.util.UUID

@JvmInline
value class AgentId(val value: String) {
    init {
        require(value.isNotBlank()) { "AgentId cannot be blank." }
    }

    override fun toString(): String = value
}

@JvmInline
value class ProcessId(val value: Long) {
    init {
        require(value > 0) { "ProcessId must be positive." }
    }

    override fun toString(): String = value.toString()
}

@JvmInline
value class RunId(val value: String) {
    init {
        require(value.isNotBlank()) { "RunId cannot be blank." }
    }

    override fun toString(): String = value

    companion object {
        fun new(): RunId = RunId("run-${UUID.randomUUID().toString().take(8)}")
    }
}
