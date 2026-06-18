# Technical Article Draft

Status: draft, not posted.

Use this after Show HN or Kotlin/JVM feedback reveals which objections are most common. The article should feel like an engineering note, not a launch ad.

## Working Titles

```text
Agent = Process: Building an Evidence OS for AI Agents in Kotlin
```

```text
Most AI Agents Fail Like Scripts. KAI OS Treats Them Like Processes.
```

## Target Outlets

- DEV.to
- Medium
- personal blog
- Kotlin/JVM community blog post

## Article Draft

Most AI agent demos end with a text answer. That is fine for a prototype, but it is thin evidence for a maintainer.

If an agent reviews code, calls tools, touches files, or becomes part of CI, the useful questions are operational:

- what ran?
- which tools were requested?
- which calls were allowed or denied?
- what failed or recovered?
- can another developer replay the run offline?
- can CI detect stable runtime behavior drift?

That is the idea behind KAI OS.

```text
KAI OS  = Evidence OS for AI agents
Agent   = Process
Workflow = Scheduler
Tool    = Syscall
Run     = Evidence
```

The project is not trying to be another chatbot framework or a Kotlin LangChain clone. KAI OS is lower-level: a local-first runtime evidence layer for agent work in Kotlin/JVM.

## Agent = Process

In KAI OS, an agent is not just a prompt wrapper. It is represented as a process-like runtime unit with PID, lifecycle state, memory metrics, token usage, syscall counts, timing, failure metadata, and recovery evidence.

That makes a run inspectable after the answer is gone. A reviewer can look at process rows and lifecycle events instead of trusting a black-box transcript.

## Workflow = Scheduler

Agent work is often described as a chain, but production work usually needs scheduling semantics: DAG execution, priority, retries, fallback, cancellation, and recovery.

KAI OS records scheduler evidence so a run can explain not only what answer appeared, but which nodes ran, in which order, and what happened when a node failed.

## Tool = Syscall

Tool access is where agent trust usually gets blurry. KAI OS treats tools as syscall-style boundaries:

- tools are registered capabilities.
- calls require grants.
- denied calls are recorded.
- arguments can be redacted.
- duration and cost can be tracked.

The important part is that denied calls also become evidence. A safe runtime should show what the agent tried to do, not only what succeeded.

## Run = Evidence

The output of a KAI OS run is not just text. A run can produce:

- a Markdown review artifact.
- a process trace JSON file.
- a syscall ledger.
- a replayable capsule.
- an evidence summary.
- a baseline diff for CI gates.

The default proof path uses a deterministic mock provider, so the first run does not require an API key. That is intentional: the evidence model should be inspectable before anyone trusts a real provider integration.

## Try The Proof

No install:

```text
https://morning-verlu.github.io/KAI/proof-pack.html
```

Local no-key tour:

```bash
curl -fsSL https://morning-verlu.github.io/KAI/install.sh | sh
export PATH="$HOME/.kaios/bin:$PATH"
kaios tour
```

Repository:

```text
https://github.com/morning-verlu/KAI
```

## What KAI OS Does Not Claim

KAI OS does not prove that an agent answer is correct. It proves a narrower infrastructure claim: what ran, what tools were requested, what was allowed or denied, what can replay offline, and whether stable runtime behavior drifted from a baseline.

That boundary matters. It keeps the project focused on portable runtime evidence instead of vague agent intelligence claims.

## Feedback Wanted

The questions I care about most:

- Does the `Agent = Process` model feel useful for JVM teams?
- Would replayable run capsules help maintainers review agent output?
- Should CI gate agent runtime behavior before real provider integration?
- Is the Kotlin API shape idiomatic enough to build on?

Focused Kotlin/JVM feedback:

```text
https://github.com/morning-verlu/KAI/discussions/17
```

## Publishing Checklist

Before posting:

```bash
./scripts/launch-metrics.sh
```

After posting, record:

```text
Article published:

- Outlet:
- URL:
- Published at:
- Baseline stars/forks/watchers:
- Baseline GitHub views:
- Next check: +24h
```
