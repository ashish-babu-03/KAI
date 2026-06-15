# Changelog

## 0.3.0 - Evidence Core

KAI OS v0.3 turns the runtime evidence path into the product center: agent work is now easier to recover, schedule, audit, package, and gate in CI without an API key.

Highlights:

- Process recovery evidence: runtime crashes are recorded as `FAILED + RUNTIME_CRASH`, recovered attempts get new PIDs, and `kaios recover <run-id> --dry-run` explains recovery status without mutating saved runs.
- Priority scheduler evidence: ready DAG nodes run by priority with stable ordering, event-triggered nodes wait for matching runtime events, and local worker execution records `workerId`.
- Syscall ledger: every tool call, including denied calls, records audit data with redacted arguments, duration, denied status, and estimated cost.
- Evidence JSON expansion: trace, capsule, review, evidence, and `kaios ps --json` now expose scheduler, syscalls, cost, and recovery fields while keeping existing schema meanings stable.
- Config expansion: `kaios.json` supports `priority`, `recovery`, `triggers`, `capabilities`, `executorHint`, and `memoryIsolation`.
- Docs now position KAI OS around the local-first Evidence OS path: `kaios quickstart`, `kaios review`, and `kaios evidence --baseline ... --check`.

Verification:

```bash
./gradlew test installDist --no-daemon
```

## 0.1.84

Initial public runnable seed with the Kotlin/JVM runtime core, CLI, deterministic mock provider, project config templates, process traces, run capsules, offline replay/diff, Agent Gate, Workspace Index, context loading, provider adapters, safe built-in tools, launch site, installer, and demo assets.
