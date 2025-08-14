# Hyphanet (Freenet) Plugins Collection

A comprehensive collection of Hyphanet (formerly Freenet) plugins organized as Git submodules, with a unified build system for compiling all plugins for both standard Hyphanet and [Cryptad](https://github.com/crypta-network/cryptad) (a Hyphanet fork with renamed package namespaces).

## Overview

This repository contains plugins that extend the functionality of the Hyphanet peer-to-peer network (formerly known as Freenet). Each plugin in the `projects/` directory is a separate Hyphanet plugin that provides decentralized, anonymous, and censorship-resistant communication tools. The build system creates both standard plugin JARs and shadow JARs with relocated packages for Cryptad compatibility.

## Quick Start

```bash
# Build all plugins
./gradlew buildAll

# List available plugins
./gradlew listPlugins

# Diagnose build issues
./gradlew diagnoseBuildIssues
```

## Build System

The repository features a comprehensive, modular Gradle-based build system that compiles all plugins non-invasively without permanent modifications to source code.

### Key Features

- **Non-invasive**: Zero permanent modifications to plugin source code
- **Modular design**: Plugin-specific build logic in separate Kotlin files
- **Automatic dependency resolution**: Downloads external JARs from Maven Central
- **Multi-build support**: Handles both Gradle and Ant-based plugins
- **Advanced compatibility**: Automatic fixes for Java version compatibility
- **Shadow JAR creation**: Package relocation for deployment compatibility

### Available Tasks

```bash
./gradlew buildAll              # Build all plugins and create shadow JARs (default)
./gradlew buildGradlePlugins    # Build only Gradle plugins
./gradlew buildAntPlugins       # Build only Ant plugins
./gradlew buildFred             # Build Fred (Freenet REference Daemon)
./gradlew createShadowJars      # Create shadow JARs with package relocation
./gradlew downloadDependencies  # Download missing external dependencies
./gradlew cleanAll              # Clean all plugin builds
```

## Plugin Support

Successfully builds **22/22 plugins** (100% success rate):

### Gradle-based Plugins (6)
- WebOfTrust, Freetalk, FlogHelper, KeepAlive, Freemail, KeyUtils

### Ant-based Plugins (16)
- HelloWorld, ThawIndexBrowser, XMLLibrarian, TestGallery, Freereader
- Librarian, HelloFCP, UPnP, Library, MDNSDiscovery, sharesite
- SNMP, JSTUN, Spider, XMLSpider

### Advanced Plugin Integration

The build system includes special handling for complex plugins:

- **WebOfTrust/Freetalk**: Gradle wrapper installation, Java compatibility fixes, db4o integration
- **plugin-Library**: Generic type compatibility fixes for Java 21
- **plugin-Freereader**: Java version handling and db4o compatibility
- **plugin-SNMP**: IOStatisticCollector API compatibility fixes
- **plugin-JSTUN**: Tanuki Wrapper dependency integration
- **plugin-FlogHelper**: Manifest fixes and build isolation

## Dependencies

The build system automatically handles:

- **Fred (Freenet REference Daemon)**: Built from submodule
- **db4o-7.4**: Shared database JAR for database-dependent plugins
- **External JARs**: SnakeYAML, XOM, BouncyCastle from Maven Central
- **Tanuki Wrapper**: Official Community Edition for WrapperManager support

## Build Output

- **Standard JARs**: `./build/libs/` - Original plugin JARs for standard Hyphanet
- **Shadow JARs**: `./build/libs-crypta/` - Modified JARs for Cryptad compatibility

### Cryptad Compatibility

The shadow JARs are specifically created to make Hyphanet plugins compatible with [Cryptad](https://github.com/crypta-network/cryptad), a fork of Hyphanet that has renamed the `freenet.*` package namespace to `network.crypta.*`. 

The build system automatically:
- Relocates all `freenet.*` references to `network.crypta.*` at the bytecode level
- Preserves plugin manifests including Plugin-Main-Class entries
- Ensures plugins work seamlessly with Cryptad's renamed API

For more information about Cryptad, visit: https://github.com/crypta-network/cryptad

## Performance Features

- Parallel builds and optimized memory usage
- Incremental builds with proper up-to-date checks
- Build caching for faster subsequent builds
- Lazy evaluation for reduced startup time

## Architecture

The build system uses a modular design with plugin-specific logic separated into dedicated files in `buildSrc/`, making it maintainable and scalable for adding new plugins.

## Requirements

- Java 8 or later
- Git (for submodules)
- Internet connection (for dependency downloads)

## Contributing

When adding new plugins:
1. Add as Git submodule in `projects/`
2. Update plugin lists in `buildSrc/BuildConfig.kt`
3. Add plugin-specific build logic if needed in `buildSrc/plugins/`

## License

GNU General Public License v2.0 or later