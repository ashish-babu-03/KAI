package ai.kaios

import kotlin.test.Test
import kotlin.test.assertTrue

class MockModelProviderTest {
    @Test
    fun `mock provider summarizes workspace index and context deterministically`() {
        val provider = MockModelProvider()
        val response = provider.complete(
            ModelRequest(
                runId = RunId("run-project"),
                agent = AgentSpec(AgentId("validator")),
                input = """
                    summarize this project

                    [KAIOS_WORKSPACE_INDEX]
                    root: /tmp/project
                    files: 2, lines: 3, bytes: 59

                    languages:
                    - Kotlin: 1 files, 1 lines
                    - Markdown: 1 files, 2 lines

                    notable files:
                    - README.md (Markdown, 2 lines)
                    - src/main/kotlin/App.kt (Kotlin, 1 lines)
                    [/KAIOS_WORKSPACE_INDEX]

                    [KAIOS_CONTEXT]
                    ### README.md
                    ```
                    # Demo
                    Useful public overview.
                    ```
                    [/KAIOS_CONTEXT]
                """.trimIndent(),
                dependencyContext = mapOf("executor" to "execute:abc project summary"),
            ),
        )

        assertTrue(response.content.contains("validate:"))
        assertTrue(response.content.contains("Project summary for 'summarize this project'"))
        assertTrue(response.content.contains("Shape: 2 files, 3 lines."))
        assertTrue(response.content.contains("Languages: Kotlin:1, Markdown:1."))
        assertTrue(response.content.contains("Notable files: README.md, src/main/kotlin/App.kt."))
        assertTrue(response.content.contains("Context used: README.md."))
        assertTrue(response.content.contains("kaios trace run-project --check"))
    }

    @Test
    fun `mock provider keeps existing validate contract without project input`() {
        val provider = MockModelProvider()
        val response = provider.complete(
            ModelRequest(
                runId = RunId("run-basic"),
                agent = AgentSpec(AgentId("validator")),
                input = "draft a release note",
                dependencyContext = mapOf("executor" to "execute:abc synthesized result"),
            ),
        )

        assertTrue(response.content.startsWith("validate:"))
        assertTrue(response.content.contains("accepted result from after executor"))
    }
}
