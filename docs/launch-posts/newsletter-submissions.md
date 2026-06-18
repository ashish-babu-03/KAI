# Newsletter Submission Pack

Status: draft, not submitted.

Use this after Show HN, Kotlin/JVM feedback, or the technical article produces a clearer public trail. Newsletter submissions work best when the project already has proof links and a concise category claim.

## Target Lists

Start with developer audiences that understand Kotlin, JVM, CI, observability, or agent infrastructure:

- Kotlin Weekly
- JVM Weekly
- Java Weekly
- AI engineering newsletters
- LLMOps / observability newsletters
- OSS maintainer newsletters

Do not submit everywhere at once. Submit to one or two relevant lists, wait for a response or publication window, then continue.

## One-Sentence Pitch

```text
KAI OS is a local-first Kotlin/JVM Evidence OS for AI agents: process traces, syscall ledgers, replayable capsules, and CI gates for agent runs.
```

## Short Submission

```text
KAI OS is an early Kotlin/JVM Evidence OS for AI agents.

The model is:

Agent = Process
Workflow = Scheduler
Tool = Syscall
Run = Evidence

It is not a chatbot framework or Kotlin LangChain clone. It focuses on portable runtime evidence: process traces, syscall ledgers, replayable capsules, and CI baseline gates.

Proof Pack:
https://morning-verlu.github.io/KAI/proof-pack.html

Repository:
https://github.com/morning-verlu/KAI
```

## Longer Submission

```text
KAI OS is a local-first Kotlin/JVM runtime that turns AI agent runs into inspectable evidence.

Instead of treating an agent run as a chat transcript, KAI OS models it with operating-system style primitives:

Agent = Process
Workflow = Scheduler
Tool = Syscall
Run = Evidence

The current release is early but runnable. It includes a no-key `kaios tour`, checked-in evidence samples, process trace JSON, syscall ledger records, replayable capsules, and a CI-style baseline gate.

The goal is not another agent framework. The goal is portable runtime evidence that maintainers can inspect, replay, compare, and attach to PRs or CI.

Proof Pack:
https://morning-verlu.github.io/KAI/proof-pack.html

Evidence Viewer:
https://morning-verlu.github.io/KAI/evidence-viewer.html

Repository:
https://github.com/morning-verlu/KAI
```

## Links To Include

| Link | Use |
| --- | --- |
| `https://github.com/morning-verlu/KAI` | Main repo and star target |
| `https://morning-verlu.github.io/KAI/proof-pack.html` | Skeptical developer proof |
| `https://morning-verlu.github.io/KAI/evidence-viewer.html` | Visual no-install proof |
| `https://github.com/morning-verlu/KAI/blob/main/docs/KOTLIN_JVM_EVALUATION.md` | Kotlin/JVM evaluator path |
| `https://github.com/morning-verlu/KAI/blob/main/docs/WHY_STAR.md` | Star/watch/fork rationale |

## Submission Log Template

Record each submission in issue #7 or a dedicated launch issue:

```text
Newsletter submission:

- Outlet:
- Submitted at:
- Submitted by:
- URL or contact path:
- Copy variant:
- Baseline stars/forks/watchers:
- Baseline GitHub views:
- Follow-up date:
```

## Decision Rules

| Signal | Next action |
| --- | --- |
| Accepted or published | Capture metrics at +24h and +72h |
| No response after one week | Submit to the next adjacent list |
| GitHub views rise, stars flat | Improve README / Proof Pack / Why Star before more submissions |
| Stars rise, no issues | Point readers to Contributor Board and good-first issues |
