# Repository Guidelines

## Project Structure & Module Organization
- `projects/`: Git submodules for plugins (`plugin-*`), plus `fred/` and `db4o-7.4/`.
- `buildSrc/`: Gradle Kotlin build logic (no plugin source changes committed).
- Output: `build/libs/` (standard JARs), `build/libs-crypta/` (shadow JARs), `build/deps/`.

## Build, Test, and Development Commands
- `./gradlew listPlugins`: Show discovered Gradle/Ant plugins.
- `./gradlew buildAll`: Build all plugins, collect JARs, create shadow JARs.
- `./gradlew buildGradleOnly`: Build Gradle plugins and collect JARs.
- `./gradlew buildAntPlugins`: Build Ant plugins.
- `./gradlew buildFred`: Build core deps (freenet.jar, ext jar) from `projects/fred/`.
- `./gradlew createShadowJars`: Build Cryptad‑compatible shadow JARs.
- `./gradlew diagnoseBuildIssues`: Print environment and dependency checks.
- `./gradlew cleanAll`: Clean plugin builds and collected outputs.
 - `./gradlew collectJars`: Re-scan plugins and copy built JARs into `build/libs/`.

Examples:
- Build a single Gradle plugin in isolation: `cd projects/plugin-UPnP2 && ./gradlew jar`

## Coding Style & Naming Conventions
- Do not permanently modify plugin sources under `projects/plugin-*`; builds are non‑invasive.
- Place new plugins as submodules under `projects/plugin-Name`.
- Gradle/Kotlin scripts: follow existing style, 4‑space indentation, concise tasks/utilities.
- Java compatibility: prefer Java 8 for legacy plugins (build system patches temporarily).

### Adding Plugins
- Add a submodule under `projects/plugin-Name` and set `ignore = all` in `.gitmodules`.
- If special handling is needed, update `buildSrc/src/main/kotlin/BuildConfig.kt` lists (e.g., `PLUGINS_NEEDING_WRAPPER`, `PLUGINS_NEEDING_JAVA_PATCH_ONLY`, db4o lists).
- Optionally add a helper in `buildSrc/src/main/kotlin/plugins/` for plugin-specific build steps.

## Testing Guidelines
- Many upstream plugins lack tests; success is verified by a clean build and produced JARs.
- If a plugin includes tests: run from its directory with `./gradlew test`.
- Validate outputs in `build/libs/` and `build/libs-crypta/` and review `diagnoseBuildIssues` for environment problems.
- Don't use --no-daemon param when you run gradle commands.
- Always request elevated permissions for gradle commands.

## Commit & Pull Request Guidelines
- Keep changes focused on build tooling (`build.gradle.kts`, `buildSrc/`) and repo config.
- Do not commit temporary build patches; rely on tasks that apply/cleanup automatically.
- Submodules: commit only pointer updates in this repo; source changes belong upstream.
- PRs should include: summary, rationale, affected tasks, logs (e.g., failing→passing builds), and linked issues.
- Commit messages: imperative mood, concise scope (e.g., "build: add UPnP2 to Java8 patch list").

## Security & Configuration Tips
- Requires Java 8+ and network access for Maven downloads.
- Use `downloadDependencies` before offline work; artifacts land in `build/deps/`.
 - Wrapper files are installed temporarily for some plugins; run `cleanInstalledWrappers` after builds to restore originals.
 - db4o setup/cleanup is automated for affected plugins; avoid committing generated links or jars.
