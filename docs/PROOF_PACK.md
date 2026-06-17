# KAI OS Proof Pack

Use this page when you need the shortest proof that KAI OS is a product surface, not just a slogan.

KAI OS is early, but the core claim is already checkable:

```text
Agent    = Process
Workflow = Scheduler
Tool     = Syscall
Run      = Evidence
```

The proof pack is intentionally local-first. You can inspect checked-in artifacts, run deterministic smoke checks, and replay capsules without an API key or a hosted agent service.

## The Five Proofs

| Claim | Proof today | Where to check |
| --- | --- | --- |
| No API key is required for the first run | deterministic mock provider and `kaios tour` | `kaios tour` |
| Agents are inspectable as processes | process rows with PID, state, tokens, context, syscalls, worker id, and lifecycle events | `examples/evidence-sample/change-review.trace.json` |
| Tools are syscall-bounded | syscall ledger records tool, permission, allowed/denied status, redacted args, duration, and cost | `examples/evidence-sample/change-review.trace.json` |
| Runs are portable | run capsule embeds snapshot, trace, provenance hashes, and replay commands | `examples/evidence-sample/change-review.capsule.json` |
| CI can gate runtime drift | baseline/current capsules produce a stable diff and `--check` exits nonzero on changed behavior | `examples/baseline-gate/expected/diff.stable.json` |

## Fastest No-Install Path

Open the visual proof first:

```text
https://morning-verlu.github.io/KAI/evidence-viewer.html
```

It shows one checked-in run as:

- process table
- syscall ledger
- replayable capsule
- offline replay status
- baseline gate result

## Fastest Local Path

```bash
curl -fsSL https://morning-verlu.github.io/KAI/install.sh | sh
export PATH="$HOME/.kaios/bin:$PATH"
kaios tour
```

`kaios tour` creates a disposable Git workspace, runs the no-key Evidence OS loop, and prints artifact paths for review, trace, capsule, evidence summary, and recovery dry-run output.

## Fastest Source-Checkout Verification

```bash
./scripts/evidence-samples-smoke.sh
```

This validates:

- checked-in Evidence Sample capsule contract
- offline replay for the Evidence Sample
- baseline/current capsule contracts
- offline replay for baseline/current capsules
- normalized stable `kaios.run-diff/v1` output
- `kaios diff --check` exiting `1` for valid changed runtime behavior

For the full repository trust path:

```bash
./scripts/repository-ci-smoke.sh
```

That also builds and tests the project, installs the CLI, runs the Kotlin runtime API example, runs `kaios tour`, validates the generated tour capsule, and replays it offline.

## Checked-In Artifacts

| Artifact | Why it matters |
| --- | --- |
| `examples/evidence-sample/change-review.md` | human-readable review artifact from `kaios review` |
| `examples/evidence-sample/change-review.trace.json` | process trace with processes, scheduler, syscalls, cost, and events |
| `examples/evidence-sample/change-review.capsule.json` | portable replay capsule |
| `examples/evidence-sample/review-result.json` | stable `kaios.review/v1` CLI/CI contract |
| `examples/baseline-gate/capsules/baseline.capsule.json` | known-good runtime behavior |
| `examples/baseline-gate/capsules/current-different.capsule.json` | changed runtime behavior |
| `examples/baseline-gate/expected/diff.stable.json` | normalized baseline-gate proof |
| `examples/kotlin-runtime-api` | embeddable Kotlin/JVM runtime API example |

## What This Proves

KAI OS does not prove that an agent answer is correct. It proves something narrower and more useful for infrastructure:

- what processes ran
- which tools were requested
- which syscalls were allowed or denied
- what runtime metrics changed
- whether a run capsule can replay offline
- whether stable runtime behavior drifted from a baseline

That is the product boundary: portable agent-run evidence for developers, reviewers, maintainers, and CI.

## Honest Gaps

- Public GitHub Actions CI is not committed yet because the current token lacks the `workflow` scope. A copyable workflow template exists at `examples/github-actions-repository-ci.yml`.
- Docker smoke is documented, but full image pulls have been slow in the current local network. The lightweight preflight path is `./scripts/docker-smoke.sh --preflight`.
- Real model providers are optional. The first-run proof path stays deterministic and local by default.

## Next Step

If this evidence model is useful, star or watch the repository and run the no-key tour:

```text
https://github.com/morning-verlu/KAI
```

If you want to challenge the proof, start with the checked-in artifacts above or the focused Kotlin/JVM feedback discussion:

```text
https://github.com/morning-verlu/KAI/discussions/17
```
