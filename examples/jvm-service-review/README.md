# JVM Service Review Example

This example shows the `kaios review` path on a tiny Kotlin/JVM service change.

Use it when you want to understand the difference between:

- [examples/evidence-sample](../evidence-sample/): checked-in output artifacts you can inspect without running anything.
- this example: a runnable mini-project that creates a dirty Git change and asks KAI OS to review it.

The flow uses the deterministic mock provider, so no API key is required.

## What The Change Does

The sample service classifies invoice risk for a billing backend. The review patch adds an active payment-plan field and changes the past-due signal so invoices on a payment plan are not flagged only for age.

That is intentionally small but realistic: it touches a domain model, business logic, and a unit test.

## Run It

From the KAI OS repository:

```bash
./gradlew installDist

KAIOS_BIN="$PWD/build/install/kaios-cli/bin/kaios"
WORKDIR="$(mktemp -d)"

cp -R examples/jvm-service-review/service/. "$WORKDIR/"
cp examples/jvm-service-review/review-change.patch "$WORKDIR/"

cd "$WORKDIR"
git init
git add .
git -c user.name="KAIOS Example" -c user.email=kaios@example.invalid commit -m "baseline billing service"

"$KAIOS_BIN" quickstart --no-ci

git apply --check review-change.patch
git apply review-change.patch
git status --short

mkdir -p artifacts
"$KAIOS_BIN" review --json | tee artifacts/review-result.json
"$KAIOS_BIN" ps
"$KAIOS_BIN" evidence --summary
```

If you installed KAI OS from the release ZIP, hosted installer, Homebrew, or Codespaces, replace `"$KAIOS_BIN"` with `kaios`.

## Expected Outputs

After `kaios review`, inspect:

- `artifacts/change-review.md`: Markdown review artifact.
- `artifacts/change-review.trace.json`: `kaios.process-trace/v1` process trace.
- `artifacts/change-review.capsule.json`: replayable `kaios.run-capsule/v1` evidence capsule.
- `artifacts/review-result.json`: `kaios.review/v1` CLI/CI contract from `kaios review --json`.

The example `.gitignore` excludes `.kaios/`, `artifacts/`, `build/`, and `.gradle/` so generated evidence does not pollute the Git change set being reviewed.
The example `.kaiosignore` excludes the same generated paths plus common secret patterns so agent context stays focused on source changes.

Do not apply `review-change.patch` in the KAI OS repository root. Copy `service/` into a temporary directory or another standalone Git repository first.

## Try A Baseline Gate

Once you have one review capsule, save it as a baseline:

```bash
mkdir -p baselines
cp artifacts/change-review.capsule.json baselines/billing-review.capsule.json
```

Then edit the service again and run:

```bash
kaios review --baseline baselines/billing-review.capsule.json --check
```

That turns the current code change into a repeatable evidence comparison: KAI OS validates the new capsule, replays saved evidence offline, and fails when stable runtime behavior differs from the baseline.
