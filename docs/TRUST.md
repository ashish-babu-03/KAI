# KAI OS Trust Contract

KAI OS is designed to make agent work observable before it becomes powerful.

The v0.1 trust boundary is simple: the default path runs without model credentials, avoids real network access, keeps side effects behind explicit tools, and writes evidence that can be inspected, replayed, and compared.

## Default Safety Posture

- No API key is required for `kaios quickstart`, `kaios review`, `kaios demo`, `kaios setup`, `kaios gate`, `kaios verify`, `kaios analyze`, `kaios index`, `kaios next`, `kaios doctor`, or `kaios bug-report`.
- The default model provider is `mock`, which is deterministic and local.
- `kaios gate` and `kaios verify` always run the smoke workflow with the deterministic mock provider.
- Real HTTP is disabled unless `KAIOS_HTTP_ALLOWLIST` is set.
- The built-in `mock-http` tool returns deterministic mock responses and does not use the network.
- Tool calls are registered syscalls; an agent can only call tools granted by its workflow and runtime permissions.
- The scoped file syscall rejects absolute paths and path traversal outside its configured root.

## Evidence Boundary

Every normal run can be inspected from `.kaios/runs/`:

```bash
kaios ps
kaios inspect
kaios trace --check
```

When the run needs to travel between people, machines, or CI jobs, package it:

```bash
kaios evidence --out artifacts/run.capsule.json --force
kaios replay --file artifacts/run.capsule.json
```

The evidence path is designed to prove runtime behavior without re-calling a model provider:

- `kaios.process-trace/v1` records process metrics, execution path, event counts, and lifecycle timeline.
- `kaios.run-capsule/v1` packages the snapshot, trace, provenance hashes, validation status, and replay commands.
- `kaios replay` rebuilds the trace from the embedded snapshot and checks it against the embedded trace.
- `kaios diff` compares capsules by stable runtime signature instead of run ids, timestamps, or duration noise.
- `kaios gate --baseline ... --check` turns that evidence into a CI-friendly regression check.

## Secret Handling

KAI OS commands should not print API keys, authorization headers, or secret environment values.

The support path is safe to paste by default:

```bash
kaios bug-report
kaios bug-report --json
```

The report includes doctor checks, config validation, latest run metrics, trace status, and a Fix First command. It does not intentionally include secrets.

You still control what files you attach as context. Do not pass secret files to `kaios run --context`, and add local exclusions to `.kaiosignore`:

```gitignore
secrets/
.env
*.pem
```

## What Is Not Promised In v0.1

- KAI OS is not a security sandbox for arbitrary custom tools.
- Real model providers can produce nondeterministic text; use the mock provider, traces, capsules, and baseline diffs when you need stable checks.
- Real HTTP is only as safe as the allowlist you configure.
- A run capsule is audit evidence, not a secret scrubber. Review artifacts before sharing outside your team.
- Public API stability is strongest around the CLI commands and JSON schemas documented in `docs/JSON_CONTRACTS.md`; provider-native function calling and UI surfaces are still future design space.

## Operational Checks

Use these commands when evaluating whether a repository is ready:

```bash
kaios next
kaios doctor
kaios config validate --json
kaios gate --config kaios.json
kaios bug-report
```

For CI, start from the generated Agent Gate:

```bash
kaios setup --ci
kaios gate --config kaios.json
```

The generated workflow uses the same local contract: deterministic provider, validated config, process trace validation, portable evidence capsule, and failure-time support report.
