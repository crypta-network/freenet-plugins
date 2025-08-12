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
- **Non-invasive**: Zero permanent modifications to plugin source code
- **Automatic dependency resolution**: Downloads external JARs from Maven Central
- **Multi-build support**: Handles both Gradle and Ant-based plugins
- **Fred integration**: Includes Freenet core as submodule for dependencies
- **Advanced plugin support**: Automatic Gradle wrapper installation and build compatibility fixes
- **Real db4o integration**: Uses authentic db4o-7.4 source for database functionality

### Available Tasks
- `./gradlew buildAll` - Build all plugins and collect JARs (default)
- `./gradlew buildGradlePlugins` - Build only Gradle plugins
- `./gradlew buildAntPlugins` - Build only Ant plugins
- `./gradlew buildFred` - Build Fred (Freenet core dependencies)
- `./gradlew downloadDependencies` - Download missing external dependencies
- `./gradlew installGradleWrappers` - Install Gradle wrappers for plugins that need them
- `./gradlew cleanInstalledWrappers` - Remove temporarily installed wrappers and restore originals
- `./gradlew fixAllBuildIssues` - Apply build fixes (non-invasively)
- `./gradlew diagnoseBuildIssues` - Show comprehensive build diagnosis
- `./gradlew listPlugins` - List all plugins by type
- `./gradlew cleanAll` - Clean all plugin builds

### Advanced Plugin Building
The build system includes special handling for complex plugins:

#### WebOfTrust and Freetalk Integration
- **Automatic Gradle wrapper installation**: Copies wrapper from plugin-FlogHelper for compatibility
- **Java compatibility fixes**: Temporarily patches build.gradle files to use Java 8 instead of Java 7
- **Real db4o compilation**: Creates symlinks to authentic db4o-7.4 source code
- **Test compilation skipping**: Bypasses test compilation issues with Java 21 compatibility
- **Deprecated property fixes**: Updates archiveBaseName and destinationDirectory for modern Gradle
- **Complete cleanup**: All temporary modifications are automatically restored after building

#### db4o-7.4 Database Integration
The build system provides comprehensive db4o database support for plugins that require it:

**Supported Plugins**:
- **Ant plugins**: XMLLibrarian, XMLSpider - receive db4o via shared JAR on classpath
- **Gradle plugins**: WebOfTrust, Freetalk - receive db4o.jar copied to their db4o-7.4/ directory

**Architecture**:
- **Shared compilation**: Creates single `build/deps/db4o-7.4.jar` from authentic db4o-7.4 source
- **Non-invasive delivery**: 
  - Ant plugins: Uses `-lib` flag to add shared JAR to classpath
  - Gradle plugins: Copies shared JAR as `db4o-7.4/db4o.jar` expected by build.gradle
- **Source symlinks**: Ant plugins get temporary symlinks to db4o-7.4/src for compilation access
- **Complete cleanup**: All symlinks and copied JARs automatically removed after build

**Scalability**: Adding new plugins requires only updating plugin name lists - no custom build logic needed

### Build Output
- All built JARs are collected in `./build/libs/` with plugin-specific names
- Build artifacts are isolated and don't affect git status
- Successfully builds 13/22 plugins including all db4o-dependent plugins (XMLLibrarian, XMLSpider, WebOfTrust, Freetalk)
- JARs contain authentic compiled functionality (larger sizes for db4o plugins reflect real database integration)

### Dependencies
The build system automatically handles:
- **Fred (Freenet core)**: Built from submodule in `projects/fred/`
- **db4o-7.4**: Shared database JAR compiled from submodule in `projects/db4o-7.4/` and stored in `build/deps/`
- **External JARs**: SnakeYAML, XOM, BouncyCastle downloaded from Maven Central and stored in `build/deps/`
- **Plugin dependencies**: Proper classpath setup for both Ant and Gradle plugins
- **Gradle wrappers**: Automatic installation and cleanup for plugins requiring them

### Git Configuration
- All submodules configured with `ignore = all` to ignore build artifacts
- Source code integrity preserved - no tracked files are modified
- Build directories (.gradle/, lib/, dist/, build/) are ignored
- Temporary modifications automatically cleaned up by finalizedBy cleanup tasks
