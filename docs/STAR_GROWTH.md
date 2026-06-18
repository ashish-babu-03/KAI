# 1000 Stars Operating Plan

This is the KAI OS growth plan for real developer interest. It is not a plan for paid stars, bot engagement, or artificial popularity.

## Current Diagnosis

KAI OS already has a checkable product surface:

- Proof Pack
- Evidence Viewer
- `kaios tour`
- checked-in evidence samples
- Contributor Board

The current bottleneck is the loop from external reach to GitHub understanding:

```text
channel reach -> link click -> GitHub view -> star/fork/watch -> issue/discussion/PR
```

If GitHub views stay near zero, distribution failed. If views rise but stars stay flat, the repository first screen or star rationale failed. If stars rise but issues stay flat, the contribution path failed.

## Conversion Surface

Every public entry point should explain KAI OS in one screen:

```text
KAI OS  = Evidence OS for AI agents
Agent   = Process
Workflow = Scheduler
Tool    = Syscall
Run     = Evidence
```

Primary actions:

| Action | Link or command | Purpose |
| --- | --- | --- |
| See proof | `https://morning-verlu.github.io/KAI/proof-pack.html` | Convert skeptical developers before install |
| Try locally | `kaios tour` | Prove no-key local value |
| Use in CI | `kaios evidence --baseline ... --check` | Show why this is infrastructure |

Secondary actions:

- [Why Star KAI OS](WHY_STAR.md)
- [Kotlin/JVM Evaluation Path](KOTLIN_JVM_EVALUATION.md)
- [Contributor Board](CONTRIBUTOR_BOARD.md)

## Channel Ladder

Post in sequence, not everywhere at once.

| Order | Channel | Primary link | Success signal |
| --- | --- | --- | --- |
| 1 | Show HN | GitHub repo | GitHub views from `news.ycombinator.com`, technical comments |
| 2 | Kotlin/JVM community | Proof Pack or Kotlin/JVM evaluation path | Kotlin API feedback, discussion replies |
| 3 | X technical thread | Evidence image + repo | profile reach, repo views, quote replies |
| 4 | Dev.to / Medium | technical article | sustained referrers after 24h |
| 5 | Newsletter submissions | Proof Pack + repo | delayed referrers and stars |

Default Show HN title:

```text
Show HN: KAI OS - an Evidence OS for Kotlin AI agents
```

Default X hook:

```text
Most AI agents fail like scripts. KAI OS treats them like processes.
```

## Metric Loop

After every post, capture:

```bash
gh repo view morning-verlu/KAI --json stargazerCount,forkCount,watchers
gh api repos/morning-verlu/KAI/traffic/views
gh api repos/morning-verlu/KAI/traffic/popular/referrers
```

Record the result in issue #7 or the active launch issue:

```text
Post readout:

- Channel:
- URL:
- Published at:
- Stars/forks/watchers:
- GitHub views:
- Top referrers:
- Main question or objection:
- Next decision:
```

Decision rules:

| Signal | Decision |
| --- | --- |
| GitHub views stay at 0 | Switch channel before editing product copy |
| Views rise but stars stay flat | Improve README first screen, Proof Pack, or Why Star |
| Stars rise but no issues/discussions | Improve Contributor Board and starter issues |
| Comments ask "what is this?" | Reply with the Evidence OS model and Proof Pack |
| Comments ask "why Kotlin?" | Reply with JVM CI/runtime-boundary use cases |

## Trust Gaps To Close

These are launch conversion blockers:

- Public GitHub Actions CI: tracked in issue #12 and [CI_ENABLE_RUNBOOK.md](CI_ENABLE_RUNBOOK.md).
- Full Docker smoke PASS: tracked in issue #16.
- More visible contributor paths: keep 5-8 high-quality `good first issue` items live.
- Fast maintainer replies: respond to external contributor comments within 12 hours and PRs within 24 hours when possible.

## Milestones

| Milestone | Meaning |
| --- | --- |
| 10 stars | README + Proof Pack can convert cold visitors |
| 50 stars | At least one external channel produces sustained GitHub views |
| 100 stars | External issues, PRs, or discussions begin recurring |
| 300 stars | One channel creates second-order sharing |
| 1000 stars | KAI OS has become visible as a Kotlin/JVM agent infrastructure idea |

The project should earn these with useful proof, clear positioning, and real community interaction.
