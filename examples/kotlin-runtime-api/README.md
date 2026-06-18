# Kotlin Runtime API Example

This example is for Kotlin/JVM developers who want to use KAI OS as a runtime library instead of starting with the CLI.

For the API shape and a smaller snippet, read [Kotlin Runtime API](../../docs/KOTLIN_API.md).

It demonstrates the core model in code:

```text
Agent    = Process
Workflow = Scheduler
Tool     = Syscall
Run      = Evidence
```

Run it from the repository root:

```bash
./gradlew -p examples/kotlin-runtime-api run
```

What the example does:

- creates four agents: `researcher`, `inspector`, `reviewer`, and `validator`.
- runs a priority-aware DAG through `WorkflowScheduler`.
- uses `LocalWorkerExecutorBackend(parallelism = 2)` to show local worker leases.
- gives each agent explicit tool capabilities.
- records syscall audit entries and process metrics.
- uses `MemoryIsolation.PROCESS` on recovery policy for the researcher.
- prints a process table and syscall ledger.

The output is deterministic because it uses `MockModelProvider`; no API key or network call is required.

Useful follow-up commands:

```bash
./gradlew test --no-daemon
./scripts/repository-ci-smoke.sh
```
