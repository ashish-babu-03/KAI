# Install KAI OS

KAI OS is distributed as a JVM CLI. Java 17+ is required.

## Homebrew

```bash
brew tap morning-verlu/tap
brew install kaios
kaios doctor
kaios run "analyze crypto market"
```

## Hosted Installer

The installer downloads the release ZIP, verifies the published SHA256 checksum, and links `kaios` under `$HOME/.kaios/bin`.

```bash
curl -fsSL https://morning-verlu.github.io/KAI/install.sh | sh
export PATH="$HOME/.kaios/bin:$PATH"
kaios doctor
kaios run "analyze crypto market"
```

Set `KAIOS_INSTALL_DIR` to install somewhere else:

```bash
curl -fsSL https://morning-verlu.github.io/KAI/install.sh | KAIOS_INSTALL_DIR="$HOME/.local/kaios" sh
```

## Download ZIP

```bash
curl -L -o kaios-0.1.6.zip https://github.com/morning-verlu/KAI/releases/download/v0.1.6/kaios-0.1.6.zip
unzip kaios-0.1.6.zip
./kaios-0.1.6/bin/kaios doctor
./kaios-0.1.6/bin/kaios run "analyze crypto market"
```

## Build From Source

```bash
git clone https://github.com/morning-verlu/KAI.git
cd KAI
./gradlew test installDist
build/install/kaios-cli/bin/kaios doctor
build/install/kaios-cli/bin/kaios run "analyze crypto market"
```
