package ai.kaios

import java.time.Clock

class AgentRuntime(
    private val clock: Clock = Clock.systemUTC(),
) {
    private val lock = Any()
    private var nextPid = 1L
    private val processes = linkedMapOf<ProcessId, AgentProcess>()
    private val events = mutableListOf<RuntimeEvent>()

    fun spawn(agent: AgentSpec, runId: RunId): AgentProcess = synchronized(lock) {
        val now = clock.instant()
        val process = AgentProcess(
            pid = ProcessId(nextPid++),
            runId = runId,
            agent = agent.id,
            state = ProcessState.CREATED,
            createdAt = now,
            updatedAt = now,
        )
        processes[process.pid] = process
        emit(process, RuntimeEventType.SPAWNED, "spawned '${agent.displayName}'")
        process
    }

    fun start(pid: ProcessId): AgentProcess = transition(
        pid = pid,
        allowed = setOf(ProcessState.CREATED),
        next = ProcessState.RUNNING,
        type = RuntimeEventType.STARTED,
        message = "started",
    ) { process, now -> process.copy(startedAt = now, updatedAt = now) }

    fun suspend(pid: ProcessId): AgentProcess = transition(
        pid = pid,
        allowed = setOf(ProcessState.RUNNING),
        next = ProcessState.SUSPENDED,
        type = RuntimeEventType.SUSPENDED,
        message = "suspended",
    )

    fun resume(pid: ProcessId): AgentProcess = transition(
        pid = pid,
        allowed = setOf(ProcessState.SUSPENDED),
        next = ProcessState.RUNNING,
        type = RuntimeEventType.RESUMED,
        message = "resumed",
    )

    fun cancel(pid: ProcessId): AgentProcess = synchronized(lock) {
        val process = requireProcess(pid)
        check(!process.state.isTerminal) { "Cannot cancel terminal process $pid in state ${process.state}." }
        val now = clock.instant()
        val updated = process.copy(state = ProcessState.CANCELLED, updatedAt = now, completedAt = now)
        processes[pid] = updated
        emit(updated, RuntimeEventType.CANCELLED, "cancelled")
        updated
    }

    fun recordSyscall(pid: ProcessId, result: ToolResult): AgentProcess = synchronized(lock) {
        val process = requireProcess(pid)
        check(process.state == ProcessState.RUNNING) { "Process $pid must be RUNNING to record a syscall." }
        val updated = process.copy(
            syscallCount = process.syscallCount + 1,
            updatedAt = clock.instant(),
        )
        processes[pid] = updated
        val message = if (result.ok) {
            "syscall ${result.tool} -> ok"
        } else {
            "syscall ${result.tool} -> denied: ${result.error}"
        }
        emit(updated, RuntimeEventType.TOOL_CALLED, message)
        updated
    }

    fun recordMemory(pid: ProcessId, entry: MemoryEntry): AgentProcess = synchronized(lock) {
        val process = requireProcess(pid)
        check(process.state == ProcessState.RUNNING) { "Process $pid must be RUNNING to append memory." }
        val updated = process.copy(updatedAt = clock.instant())
        processes[pid] = updated
        emit(updated, RuntimeEventType.MEMORY_APPENDED, "memory ${entry.role} +${entry.content.length} chars")
        updated
    }

    fun succeed(pid: ProcessId, tokenUsage: TokenUsage, contextSize: Int): AgentProcess = transition(
        pid = pid,
        allowed = setOf(ProcessState.RUNNING),
        next = ProcessState.SUCCEEDED,
        type = RuntimeEventType.SUCCEEDED,
        message = "succeeded",
    ) { process, now ->
        process.copy(
            tokenUsage = tokenUsage,
            contextSize = contextSize,
            updatedAt = now,
            completedAt = now,
        )
    }

    fun fail(pid: ProcessId, error: String): AgentProcess = transition(
        pid = pid,
        allowed = setOf(ProcessState.RUNNING, ProcessState.CREATED, ProcessState.SUSPENDED),
        next = ProcessState.FAILED,
        type = RuntimeEventType.FAILED,
        message = error,
    ) { process, now ->
        process.copy(failure = error, updatedAt = now, completedAt = now)
    }

    fun process(pid: ProcessId): AgentProcess? = synchronized(lock) {
        processes[pid]
    }

    fun processes(runId: RunId? = null): List<AgentProcess> = synchronized(lock) {
        processes.values.filter { runId == null || it.runId == runId }
    }

    fun events(runId: RunId? = null): List<RuntimeEvent> = synchronized(lock) {
        events.filter { runId == null || it.runId == runId }
    }

    private fun transition(
        pid: ProcessId,
        allowed: Set<ProcessState>,
        next: ProcessState,
        type: RuntimeEventType,
        message: String,
        mutate: (AgentProcess, java.time.Instant) -> AgentProcess = { process, now -> process.copy(updatedAt = now) },
    ): AgentProcess = synchronized(lock) {
        val process = requireProcess(pid)
        check(process.state in allowed) { "Invalid transition for process $pid: ${process.state} -> $next." }
        val now = clock.instant()
        val updated = mutate(process.copy(state = next), now)
        processes[pid] = updated
        emit(updated, type, message)
        updated
    }

    private fun requireProcess(pid: ProcessId): AgentProcess =
        processes[pid] ?: error("Unknown process $pid.")

    private fun emit(process: AgentProcess, type: RuntimeEventType, message: String) {
        events += RuntimeEvent(
            runId = process.runId,
            pid = process.pid,
            agent = process.agent,
            type = type,
            message = message,
            timestamp = clock.instant(),
        )
    }
}
