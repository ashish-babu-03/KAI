#!/usr/bin/env bash
set -euo pipefail

TASK="${1:-analyze crypto market}"

./gradlew test installDist

RUN_OUTPUT="$(build/install/kaios-cli/bin/kaios run "$TASK")"
echo "$RUN_OUTPUT"

RUN_ID="$(printf '%s\n' "$RUN_OUTPUT" | awk '/^run_id:/ { print $2 }')"

if [[ -z "$RUN_ID" ]]; then
  echo "Could not find run_id in CLI output." >&2
  exit 1
fi

echo
echo "== process table =="
build/install/kaios-cli/bin/kaios ps "$RUN_ID"

echo
echo "== lifecycle events =="
build/install/kaios-cli/bin/kaios inspect "$RUN_ID"
