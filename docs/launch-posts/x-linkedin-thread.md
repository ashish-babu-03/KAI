# X / LinkedIn Technical Thread

Status: draft, not posted.

Attach this image to the first post when supported:

```text
https://morning-verlu.github.io/KAI/assets/kaios-evidence-proof.png
```

Thread:

```text
1/ Most AI agents fail like scripts.

KAI OS treats them like processes.

I am building a Kotlin/JVM Evidence OS for AI agent runs: process traces, syscall ledgers, replayable capsules, and CI-grade proof.

2/ The model:

KAI OS  = Evidence OS for AI agents
Agent   = Process
Workflow = Scheduler
Tool    = Syscall
Run     = Evidence

3/ Why this matters:

When an agent reviews code or touches tools, a maintainer should be able to ask:

- what ran?
- which tools were called?
- what was denied?
- can I replay this offline?
- did runtime behavior drift from baseline?

4/ No API key is required for the first run:

curl -fsSL https://morning-verlu.github.io/KAI/install.sh | sh
export PATH="$HOME/.kaios/bin:$PATH"
kaios tour

5/ `kaios tour` creates a disposable Git repo, runs a deterministic review, and writes:

- Markdown review artifact
- process trace JSON
- replayable capsule
- process table
- evidence summary
- recovery dry-run report

6/ The product is not the installer.

Homebrew / curl / ZIP are just delivery paths.

The product is the local evidence layer around agent work.

7/ Proof before install:

https://morning-verlu.github.io/KAI/proof-pack.html

Repo:
https://github.com/morning-verlu/KAI

I would love feedback from Kotlin/JVM developers, OSS maintainers, and agent-infra builders.
```

Short one-post version:

```text
Most AI agents fail like scripts. KAI OS treats them like processes.

Agent = Process
Workflow = Scheduler
Tool = Syscall
Run = Evidence

It is a local-first Kotlin/JVM Evidence OS for agent runs: process traces, syscall ledgers, replayable capsules, and CI gates.

Proof: https://morning-verlu.github.io/KAI/proof-pack.html
Repo: https://github.com/morning-verlu/KAI
```
