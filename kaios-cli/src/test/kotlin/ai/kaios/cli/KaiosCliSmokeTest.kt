package ai.kaios.cli

import ai.kaios.FileRunSnapshotStore
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KaiosCliSmokeTest {
    @Test
    fun `run ps and inspect work against a saved mock run`() {
        val root = Files.createTempDirectory("kaios-cli-runs")
        val cli = KaiosCli(FileRunSnapshotStore(root))

        val runOut = ByteArrayOutputStream()
        val runCode = cli.run(
            arrayOf("run", "analyze", "crypto", "market"),
            PrintStream(runOut),
            PrintStream(ByteArrayOutputStream()),
        )
        val runText = runOut.toString()
        val runId = Regex("run_id: (\\S+)").find(runText)?.groupValues?.get(1)

        assertEquals(0, runCode)
        assertTrue(runText.contains("validate:"))
        assertTrue(runId != null)

        val psOut = ByteArrayOutputStream()
        val psCode = cli.run(
            arrayOf("ps", runId),
            PrintStream(psOut),
            PrintStream(ByteArrayOutputStream()),
        )
        val psText = psOut.toString()

        assertEquals(0, psCode)
        assertTrue(psText.contains("planner"))
        assertTrue(psText.contains("executor"))
        assertTrue(psText.contains("validator"))
        assertTrue(psText.contains("SYSCALLS"))

        val inspectOut = ByteArrayOutputStream()
        val inspectCode = cli.run(
            arrayOf("inspect", runId),
            PrintStream(inspectOut),
            PrintStream(ByteArrayOutputStream()),
        )
        val inspectText = inspectOut.toString()

        assertEquals(0, inspectCode)
        assertTrue(inspectText.contains("events:"))
        assertTrue(inspectText.contains("SPAWNED"))
        assertTrue(inspectText.contains("SUCCEEDED"))
    }
}
