# Why Star KAI OS

Star KAI OS if you want AI agent work to leave runtime evidence instead of disappearing into a chat transcript.

```text
KAI OS  = Evidence OS for AI agents
Agent   = Process
Workflow = Scheduler
Tool    = Syscall
Run     = Evidence
```

## The Short Case

Most agent projects optimize for prompts, provider calls, or chat UX. KAI OS is betting on a lower layer: the evidence around a run.

That means:

- process traces that show which agents ran, failed, recovered, and exited.
- syscall ledgers that show which tools were requested, allowed, denied, timed, and costed.
- replayable capsules that can travel with a PR, incident, bug report, or CI artifact.
- baseline gates that can fail a build when stable runtime behavior changes.
- a no-key first run, so the proof works before any hosted model or secret is configured.

## Star If

- You build Kotlin/JVM services and want agent infrastructure with typed runtime boundaries.
- You maintain an OSS repo and want AI review output that can be inspected and replayed.
- You care about agent observability but do not want proof locked inside one hosted dashboard.
- You think tool access should look like capabilities and syscalls, not arbitrary model action.
- You want CI to gate agent runtime behavior, not only compile code.

## What A Star Helps

Early stars are not decoration. They tell Kotlin/JVM developers, maintainers, and infrastructure teams that this direction is worth inspecting.

A star helps KAI OS get:

- more feedback on the Evidence OS model.
- more Kotlin API review from real JVM users.
- more small docs and examples from contributors.
- enough visibility to test whether local-first agent evidence should become a category.

## Fast Proof Before You Star

No install:

- [Proof Pack](https://morning-verlu.github.io/KAI/proof-pack.html)
- [Evidence Viewer](https://morning-verlu.github.io/KAI/evidence-viewer.html)

Local no-key tour:

```bash
curl -fsSL https://morning-verlu.github.io/KAI/install.sh | sh
export PATH="$HOME/.kaios/bin:$PATH"
kaios tour
```

Source checkout verification:

```bash
./scripts/evidence-samples-smoke.sh
./scripts/repository-ci-smoke.sh
```

## Watch Or Fork If

Watch releases if you want the next milestones: stronger `kaios review` artifacts, better capsule replay, public CI templates, and more syscall sandbox examples.

Fork if you want to try KAI OS against your own JVM service, write a Kotlin API recipe, add a denied-syscall walkthrough, or test the no-key first-run path in Codespaces or Docker.

## Skip It If

KAI OS is early and low-level. It is probably not what you need if you want a polished hosted chatbot, a visual workflow builder today, or a mature provider marketplace.

That is the point of the star test: star it if the Evidence OS direction should exist, even while the implementation is still small.
