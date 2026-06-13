# Project Config

KAI OS can run the built-in `planner -> executor -> validator` workflow, or load an editable project workflow from `kaios.json`.

```bash
kaios init
kaios run --config kaios.json "map the JVM agent runtime"
```

`kaios init` refuses to overwrite an existing file unless you pass `--force`:

```bash
kaios init --force
```

Use a different path with `--config`:

```bash
kaios init --config workflows/research.json
kaios run --config workflows/research.json "analyze a release plan"
```

## Shape

```json
{
  "name": "custom-research",
  "agents": [
    {
      "id": "researcher",
      "instruction": "Gather useful context for the task.",
      "tools": ["echo", "clock"],
      "dependsOn": [],
      "memory": true
    },
    {
      "id": "writer",
      "instruction": "Write a concise answer.",
      "tools": ["echo"],
      "dependsOn": ["researcher"],
      "memory": true
    },
    {
      "id": "validator",
      "instruction": "Check the answer and mark it accepted.",
      "tools": ["echo"],
      "dependsOn": ["writer"],
      "memory": true
    }
  ]
}
```

Fields:

- `name`: workflow name shown in snapshots, `kaios ps`, and `kaios inspect`.
- `agents`: ordered list of agent process nodes.
- `id`: unique agent process name.
- `instruction`: system-style guidance passed to the configured model provider.
- `tools`: syscall tools the agent may call.
- `dependsOn`: upstream agent ids that must complete before this agent is scheduled.
- `memory`: enables the configured memory store for that agent. Defaults to `true`.
- `fallback`: optional fallback agent id to run when this node fails.
- `fallbackOnly`: keeps a node out of the normal DAG and reserves it for fallback routing.

## Built-In Tools

The v0.1 safe syscall set is intentionally small:

- `echo`: returns a supplied message.
- `clock`: returns the current UTC timestamp.
- `mock-http`: returns a deterministic mocked HTTP response.
- `file`: reads, writes, lists, and checks files inside `.kaios/files`.

Tool names are validated before any agent process starts. Unknown tools fail fast.

## Validation

The CLI validates project configs before spawning agents:

- workflow name is not blank.
- at least one non-fallback agent exists.
- agent ids are non-blank and unique.
- every tool exists in the built-in registry.
- every dependency points to a known agent.
- every fallback points to a known agent and does not point to itself.
- dependency edges do not contain cycles.

## Observability

Configured workflows use the same process observability as the built-in workflow:

```bash
kaios ps <run-id>
kaios inspect <run-id>
kaios report <run-id>
```

Snapshots are still written under `.kaios/runs/`, so custom workflows can be inspected later without re-running the task.
