# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This repository is a collection of Freenet plugins organized as Git submodules. Each plugin in the `projects/` directory is a separate Freenet plugin that extends the functionality of the Freenet peer-to-peer network. All plugins are related to the Hyphanet (formerly Freenet) project and focus on providing decentralized, anonymous, and censorship-resistant communication tools.

## Build System

This repository includes a comprehensive Gradle-based build system that can compile all plugins non-invasively:

### Quick Start
```bash
./gradlew buildAll          # Build all plugins (default)
./gradlew listPlugins       # List all discovered plugins
./gradlew diagnoseBuildIssues  # Diagnose build problems
```

### Architecture
- **Non-invasive**: Zero modifications to plugin source code
- **Automatic dependency resolution**: Downloads external JARs from Maven Central
- **Multi-build support**: Handles both Gradle and Ant-based plugins
- **Fred integration**: Includes Freenet core as submodule for dependencies

### Available Tasks
- `./gradlew buildAll` - Build all plugins and collect JARs (default)
- `./gradlew buildGradleOnly` - Build only Gradle plugins
- `./gradlew buildFred` - Build Fred (Freenet core dependencies)
- `./gradlew downloadDependencies` - Download missing external dependencies
- `./gradlew fixAllBuildIssues` - Apply build fixes (non-invasively)
- `./gradlew diagnoseBuildIssues` - Show comprehensive build diagnosis
- `./gradlew listPlugins` - List all plugins by type
- `./gradlew cleanAll` - Clean all plugin builds

### Build Output
- All built JARs are collected in `./build/libs/` with plugin-specific names
- Build artifacts are isolated and don't affect git status
- Successfully builds 9/22 plugins (41% success rate)

### Dependencies
The build system automatically handles:
- **Fred (Freenet core)**: Built from submodule in `projects/fred/`
- **External JARs**: SnakeYAML, XOM, BouncyCastle downloaded from Maven Central
- **Plugin dependencies**: Proper classpath setup and symlink creation

### Git Configuration
- All submodules configured with `ignore = all` to ignore build artifacts
- Source code integrity preserved - no tracked files are modified
- Build directories (.gradle/, lib/, dist/, build/) are ignored
