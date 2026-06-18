# Show HN Post

Status: draft, not posted.

Submit URL:

```text
https://github.com/morning-verlu/KAI
```

Title:

```text
Show HN: KAI OS - an Evidence OS for Kotlin AI agents
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

The tour creates a disposable Git repo, runs a deterministic current-change review, and writes a Markdown review artifact, process trace, replayable capsule, process table, evidence summary, and recovery dry-run report.

I would especially like feedback from Kotlin/JVM developers, OSS maintainers, and people building agent infrastructure: would portable run evidence help you trust or debug agent-driven review/CI work?
```

First comment:

```text
For anyone who wants proof before installing:

Proof Pack:
https://morning-verlu.github.io/KAI/proof-pack.html

Evidence Viewer:
https://morning-verlu.github.io/KAI/evidence-viewer.html

Checked-in sample artifacts:
https://github.com/morning-verlu/KAI/tree/main/examples/evidence-sample

The product boundary is deliberately narrow: KAI OS does not prove that an agent answer is correct. It proves what processes ran, which tools were called or denied, what can replay offline, and whether stable runtime behavior changed from a baseline.
```

After posting, record this in issue #7:

```text
Show HN published:

- URL:
- Published at:
- Baseline stars/forks/watchers:
- Baseline GitHub views:
- Next check: +2h
```

Then run:

```bash
./scripts/launch-metrics.sh
```
