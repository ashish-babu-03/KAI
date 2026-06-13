package ai.kaios.cli

import ai.kaios.MemoryStore
import ai.kaios.ToolRegistry
import ai.kaios.agent
import ai.kaios.workflow
import ai.kaios.Workflow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

internal const val KAIOS_CONFIG_FILE = "kaios.json"

@Serializable
internal data class KaiosProjectConfig(
    val name: String = "default",
    val agents: List<KaiosAgentConfig> = emptyList(),
)

@Serializable
internal data class KaiosAgentConfig(
    val id: String,
    val instruction: String = "",
    val tools: List<String> = emptyList(),
    val dependsOn: List<String> = emptyList(),
    val fallback: String? = null,
    val fallbackOnly: Boolean = false,
    val memory: Boolean = true,
)

internal val kaiosConfigJson: Json = Json {
    prettyPrint = true
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = false
}

internal fun defaultProjectConfig(): KaiosProjectConfig =
    KaiosProjectConfig(
        name = "default",
        agents = listOf(
            KaiosAgentConfig(
                id = "planner",
                instruction = "Plan the task as an agent process.",
                tools = listOf("echo", "clock"),
            ),
            KaiosAgentConfig(
                id = "executor",
                instruction = "Execute the plan through permitted syscalls.",
                tools = listOf("echo", "mock-http"),
                dependsOn = listOf("planner"),
            ),
            KaiosAgentConfig(
                id = "validator",
                instruction = "Validate the executor output.",
                tools = listOf("echo"),
                dependsOn = listOf("executor"),
            ),
        ),
    )

internal fun defaultProjectConfigText(): String =
    kaiosConfigJson.encodeToString(defaultProjectConfig()) + "\n"

internal fun loadProjectWorkflow(path: Path, memory: MemoryStore, tools: ToolRegistry): Workflow {
    require(path.exists()) { "Config file '$path' was not found." }
    val config = runCatching {
        kaiosConfigJson.decodeFromString<KaiosProjectConfig>(path.readText())
    }.getOrElse { failure ->
        error("Invalid KAI OS config '$path': ${failure.message}")
    }
    return config.toWorkflow(memory, tools.names)
}

internal fun KaiosProjectConfig.toWorkflow(memory: MemoryStore, knownTools: Set<String>): Workflow {
    validate(knownTools)

    val specs = agents.associate { configuredAgent ->
        configuredAgent.id to agent(configuredAgent.id) {
            instruction(configuredAgent.instruction)
            configuredAgent.tools.forEach { tool(it) }
            if (configuredAgent.memory) this.memory(memory)
        }
    }

    return workflow(name) {
        agents.forEach { configuredAgent ->
            node(configuredAgent.id, specs.getValue(configuredAgent.id)).apply {
                if (configuredAgent.dependsOn.isNotEmpty()) {
                    dependsOn(*configuredAgent.dependsOn.toTypedArray())
                }
                configuredAgent.fallback?.let { fallbackTo(it) }
                if (configuredAgent.fallbackOnly) fallbackOnly()
            }
        }
    }
}

private fun KaiosProjectConfig.validate(knownTools: Set<String>) {
    require(name.isNotBlank()) { "Config field 'name' cannot be blank." }
    require(agents.isNotEmpty()) { "Config field 'agents' must contain at least one agent." }
    require(agents.any { !it.fallbackOnly }) { "Config must include at least one non-fallback agent." }

    val ids = agents.map { it.id }
    require(ids.all { it.isNotBlank() }) { "Agent id cannot be blank." }

    val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    require(duplicates.isEmpty()) { "Agent ids must be unique: ${duplicates.sorted().joinToString(", ")}." }

    val knownIds = ids.toSet()
    agents.forEach { configuredAgent ->
        val unknownTools = configuredAgent.tools.filterNot { it in knownTools }.toSortedSet()
        require(unknownTools.isEmpty()) {
            "Agent '${configuredAgent.id}' references unknown tool(s): ${unknownTools.joinToString(", ")}."
        }

        val unknownDependencies = configuredAgent.dependsOn.filterNot { it in knownIds }.toSortedSet()
        require(unknownDependencies.isEmpty()) {
            "Agent '${configuredAgent.id}' depends on unknown agent(s): ${unknownDependencies.joinToString(", ")}."
        }

        configuredAgent.fallback?.let { fallback ->
            require(fallback in knownIds) { "Agent '${configuredAgent.id}' references unknown fallback agent '$fallback'." }
            require(fallback != configuredAgent.id) { "Agent '${configuredAgent.id}' cannot fallback to itself." }
        }
    }

    dependencyCycle(ids, agents.associate { it.id to it.dependsOn })?.let { cycle ->
        error("Workflow dependencies contain a cycle: ${cycle.joinToString(" -> ")}.")
    }
}

private fun dependencyCycle(ids: List<String>, dependencies: Map<String, List<String>>): List<String>? {
    val visiting = linkedSetOf<String>()
    val visited = linkedSetOf<String>()

    fun visit(id: String): List<String>? {
        if (id in visited) return null
        if (id in visiting) {
            val cycleStart = visiting.indexOf(id)
            return visiting.drop(cycleStart) + id
        }

        visiting += id
        dependencies.getValue(id).forEach { dependency ->
            visit(dependency)?.let { return it }
        }
        visiting -= id
        visited += id
        return null
    }

    ids.forEach { id ->
        visit(id)?.let { return it }
    }
    return null
}
