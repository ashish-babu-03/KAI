# First External Wave

Status: ready to post, not posted by this file.

Goal: get the first real feedback and first real stars from people likely to understand the Evidence OS pitch.

Primary CTA:

```text
https://github.com/morning-verlu/KAI
```

Secondary CTA:

```text
https://morning-verlu.github.io/KAI/proof-pack.html
```

## Posting Order

1. Show HN with the GitHub repo URL and Proof Pack in the first comment.
2. Kotlin/JVM community post with a feedback-first angle.
3. X or LinkedIn technical thread with the evidence proof image.

Do not publish every draft everywhere at once. Post, watch replies, then adjust the next post.

## Post 1: Show HN

Use this first if the maintainer can answer comments for the first 2-3 hours.

Title:

```text
Show HN: KAI OS - an Evidence OS for Kotlin AI agents
```

URL:

```text
https://github.com/morning-verlu/KAI
```

```text
I am building KAI OS, a local-first runtime that turns AI agent runs into evidence developers can inspect, replay, and gate in CI.

The model is:

KAI OS = Evidence OS for AI agents
Agent = Process
Workflow = Scheduler
Tool = Syscall
Run = Evidence

The goal is not to build another chatbot framework or Kotlin LangChain clone. I am trying to make agent work look more like runtime infrastructure: process traces, syscall ledgers, replayable capsules, baseline diffs, and deterministic no-key first runs.

I would especially like feedback from Kotlin/JVM developers, OSS maintainers, and people building agent infrastructure.
```

First comment:

```text
Proof Pack:
https://morning-verlu.github.io/KAI/proof-pack.html

Evidence Viewer:
https://morning-verlu.github.io/KAI/evidence-viewer.html
```

## Post 2: Kotlin / JVM Community

Use this for Kotlin Slack, Kotlin forum, JVM backend groups, or a Kotlin-focused Discord.

```text
I am building KAI OS, a Kotlin/JVM runtime for AI agent evidence.

It is not trying to be a Kotlin LangChain clone. The runtime model is closer to OS infrastructure:

Agent = Process
Workflow = Scheduler
Tool = Syscall
Run = Evidence

Proof before install:
https://morning-verlu.github.io/KAI/proof-pack.html

The Proof Pack links a checked-in run, process table, syscall ledger, replayable capsule, and baseline gate.

The Kotlin/JVM library path is here:
https://github.com/morning-verlu/KAI/blob/main/docs/KOTLIN_API.md

And the no-key tour is:

curl -fsSL https://morning-verlu.github.io/KAI/install.sh | sh
export PATH="$HOME/.kaios/bin:$PATH"
kaios tour

I would especially like feedback on the Kotlin API shape, the process/scheduler/tool model, and whether local evidence capsules are useful for JVM backend teams.
```

## Post 3: X / LinkedIn Short

Use this after Show HN or Kotlin/JVM feedback clarifies the strongest objection.

```text
Most AI agents fail like scripts. KAI OS treats them like processes.

I am building a local-first Evidence OS for AI agents in Kotlin:

Agent = Process
Workflow = Scheduler
Tool = Syscall
Run = Evidence

Proof before install:
https://morning-verlu.github.io/KAI/proof-pack.html

Repo:
https://github.com/morning-verlu/KAI
```

Attach when the platform supports images:

```text
https://morning-verlu.github.io/KAI/assets/kaios-evidence-proof.png
```

## First Reply Template

Use this when someone asks why KAI OS is different:

```text
The difference is the evidence layer.

KAI OS records process traces, replayable capsules, syscall audit records, recovery evidence, and baseline diffs so agent runs can be inspected or gated later without relying on provider logs.

No-install viewer:
https://morning-verlu.github.io/KAI/evidence-viewer.html

Trust matrix:
https://github.com/morning-verlu/KAI/blob/main/docs/TRUST_MATRIX.md
```

## Metrics To Capture

Capture these before posting, then again 2 hours, 24 hours, and 72 hours after posting:

```bash
gh repo view morning-verlu/KAI --json stargazerCount,forkCount,watchers,usesCustomOpenGraphImage
```

Also note:

- post URLs.
- top questions or objections.
- whether people clicked the Evidence Viewer or asked for screenshots.
- whether Kotlin/JVM feedback is about API shape, runtime model, or install friction.
- any issue or PR opened by an external contributor.

## Current Baseline

As of the first-wave prep pass:

```text
stars: 0
forks: 2
watchers: 0
social preview: uploaded
```

## Posted Links

- X short post: https://x.com/wurslu/status/2066846887983096042 (posted 2026-06-16)

## 2-Hour Readout

Roughly two hours after the first X post:

```text
GitHub stars: 0
GitHub forks: 2
GitHub watchers: 0
GitHub views: 0
X impressions: 1
X engagements: 0
X link clicks: 0
X public replies/reposts/likes/bookmarks: 0/0/0/0
```

Conclusion: the first text-first X post did not receive distribution. The next external wave should lead with visual proof and community surfaces rather than another concept-only text post.
