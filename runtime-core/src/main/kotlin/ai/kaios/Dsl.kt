package ai.kaios

fun agent(id: String, block: AgentBuilder.() -> Unit = {}): AgentSpec =
    AgentBuilder(id).apply(block).build()

class AgentBuilder(
    private val id: String,
) {
    private val tools = linkedSetOf<String>()
    private val permissions = linkedSetOf<ToolPermission>()
    private var instruction: String = ""
    private var memoryEnabled: Boolean = false

    fun instruction(value: String) {
        instruction = value
    }

    fun tool(name: String) {
        tools += name
        when (name) {
            "echo" -> permissions += ToolPermission.ECHO
            "clock" -> permissions += ToolPermission.READ_CLOCK
            "mock-http" -> permissions += ToolPermission.NETWORK
            "http" -> permissions += ToolPermission.NETWORK
            "file" -> permissions += ToolPermission.FILE
        }
    }

    fun permission(permission: ToolPermission) {
        permissions += permission
    }

    fun memory(store: MemoryStore) {
        memoryEnabled = store !is NoopMemoryStore
    }

    fun build(): AgentSpec = AgentSpec(
        id = AgentId(id),
        instruction = instruction,
        allowedTools = tools.toSet(),
        permissions = permissions.toSet(),
        memoryEnabled = memoryEnabled,
    )
}

fun workflow(name: String, block: WorkflowBuilder.() -> Unit): Workflow =
    WorkflowBuilder(name).apply(block).build()

class WorkflowBuilder(
    private val name: String,
) {
    private val nodes = linkedMapOf<String, WorkflowNodeBuilder>()

    fun node(id: String, agent: AgentSpec = agent(id)): WorkflowNodeBuilder {
        val builder = WorkflowNodeBuilder(id, agent)
        nodes[id] = builder
        return builder
    }

    fun build(): Workflow = Workflow(name, nodes.values.map { it.build() })
}

class WorkflowNodeBuilder(
    private val id: String,
    private val agent: AgentSpec,
) {
    private val dependencies = linkedSetOf<String>()
    private var fallback: String? = null
    private var fallbackOnly: Boolean = false
    private var maxAttempts: Int = 1

    fun dependsOn(vararg ids: String): WorkflowNodeBuilder = apply {
        dependencies += ids
    }

    fun fallbackTo(id: String): WorkflowNodeBuilder = apply {
        fallback = id
    }

    fun fallbackOnly(): WorkflowNodeBuilder = apply {
        fallbackOnly = true
    }

    fun retries(count: Int): WorkflowNodeBuilder = apply {
        require(count >= 0) { "Retry count cannot be negative." }
        maxAttempts = count + 1
    }

    fun build(): WorkflowNode = WorkflowNode(
        id = id,
        agent = agent,
        dependencies = dependencies.toSet(),
        fallback = fallback,
        fallbackOnly = fallbackOnly,
        maxAttempts = maxAttempts,
    )
}
