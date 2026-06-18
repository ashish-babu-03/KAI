# Post Now

Status: draft, not posted.

Use this page when you want the next manual launch action without reading every launch draft. For channel choice and source notes, see [community-targets.md](community-targets.md).
After publishing, use [follow-up-playbook.md](follow-up-playbook.md) for replies, metric checks, and channel-switch decisions.

Current diagnosis:

- GitHub repo and website conversion paths are ready.
- The Reddit attempt was removed before a valid exposure window.
- The text-only X post did not create enough measurable reach.
- The best next channel is Show HN if the maintainer can monitor replies; otherwise use a focused Kotlin/JVM feedback post.

## Preflight

Run:

```bash
./scripts/launch-metrics.sh
```

Confirm the current public links:

```text
Evidence Viewer:
https://morning-verlu.github.io/KAI/evidence-viewer.html

Product-proof image:
https://morning-verlu.github.io/KAI/assets/kaios-evidence-proof.png

Contributor Board:
https://github.com/morning-verlu/KAI/blob/main/docs/CONTRIBUTOR_BOARD.md

Kotlin/JVM evaluation path:
https://github.com/morning-verlu/KAI/blob/main/docs/KOTLIN_JVM_EVALUATION.md

Kotlin/JVM feedback discussion:
https://github.com/morning-verlu/KAI/discussions/17

Proof Pack:
https://morning-verlu.github.io/KAI/proof-pack.html
```

## 1. Show HN

Use this first if the maintainer can answer comments for the first 2-3 hours.

Title:

```text
Show HN: KAI OS - an Evidence OS for Kotlin AI agents
```

URL:

```text
https://github.com/morning-verlu/KAI
```

Text:

```text
I am building KAI OS, a local-first runtime that turns AI agent runs into evidence developers can inspect, replay, and gate in CI.

The model is:

KAI OS  = Evidence OS for AI agents
Agent   = Process
Workflow = Scheduler
Tool    = Syscall
Run     = Evidence

The goal is not to build another chatbot framework or Kotlin LangChain clone. I am trying to make agent work look more like runtime infrastructure: process traces, syscall ledgers, replayable capsules, baseline diffs, and deterministic no-key first runs.

The current release is early but runnable:

curl -fsSL https://morning-verlu.github.io/KAI/install.sh | sh
export PATH="$HOME/.kaios/bin:$PATH"
kaios tour

I would especially like feedback from Kotlin/JVM developers, OSS maintainers, and people building agent infrastructure: would portable run evidence help you trust or debug agent-driven review/CI work?
```

First comment:

```text
For anyone who wants proof before installing:

Proof Pack:
https://morning-verlu.github.io/KAI/proof-pack.html

Evidence Viewer:
https://morning-verlu.github.io/KAI/evidence-viewer.html
```

## 2. Kotlin/JVM Community

Use this for Kotlin Slack, Kotlin forum, JVM backend groups, or a Kotlin-focused Discord.

### 30-Second Version

Use this when the channel favors short posts:

```text
I am building KAI OS, a Kotlin/JVM Evidence OS for AI agent runs.

The runtime model is:

Agent = Process
Workflow = Scheduler
Tool = Syscall
Run = Evidence

The goal is not another Kotlin LangChain clone. It is local runtime evidence:
process traces, syscall ledgers, replayable capsules, and CI baseline gates.

No install needed to inspect the product surface:
https://morning-verlu.github.io/KAI/evidence-viewer.html?utm_source=kotlin_community&utm_medium=community&utm_campaign=post_now

Kotlin/JVM evaluation path:
https://github.com/morning-verlu/KAI/blob/main/docs/KOTLIN_JVM_EVALUATION.md

Focused feedback discussion:
https://github.com/morning-verlu/KAI/discussions/17

I would love feedback on whether the API shape feels idiomatic Kotlin and whether replayable capsules would help JVM maintainers trust agent reviews.
```

First reply if the post gets any response:

```text
The shortest proof is the checked-in Evidence Viewer. It shows a real run as
process table + syscall ledger + replayable capsule + baseline gate:
https://morning-verlu.github.io/KAI/evidence-viewer.html
```

Record immediately in #7:

```text
Manual post published:

- Channel: Kotlin/JVM community
- URL:
- UTM campaign: post_now
- Baseline metrics:
- Next check: +2h
```

### Longer Version

Use this when the channel allows more context:

```text
I am building KAI OS, a Kotlin/JVM runtime for AI agent evidence.

It is not trying to be a Kotlin LangChain clone. The runtime model is closer to OS infrastructure:

Agent = Process
Workflow = Scheduler
Tool = Syscall
Run = Evidence

Proof before install:
https://morning-verlu.github.io/KAI/proof-pack.html?utm_source=kotlin_community&utm_medium=community&utm_campaign=post_now

The Proof Pack links a checked-in run as a process table, syscall ledger, replayable capsule, and baseline gate.

The Kotlin API example is here:
https://github.com/morning-verlu/KAI/tree/main/examples/kotlin-runtime-api

Kotlin/JVM evaluation path:
https://github.com/morning-verlu/KAI/blob/main/docs/KOTLIN_JVM_EVALUATION.md

Kotlin/JVM feedback form:
https://github.com/morning-verlu/KAI/issues/new?template=kotlin_api_feedback.yml

Focused GitHub Discussion:
https://github.com/morning-verlu/KAI/discussions/17

Contributor board:
https://github.com/morning-verlu/KAI/blob/main/docs/CONTRIBUTOR_BOARD.md

I would especially like feedback on:

- whether the Agent = Process model is useful for JVM teams
- whether the Kotlin runtime API feels idiomatic
- whether replayable capsules and CI evidence gates would help maintainers trust agent reviews
```

After posting, record the URL in issue #7 and run:

```bash
./scripts/launch-metrics.sh
```

Then follow [follow-up-playbook.md](follow-up-playbook.md) at +2h, +24h, and +72h.

## 3. X / LinkedIn Visual

Use this if the maintainer account has an engineering audience.

Attach:

```text
https://morning-verlu.github.io/KAI/assets/kaios-evidence-proof.png
```

Post:

```text
Most AI agents fail like scripts. KAI OS treats them like processes.

I am building a local-first Evidence OS for AI agents in Kotlin.

The model:

Agent = Process
Workflow = Scheduler
Tool = Syscall
Run = Evidence

Each run becomes inspectable evidence:

- process traces
- syscall ledgers
- replayable capsules
- CI baseline gates

Proof before install:
https://morning-verlu.github.io/KAI/proof-pack.html?utm_source=linkedin&utm_medium=social&utm_campaign=post_now

Repo:
https://github.com/morning-verlu/KAI

Feedback welcome from Kotlin/JVM developers, OSS maintainers, and people building agent infrastructure.
```

## Record After Posting

Add a comment to issue #7:

```text
Manual post published:

- Channel:
- URL:
- UTM campaign: post_now
- Baseline metrics:
- Next check: +2h
```

Then capture follow-up metrics:

```bash
./scripts/launch-metrics.sh
```

## Reply Shortcuts

Why not LangChain/Koog/LangChain4j?

```text
KAI OS is lower-level. The bet is not more agent abstractions; it is portable runtime evidence: process traces, syscall records, replayable capsules, recovery evidence, and baseline diffs that can be inspected later or gated in CI.
```

Why Kotlin?

```text
The JVM already runs a lot of backend and CI infrastructure, and Kotlin gives the runtime a typed API plus DSL ergonomics. The project is trying to make agent evidence feel native to JVM teams rather than bolted on from Python.
```

Where can I inspect it without installing?

```text
Evidence Viewer:
https://morning-verlu.github.io/KAI/evidence-viewer.html

Checked-in evidence samples:
https://github.com/morning-verlu/KAI/tree/main/examples/evidence-sample
```
