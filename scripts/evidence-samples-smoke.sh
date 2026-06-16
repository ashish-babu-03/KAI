#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KAIOS_BIN="${KAIOS_BIN:-$ROOT/build/install/kaios-cli/bin/kaios}"

if [[ ! -x "$KAIOS_BIN" ]]; then
  echo "kaios evidence samples smoke: building local installDist first" >&2
  (cd "$ROOT" && ./gradlew installDist --no-daemon)
fi

WORKDIR="$(mktemp -d "${TMPDIR:-/tmp}/kaios-evidence-samples.XXXXXX")"
if [[ -z "${KAIOS_KEEP_SMOKE:-}" ]]; then
  trap 'rm -rf "$WORKDIR"' EXIT
fi

run_step() {
  local label="$1"
  shift
  echo "==> $label" >&2
  if [[ "$#" -gt 0 ]]; then
    "$@"
  fi
}

assert_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "Expected file '$path' to exist." >&2
    exit 1
  fi
}

assert_contains() {
  local path="$1"
  local expected="$2"
  if ! grep -Fq -- "$expected" "$path"; then
    echo "Expected '$path' to contain: $expected" >&2
    exit 1
  fi
}

assert_exit_code() {
  local actual="$1"
  local expected="$2"
  local command="$3"
  if [[ "$actual" != "$expected" ]]; then
    echo "Expected '$command' to exit $expected, got $actual." >&2
    exit 1
  fi
}

normalize_diff_json() {
  local raw="$1"
  local out="$2"
  python3 - "$raw" "$out" <<'PY'
import json
import sys
from pathlib import Path

source = Path(sys.argv[1])
target = Path(sys.argv[2])
data = json.loads(source.read_text())
stable = {
    "schema": data["schema"],
    "result": data["result"],
    "valid": data["valid"],
    "same": data["same"],
    "checks": data["checks"],
    "metricsDelta": data["metricsDelta"],
    "fields": [difference["field"] for difference in data["differences"]],
    "differences": data["differences"],
}
target.write_text(json.dumps(stable, indent=2) + "\n")
PY
}

EVIDENCE_SAMPLE="$ROOT/examples/evidence-sample/change-review.capsule.json"
BASELINE="$ROOT/examples/baseline-gate/capsules/baseline.capsule.json"
CURRENT="$ROOT/examples/baseline-gate/capsules/current-different.capsule.json"
EXPECTED_DIFF="$ROOT/examples/baseline-gate/expected/diff.stable.json"

assert_file "$EVIDENCE_SAMPLE"
assert_file "$BASELINE"
assert_file "$CURRENT"
assert_file "$EXPECTED_DIFF"

run_step "evidence sample capsule contract" "$KAIOS_BIN" capsule --file "$EVIDENCE_SAMPLE" --check > "$WORKDIR/evidence-sample-capsule.out"
assert_contains "$WORKDIR/evidence-sample-capsule.out" "schema: kaios.run-capsule/v1"
assert_contains "$WORKDIR/evidence-sample-capsule.out" "status: valid"

run_step "evidence sample offline replay" "$KAIOS_BIN" replay --file "$EVIDENCE_SAMPLE" > "$WORKDIR/evidence-sample-replay.out"
assert_contains "$WORKDIR/evidence-sample-replay.out" "schema: kaios.run-replay/v1"
assert_contains "$WORKDIR/evidence-sample-replay.out" "status: valid"
assert_contains "$WORKDIR/evidence-sample-replay.out" "deterministic: true"

run_step "baseline capsule contract" "$KAIOS_BIN" capsule --file "$BASELINE" --check > "$WORKDIR/baseline-capsule.out"
assert_contains "$WORKDIR/baseline-capsule.out" "status: valid"

run_step "current capsule contract" "$KAIOS_BIN" capsule --file "$CURRENT" --check > "$WORKDIR/current-capsule.out"
assert_contains "$WORKDIR/current-capsule.out" "status: valid"

run_step "baseline offline replay" "$KAIOS_BIN" replay --file "$BASELINE" > "$WORKDIR/baseline-replay.out"
assert_contains "$WORKDIR/baseline-replay.out" "status: valid"
assert_contains "$WORKDIR/baseline-replay.out" "deterministic: true"

run_step "current offline replay" "$KAIOS_BIN" replay --file "$CURRENT" > "$WORKDIR/current-replay.out"
assert_contains "$WORKDIR/current-replay.out" "status: valid"
assert_contains "$WORKDIR/current-replay.out" "deterministic: true"

run_step "baseline gate diff json" "$KAIOS_BIN" diff "$BASELINE" "$CURRENT" --json > "$WORKDIR/diff.raw.json"
normalize_diff_json "$WORKDIR/diff.raw.json" "$WORKDIR/diff.stable.json"
diff -u "$EXPECTED_DIFF" "$WORKDIR/diff.stable.json" > "$WORKDIR/diff-stable-check.out"

run_step "baseline gate diff --check exits 1"
set +e
"$KAIOS_BIN" diff "$BASELINE" "$CURRENT" --check > "$WORKDIR/diff-check.out" 2>&1
DIFF_CHECK_EXIT="$?"
set -e
assert_exit_code "$DIFF_CHECK_EXIT" "1" "kaios diff baseline current --check"
assert_contains "$WORKDIR/diff-check.out" "schema: kaios.run-diff/v1"
assert_contains "$WORKDIR/diff-check.out" "status: different"
assert_contains "$WORKDIR/diff-check.out" "same: false"
assert_contains "$WORKDIR/diff-check.out" "metrics_delta:"
assert_contains "$WORKDIR/diff-check.out" "differences:"

echo "kaios evidence samples smoke ok"
if [[ -n "${KAIOS_KEEP_SMOKE:-}" ]]; then
  echo "workspace: $WORKDIR"
fi
