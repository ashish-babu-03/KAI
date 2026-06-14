# Project Setup

Use `kaios setup` when you want the shortest safe path from install to a validated project workflow:

```bash
kaios setup --ci
```

This command:

- creates `kaios.json` from the `research` template when it is missing.
- keeps existing config files unless `--force` is passed.
- validates the workflow with `kaios.config-validation/v1`.
- runs readiness checks and warns about optional real-provider or persisted-memory env problems.
- writes `.github/workflows/kaios.yml` when `--ci` is passed and the project config is valid.
- points the generated Agent Gate at `kaios verify --config kaios.json --evidence --force`.
- prints the next useful commands.

## Common Paths

Create a local workflow only:

```bash
kaios setup
```

Create a workflow plus the GitHub Actions Agent Gate:

```bash
kaios setup --ci
git add kaios.json .github/workflows/kaios.yml
```

For a production-style GitHub Actions example that also stores JSON verification output and a failure-time support report, see [../examples/github-actions-agent-gate.yml](../examples/github-actions-agent-gate.yml).

Use a different template:

```bash
kaios setup --template code-review --ci
```

Repair an invalid or outdated generated config:

```bash
kaios setup --ci --force
```

If an existing config is invalid, `kaios setup --ci` reports `ci: skipped` and prints a `config validate` command plus the exact force command to repair the generated config. It will not write a CI gate for a workflow that cannot pass validation.

Use JSON for automation:

```bash
kaios setup --json
```

JSON output uses schema `kaios.setup/v1`.

## After Setup

Run the readiness gate:

```bash
kaios verify --evidence --force
kaios ps latest
kaios trace latest --check
```

`kaios verify --evidence --force` checks the local runtime, validates the project workflow, runs a deterministic mock smoke workflow, validates the process trace contract, saves a normal run snapshot, and confirms the same smoke run can become a portable audit package. Use `--evidence-out artifacts/run.capsule.json` when you need a custom capsule path, or add `--baseline ... --check` when CI should compare against a known-good capsule.

Create a project artifact when the gate is ready:

```bash
kaios run --index . --out artifacts/project.md --trace-out artifacts/trace.json --force "summarize this project"
```

If something behaves differently across machines:

```bash
kaios bug-report --out artifacts/kaios-bug-report.md --force
```
