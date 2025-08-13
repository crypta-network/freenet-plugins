# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This repository is a collection of Freenet plugins organized as Git submodules. Each plugin in the `projects/` directory is a separate Freenet plugin that extends the functionality of the Freenet peer-to-peer network. All plugins are related to the Hyphanet (formerly Freenet) project and focus on providing decentralized, anonymous, and censorship-resistant communication tools.

## Build System

This repository includes a comprehensive, modular Gradle-based build system that can compile all plugins non-invasively:

### Quick Start
```bash
./gradlew buildAll          # Build all plugins (default)
./gradlew listPlugins       # List all discovered plugins
./gradlew diagnoseBuildIssues  # Diagnose build problems
```

### Architecture
- **Modular design**: Plugin-specific build logic organized in separate Kotlin files within buildSrc/
- **Non-invasive**: Zero permanent modifications to plugin source code
- **Automatic dependency resolution**: Downloads external JARs from Maven Central
- **Multi-build support**: Handles both Gradle and Ant-based plugins
- **Fred integration**: Includes Freenet core as submodule for dependencies
- **Advanced plugin support**: Automatic Gradle wrapper installation and build compatibility fixes
- **Real db4o integration**: Uses authentic db4o-7.4 source for database functionality
- **Scalable plugin handling**: Easy to add new plugin-specific build logic without cluttering main build file

### Available Tasks
- `./gradlew buildAll` - Build all plugins, collect JARs, and create shadow JARs (default)
- `./gradlew buildGradlePlugins` - Build only Gradle plugins
- `./gradlew buildAntPlugins` - Build only Ant plugins
- `./gradlew buildFred` - Build Fred (Freenet core dependencies)
- `./gradlew createShadowJars` - Create shadow JARs with package relocation
- `./gradlew buildAllWithShadow` - Build all and create shadow JARs separately
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

#### plugin-Freereader Integration
- **Special Java version handling**: Creates temporary build.xml with corrected Java 8 source/target settings
- **db4o compatibility**: Uses List-compatible ObjectSet from db4o JDK 1.2 version via shared JAR
- **Non-invasive fixes**: Temporary build file modifications without altering source code
- **Legacy API support**: Maintains compatibility with older db4o API expectations

#### plugin-Library Integration
- **Generic type compatibility**: Fixes ProgressTracker generic type constraints for Java 21 compatibility
- **SnakeYAML dependency**: Provides SnakeYAML 1.5 JAR compatible with plugin's API usage
- **Non-invasive source patching**: Temporarily patches source files during build, automatically restored after compilation
- **Progress interface import**: Adds missing Progress interface import for proper type resolution

#### plugin-SNMP Integration
- **IOStatisticCollector API compatibility**: Fixes compatibility with current Freenet IOStatisticCollector API
- **Missing method replacement**: Replaces deprecated `getTotalStatistics()` with `getTotalIO()` and approximation logic
- **Constant substitution**: Replaces missing `STATISTICS_ENTRIES` constant with fixed value for basic I/O statistics
- **Non-invasive patching**: Temporarily modifies source files during build with automatic restoration

#### plugin-JSTUN Integration
- **Tanuki Wrapper dependency**: Downloads and extracts authentic Tanuki Wrapper Community Edition (3.6.2)
- **WrapperManager support**: Provides real WrapperManager classes for proper shutdown detection
- **Automatic JAR extraction**: Extracts wrapper.jar from tar.gz archive to build dependencies
- **Clean dependency provision**: Uses Ant `-lib` flag to add wrapper.jar to classpath without source modifications

#### plugin-FlogHelper Integration
- **Java compatibility fixes**: Automatically patches build.gradle to use Java 8 instead of Java 7 during builds
- **Manifest typo fix**: Corrects Plugin-Main-Class from 'plugins.flophelper.FlogHelper' to 'plugins.floghelper.FlogHelper'
- **Build isolation**: Uses temporary settings.gradle to prevent buildSrc interference during build
- **Original wrapper preservation**: Maintains plugin's original Gradle wrapper files without modification
- **Non-invasive patching**: Temporarily applies fixes during build, automatically restored after completion
- **Wrapper source for other plugins**: Serves as source for Gradle wrapper installation for plugins needing them
- **Separate cleanup handling**: Uses dedicated cleanup logic that preserves original wrapper files while restoring build.gradle

#### plugin-Freemail Integration
- **Template loader path fix**: Patches WebPage.java to change loader.setPrefix from "/resources/templates/" to "templates/"
- **Classpath compatibility**: Fixes template loading for standard Java layout where resources/ is not part of classpath root
- **Non-invasive patching**: Temporarily patches source during build, automatically restored after compilation
- **ClassLoader compliance**: Removes leading slash for proper ClassLoader.getResourceAsStream() usage

#### plugin-KeyUtils Integration
- **Dependency resolution fixes**: Replaces unresolvable Maven dependency `fred:build+` with local file dependencies to freenet.jar and freenet-ext.jar
- **Java compatibility fixes**: Updates Java version from 1.7 to 1.8 for modern compatibility
- **Build isolation enhancement**: Creates temporary `settings.gradle` files during build to prevent buildSrc interference from parent project
- **File path resolution fixes**: Replaces problematic `getMTime()` file access with `new Date()` to avoid working directory issues
- **Non-invasive patching**: All modifications temporarily applied during build and automatically restored after completion

#### db4o-7.4 Database Integration
The build system provides comprehensive db4o database support for plugins that require it:

**Supported Plugins**:
- **Ant plugins**: XMLLibrarian, XMLSpider - receive db4o via shared JAR on classpath
- **Ant plugins (JAR-only)**: Freereader - receives db4o via shared JAR on classpath (no source symlink)
- **Gradle plugins**: WebOfTrust, Freetalk - receive db4o.jar copied to their db4o-7.4/ directory

**Architecture**:
- **Shared compilation**: Creates single `build/deps/db4o-7.4.jar` from authentic db4o-7.4 source with JDK 1.2 compatibility layer
- **List compatibility**: Includes db4ojdk1.2 sources to provide ObjectSet that implements java.util.List for legacy plugins
- **Non-invasive delivery**: 
  - Ant plugins: Uses `-lib` flag to add shared JAR to classpath
  - Gradle plugins: Copies shared JAR as `db4o-7.4/db4o.jar` expected by build.gradle
- **Source symlinks**: Standard Ant plugins get temporary symlinks to db4o-7.4/src for compilation access
- **Complete cleanup**: All symlinks and copied JARs automatically removed after build

**Scalability**: Adding new plugins requires only updating plugin name lists - no custom build logic needed

### Performance Optimizations
The build system includes several performance enhancements for faster, more efficient builds:

#### Build Performance
- **Lazy evaluation**: Directory references and plugin discovery use lazy initialization to reduce startup time
- **Incremental builds**: Proper input/output declarations enable Gradle's incremental build capabilities
- **Build caching**: Tasks configured with `outputs.cacheIf { true }` for faster subsequent builds
- **Parallel execution**: gradle.properties configured for parallel builds and optimized memory usage
- **Configuration on demand**: Reduces configuration time by only configuring required subprojects

#### Task Dependencies & Execution
- **Optimized task ordering**: Uses `mustRunAfter` instead of heavy `dependsOn` where appropriate
- **Up-to-date checks**: Smart up-to-date detection for wrapper installation and db4o setup tasks
- **Build isolation**: Gradle plugin builds use temporary `settings.gradle` files to prevent buildSrc interference
- **Memory optimization**: JVM configured with `-Xmx2048m -XX:MaxMetaspaceSize=512m` for optimal performance

#### Caching & Configuration
- **File system watching**: Enabled via `org.gradle.vfs.watch=true` for faster file change detection  
- **Kotlin incremental compilation**: Enabled for faster buildSrc compilation
- **Configuration cache ready**: Prepared for Gradle's configuration cache (commented out for compatibility)
- **Build scan integration**: Ready for detailed build performance analysis

#### gradle.properties Configuration
```properties
# Performance settings
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
org.gradle.daemon=true

# Memory optimization
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError

# Modern Gradle features
org.gradle.vfs.watch=true
kotlin.incremental=true
kotlin.caching.enabled=true
```

### Build Output
- All built JARs are collected in `./build/libs/` with plugin-specific names
- Shadow JARs with relocated packages are created in `./build/libs-crypta/`
- Build artifacts are isolated and don't affect git status
- Successfully builds 22/22 plugins (100% success rate) including all db4o-dependent plugins
  - 6 Gradle-based plugins: WebOfTrust, Freetalk, FlogHelper, KeepAlive, Freemail, KeyUtils
  - 16 Ant-based plugins: HelloWorld, ThawIndexBrowser, XMLLibrarian, TestGallery, Freereader, Librarian, HelloFCP, UPnP, Library, MDNSDiscovery, sharesite, SNMP, JSTUN, Spider, XMLSpider
- Advanced compatibility fixes enable all plugins to build with current Freenet API
- Shadow JARs include bytecode-level package relocation:
  - `freenet.*` → `network.crypta.*`
  - `com.mitchellbosecke.*` → `io.pebbletemplates.*`
- Plugin manifests (including Plugin-Main-Class) are preserved in shadow JARs
- 3 plugins are intentionally excluded: plugin-Echo (extensive API incompatibilities), plugin-Freemail-v0.1 (superseded), plugin-old-bookmarkplugin (deprecated)

### Dependencies
The build system automatically handles:
- **Fred (Freenet core)**: Built from submodule in `projects/fred/`
- **db4o-7.4**: Shared database JAR compiled from submodule in `projects/db4o-7.4/` and stored in `build/deps/`
- **External JARs**: SnakeYAML 1.5, XOM, BouncyCastle downloaded from Maven Central and stored in `build/deps/`
- **Tanuki Wrapper**: Downloads and extracts wrapper.jar from official Tanuki Software Community Edition for WrapperManager support
- **Plugin dependencies**: Proper classpath setup for both Ant and Gradle plugins
- **Gradle wrappers**: Automatic installation and cleanup for plugins requiring them

### Modular Build System Architecture
The build system uses a modular design with plugin-specific logic separated into dedicated files:

**buildSrc/ Structure**:
- `BuildConfig.kt` - Central configuration constants and plugin lists
- `BuildUtils.kt` - Shared utility functions for patching, downloading, and command execution
- `PluginInfo.kt` - Data class for plugin metadata
- `plugins/FreereaderPlugin.kt` - Freereader-specific build logic with Java 8 compatibility
- `plugins/LibraryPlugin.kt` - Library plugin generics patching and Progress interface handling
- `plugins/SNMPPlugin.kt` - SNMP plugin IOStatisticCollector API compatibility fixes
- `plugins/JSTUNPlugin.kt` - JSTUN plugin Tanuki Wrapper integration
- `plugins/FlogHelperPlugin.kt` - FlogHelper manifest typo fix and build isolation
- `plugins/FreemailPlugin.kt` - Freemail template loader path fixes

**Benefits**:
- **Maintainable**: Plugin-specific complexity isolated from main build file
- **Scalable**: Easy to add new plugin handlers without cluttering main build logic
- **Reusable**: Common utilities shared across all build modules
- **Clear separation**: Each plugin's unique build requirements clearly documented in its own file

### Git Configuration
- All submodules configured with `ignore = all` to ignore build artifacts
- Source code integrity preserved - no tracked files are modified
- Build directories (.gradle/, lib/, dist/, build/) are ignored
- Temporary modifications automatically cleaned up by finalizedBy cleanup tasks
