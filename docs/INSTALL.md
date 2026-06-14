# Install KAI OS

KAI OS is distributed as a JVM CLI. Java 17+ is required.

## Homebrew

```bash
brew tap morning-verlu/tap
brew install kaios

kaios doctor
kaios analyze . --out artifacts/analysis.md
kaios run --index . --context README.md --out artifacts/project.md "summarize this project"
```

If your project has no `README.md`, omit `--context README.md`.

## Hosted Installer

The installer downloads the release ZIP, verifies the published SHA256 checksum, and links `kaios` under `$HOME/.kaios/bin`.

```bash
curl -fsSL https://morning-verlu.github.io/KAI/install.sh | sh
export PATH="$HOME/.kaios/bin:$PATH"
kaios doctor
kaios analyze . --out artifacts/analysis.md
kaios run --index . --context README.md --out artifacts/project.md "summarize this project"
```

Set `KAIOS_INSTALL_DIR` to install somewhere else:

```bash
curl -fsSL https://morning-verlu.github.io/KAI/install.sh | KAIOS_INSTALL_DIR="$HOME/.local/kaios" sh
```

## Download ZIP

```bash
curl -L -o kaios-0.1.17.zip https://github.com/morning-verlu/KAI/releases/download/v0.1.17/kaios-0.1.17.zip
unzip kaios-0.1.17.zip
./kaios-0.1.17/bin/kaios doctor
./kaios-0.1.17/bin/kaios analyze . --out artifacts/analysis.md
./kaios-0.1.17/bin/kaios run --index . --context README.md --out artifacts/project.md "summarize this project"
```

## Build From Source

```bash
git clone https://github.com/morning-verlu/KAI.git
cd KAI
./gradlew test installDist
build/install/kaios-cli/bin/kaios doctor
build/install/kaios-cli/bin/kaios analyze . --out artifacts/analysis.md
build/install/kaios-cli/bin/kaios run --index . --context README.md --out artifacts/project.md "summarize this project"
```

Useful next commands after the first artifact:

```bash
kaios analyze . --format json --out artifacts/analysis.json
kaios init --template research
kaios config show
kaios ps <run-id>
kaios inspect <run-id>
```
