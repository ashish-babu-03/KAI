# Local Tour

Use the local tour when you want to feel what KAI OS does before reading the full docs.

The tour runs entirely on your machine with the deterministic provider. It does not need an API key.

```bash
./scripts/local-tour.sh
```

What it shows:

- `kaios next` chooses the next workspace-aware action.
- `kaios analyze . --format json` maps the project without a model call.
- `kaios run --index . --context README.md` creates a project-aware agent run.
- `kaios ps <run-id>` shows agents as OS-style processes.
- `kaios trace <run-id> --check` validates the process trace contract.
- `kaios evidence <run-id>` packages a portable capsule.
- `kaios replay --file <capsule>` proves the capsule can rebuild the trace offline.

The script writes handoff files to a temp directory and prints their paths:

```text
artifact: /tmp/kaios-tour.xxxxxx/project.md
trace: /tmp/kaios-tour.xxxxxx/trace.json
capsule: /tmp/kaios-tour.xxxxxx/run.capsule.json
analysis: /tmp/kaios-tour.xxxxxx/analysis.json
```

Run the same tour against another local project:

```bash
KAIOS_TOUR_WORKDIR=/path/to/project ./scripts/local-tour.sh "summarize this project"
```

Use a specific installed CLI:

```bash
KAIOS_BIN=/usr/local/bin/kaios ./scripts/local-tour.sh
```

Keep the output in a predictable directory:

```bash
KAIOS_TOUR_DIR=/tmp/kaios-tour ./scripts/local-tour.sh
```

The target workspace will still receive the normal `.kaios/runs/` snapshot directory because KAI OS persists inspectable run state there. The tour keeps shareable artifacts, traces, and capsules outside the repository unless you choose a directory inside it.
