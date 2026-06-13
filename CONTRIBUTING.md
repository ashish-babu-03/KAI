# Contributing

Thanks for helping build KAI OS.

This project is early, so the most valuable contributions are clear runtime ideas, focused implementation PRs, tests, and examples that make the Agent-as-Process model sharper.

## Local Setup

```bash
./gradlew clean test installDist
build/install/kaios-cli/bin/kaios run "test local setup"
```

You need Java 17+. The Gradle wrapper is included.

## Good First Areas

- model providers
- tool permissions
- scheduler edge cases
- memory adapters
- CLI output polish
- examples and docs

## Pull Request Expectations

- Keep changes focused.
- Add tests for runtime behavior.
- Run `./gradlew test installDist`.
- Update docs when public behavior changes.

## Design Principles

- Agents are processes, not chat sessions.
- Tools are syscalls, not arbitrary callbacks.
- Workflows are schedulers, not chains.
- Observability is a core feature, not an afterthought.
