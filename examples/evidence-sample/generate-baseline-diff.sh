#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
KAIOS_BIN="${KAIOS_BIN:-$ROOT/build/install/kaios-cli/bin/kaios}"
OUT_DIR="${1:-$ROOT/examples/evidence-sample/generated}"
WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/kaios-baseline.XXXXXX")"

cleanup() {
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_cmd git

if [[ ! -x "$KAIOS_BIN" ]]; then
  echo "Expected an executable kaios CLI at: $KAIOS_BIN" >&2
  echo "Build it first with ./gradlew installDist or set KAIOS_BIN explicitly." >&2
  exit 1
fi

mkdir -p "$OUT_DIR"

write_project_files() {
  local mode="$1"
  mkdir -p "$WORK_DIR/repo/src/main/kotlin"
  cat >"$WORK_DIR/repo/README.md" <<'EOF'
# Evidence Sample App

Tiny Kotlin repo used to generate KAI OS review evidence.
EOF
  cat >"$WORK_DIR/repo/src/main/kotlin/App.kt" <<EOF
fun main() {
    println("Hello from the ${mode} review sample")
}
EOF
}

capture_review() {
  local name="$1"

  rm -rf "$WORK_DIR/runtime"
  mkdir -p "$WORK_DIR/runtime"

  (
    cd "$WORK_DIR/repo"
    export KAIOS_RUNS_DIR="$WORK_DIR/runtime/runs"
    export KAIOS_REPORTS_DIR="$WORK_DIR/runtime/reports"
    export KAIOS_ARTIFACTS_DIR="$WORK_DIR/runtime/artifacts"
    export KAIOS_CAPSULES_DIR="$WORK_DIR/runtime/capsules"

    "$KAIOS_BIN" review --json >"$OUT_DIR/${name}-review-result.json"
    "$KAIOS_BIN" capsule --file artifacts/change-review.capsule.json --check >"$OUT_DIR/${name}-capsule-check.txt"
    "$KAIOS_BIN" replay --file artifacts/change-review.capsule.json >"$OUT_DIR/${name}-replay.txt"

    cp artifacts/change-review.md "$OUT_DIR/${name}-change-review.md"
    cp artifacts/change-review.trace.json "$OUT_DIR/${name}-change-review.trace.json"
    cp artifacts/change-review.capsule.json "$OUT_DIR/${name}.capsule.json"
  )
}

mkdir -p "$WORK_DIR/repo"
cd "$WORK_DIR/repo"
git init -q
git config user.name "Codex Automation"
git config user.email "190979964+ded-furby@users.noreply.github.com"

write_project_files "base"
git add README.md src/main/kotlin/App.kt
git commit -qm "chore: seed evidence sample app"

write_project_files "baseline"
capture_review "baseline"
git checkout -- README.md src/main/kotlin/App.kt

write_project_files "current"
cat >>"$WORK_DIR/repo/README.md" <<'EOF'

This variant adds a short note so the review capsule differs from the baseline.
EOF
capture_review "current"

set +e
"$KAIOS_BIN" diff "$OUT_DIR/baseline.capsule.json" "$OUT_DIR/current.capsule.json" --check >"$OUT_DIR/baseline-current.diff.txt" 2>&1
diff_exit=$?
set -e

cat >"$OUT_DIR/README.txt" <<EOF
Generated baseline/current review evidence.

baseline capsule: $OUT_DIR/baseline.capsule.json
current capsule:  $OUT_DIR/current.capsule.json
diff output:      $OUT_DIR/baseline-current.diff.txt
diff exit code:   $diff_exit

Exit code 1 is expected when stable runtime behavior changes and --check is used.
EOF

echo "Generated evidence sample in $OUT_DIR"
