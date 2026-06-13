package ai.kaios

import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemoryEngineTest {
    private val clock = Clock.fixed(Instant.parse("2026-06-13T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `session memory appends reads and clears entries by run`() {
        val store = SessionMemoryStore()
        val runId = RunId("run-memory")
        val agent = AgentId("planner")

        store.append(MemoryEntry(runId, agent, "user", "task", clock.instant()))
        store.append(MemoryEntry(RunId("run-other"), agent, "user", "other", clock.instant()))

        assertEquals(listOf("task"), store.read(runId, agent).map { it.content })

        store.clear(runId)
        assertTrue(store.read(runId, agent).isEmpty())
        assertEquals(listOf("other"), store.read(RunId("run-other"), agent).map { it.content })
    }

    @Test
    fun `snapshot store writes and reads workflow result JSON`() {
        val runtime = AgentRuntime(clock)
        val runId = RunId("run-snapshot")
        val process = runtime.spawn(AgentSpec(AgentId("validator")), runId)
        runtime.start(process.pid)
        runtime.succeed(process.pid, TokenUsage(input = 2, output = 3), contextSize = 64)

        val result = WorkflowResult(
            runId = runId,
            workflowName = "snapshot",
            success = true,
            outputs = emptyMap(),
            finalOutput = "ok",
            processes = runtime.processes(runId),
            events = runtime.events(runId),
        )

        val root = Files.createTempDirectory("kaios-runs")
        val store = FileRunSnapshotStore(root)
        val path = store.save("snapshot task", result)
        val loaded = store.load(runId)

        assertTrue(Files.exists(path))
        assertEquals("run-snapshot", loaded.runId)
        assertEquals("snapshot task", loaded.task)
        assertEquals("ok", loaded.finalOutput)
        assertEquals(5, loaded.processes.single().tokens)
        assertEquals("SUCCEEDED", loaded.processes.single().state)
    }

    @Test
    fun `snapshot store lists saved run snapshots`() {
        val root = Files.createTempDirectory("kaios-runs-list")
        val store = FileRunSnapshotStore(root)
        val runtime = AgentRuntime(clock)

        listOf("run-list-a", "run-list-b").forEach { value ->
            val runId = RunId(value)
            val process = runtime.spawn(AgentSpec(AgentId("planner")), runId)
            runtime.start(process.pid)
            runtime.succeed(process.pid, TokenUsage(input = 1, output = 2), contextSize = 10)
            store.save(
                task = "task $value",
                result = WorkflowResult(
                    runId = runId,
                    workflowName = "default",
                    success = true,
                    outputs = emptyMap(),
                    finalOutput = "ok",
                    processes = runtime.processes(runId),
                    events = runtime.events(runId),
                ),
            )
        }

        val listed = store.list()

        assertEquals(setOf("run-list-a", "run-list-b"), listed.map { it.runId }.toSet())
    }
}
