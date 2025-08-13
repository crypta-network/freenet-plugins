import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import plugins.*
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    base
    id("com.gradleup.shadow")
}

// Directory references
val projectsDir = file("projects")
val buildLibsDir = file("build/libs")
val buildDepsDir = file("build/deps")
val tempBuildDir = file("build/temp-build-files")
val shadowLibsDir = file(BuildConfig.SHADOW_LIBS_DIR)

// Plugin discovery
val allPlugins: List<PluginInfo> by lazy {
    projectsDir.listFiles { file -> 
        file.isDirectory && file.name.startsWith("plugin-")
    }?.map { dir ->
        PluginInfo(
            dir = dir,
            name = dir.name,
            isGradle = file("${dir}/build.gradle").exists() || file("${dir}/build.gradle.kts").exists(),
            isAnt = file("${dir}/build.xml").exists()
        )
    } ?: emptyList()
}

val gradlePlugins = allPlugins.filter { it.isGradle }
val antPlugins = allPlugins.filter { it.isAnt && !it.isGradle }

println("Found ${gradlePlugins.size} Gradle plugins and ${antPlugins.size} Ant plugins")

// Helper function for command execution
fun executeCommand(command: List<String>, workingDir: File? = null): Int {
    return BuildUtils.executeCommand(command, workingDir)
}

// Task: Build Fred (Freenet core dependencies)
val buildFred = tasks.register("buildFred") {
    description = "Build Fred (Freenet core) to provide dependencies for plugins"
    group = "dependencies"
    
    val fredDir = file("projects/fred")
    val fredJar = file("projects/fred/build/output/freenet.jar")
    val fredExtJar = file("projects/fred/build/output/freenet-ext-29.jar")
    val fredSettingsFile = file("projects/fred/settings.gradle")
    
    inputs.files(fileTree(fredDir) { 
        include("src/**/*", "build.gradle", "dependencies.properties")
    })
    outputs.files(fredJar, fredExtJar)
    
    onlyIf { !fredJar.exists() || !fredExtJar.exists() }
    
    doLast {
        println("Building Fred (Freenet core dependencies)...")
        
        // Create settings.gradle for Fred if needed
        if (!fredSettingsFile.exists()) {
            fredSettingsFile.writeText("rootProject.name = 'fred'")
        }
        
        val exitCode = executeCommand(
            listOf(file("${fredDir}/gradlew").absolutePath, "-p", fredDir.absolutePath, "jar", "copyRuntimeLibs"),
            workingDir = fredDir
        )
        
        if (exitCode == 0) {
            println("Successfully built Fred dependencies")
            
            // Create expected directory structure and symlinks
            val distDir = file("projects/fred/dist")
            val libDir = file("projects/fred/lib")
            distDir.mkdirs()
            libDir.mkdirs()
            
            BuildUtils.createSymlink(
                Paths.get("../build/output/freenet.jar"),
                file("projects/fred/dist/freenet.jar").toPath()
            )
            BuildUtils.createSymlink(
                Paths.get("../build/output/freenet-ext-29.jar"),
                file("projects/fred/lib/freenet-ext.jar").toPath()
            )
        }
    }
}

// Task: Download external dependencies
val downloadDependencies = tasks.register("downloadDependencies") {
    description = "Download missing external dependencies for plugins"
    group = "dependencies"
    
    doLast {
        println("Downloading missing external dependencies...")
        buildDepsDir.mkdirs()
        
        BuildConfig.EXTERNAL_DEPENDENCIES.forEach { (fileName, url) ->
            BuildUtils.downloadFile(url, file("${buildDepsDir}/${fileName}"))
        }
        
        // Setup plugin-specific dependencies
        setupPluginDependencies()
        
        // Extract wrapper.jar from Tanuki Wrapper for plugin-JSTUN
        extractWrapperJar()
    }
}

fun extractWrapperJar() {
    val wrapperTarGz = file("${buildDepsDir}/wrapper-delta-pack-3.6.2.tar.gz")
    val wrapperJar = file("${buildDepsDir}/wrapper.jar")
    
    if (wrapperTarGz.exists() && !wrapperJar.exists()) {
        println("Extracting wrapper.jar from Tanuki Wrapper...")
        val tempDir = file("${buildDepsDir}/temp-wrapper")
        tempDir.mkdirs()
        
        try {
            // Extract tar.gz
            val tarCommand = listOf("tar", "-xzf", wrapperTarGz.absolutePath, "-C", tempDir.absolutePath)
            if (executeCommand(tarCommand) == 0) {
                // Find and copy wrapper.jar
                val extractedWrapperJar = file("${tempDir}/wrapper-delta-pack-3.6.2/lib/wrapper.jar")
                if (extractedWrapperJar.exists()) {
                    extractedWrapperJar.copyTo(wrapperJar)
                    println("Successfully extracted wrapper.jar")
                } else {
                    println("Warning: wrapper.jar not found in expected location")
                }
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }
}

fun setupPluginDependencies() {
    // Library plugin
    val libraryLib = file("projects/plugin-Library/lib")
    libraryLib.mkdirs()
    val snakeYamlSource = file("${buildDepsDir}/snakeyaml-1.5.jar")
    if (snakeYamlSource.exists()) {
        snakeYamlSource.copyTo(file("projects/plugin-Library/lib/snakeyaml-1.5.jar"), overwrite = true)
    }
    
    // Freemail BouncyCastle symlink
    val fredBcJar = file("projects/fred/build/output/bcprov-jdk15on-1.59.jar")
    val freemailBcJar = file("projects/fred/lib/bcprov-jdk15on-151.jar")
    if (fredBcJar.exists() && !freemailBcJar.exists()) {
        freemailBcJar.parentFile.mkdirs()
        fredBcJar.copyTo(freemailBcJar)
    }
}

// Task: Install Gradle wrappers
val installGradleWrappers = tasks.register("installGradleWrappers") {
    description = "Install Gradle wrapper for plugins without one"
    group = "build"
    
    doLast {
        val sourcePluginDir = file("projects/plugin-FlogHelper")
        val wrapperFiles = listOf(
            "gradlew" to "gradlew",
            "gradlew.bat" to "gradlew.bat",
            "gradle/wrapper/gradle-wrapper.jar" to "gradle/wrapper/gradle-wrapper.jar",
            "gradle/wrapper/gradle-wrapper.properties" to "gradle/wrapper/gradle-wrapper.properties"
        )
        
        BuildConfig.PLUGINS_NEEDING_WRAPPER.forEach { pluginName ->
            val pluginDir = file("projects/${pluginName}")
            if (pluginDir.exists() && !file("${pluginDir}/gradlew").exists()) {
                println("Installing Gradle wrapper for ${pluginName}...")
                
                // Copy wrapper files
                wrapperFiles.forEach { (source, target) ->
                    val sourceFile = File(sourcePluginDir, source)
                    val targetFile = File(pluginDir, target)
                    
                    if (sourceFile.exists()) {
                        targetFile.parentFile.mkdirs()
                        sourceFile.copyTo(targetFile, overwrite = true)
                        if (target == "gradlew") {
                            targetFile.setExecutable(true)
                        }
                    }
                }
                
                // Patch build.gradle for Java 8 compatibility
                val buildGradleFile = File(pluginDir, "build.gradle")
                if (buildGradleFile.exists()) {
                    val original = buildGradleFile.readText()
                    val patched = BuildUtils.patchJavaVersion(original, true)
                    if (original != patched) {
                        File(pluginDir, "build.gradle.original").writeText(original)
                        buildGradleFile.writeText(patched)
                        println("Patched build.gradle for ${pluginName}")
                    }
                }
                
                // Create db4o symlink if needed
                if (pluginName in BuildConfig.GRADLE_PLUGINS_NEEDING_DB4O) {
                    val db4oDir = File(pluginDir, "db4o-7.4")
                    if (db4oDir.exists() && !Files.isSymbolicLink(db4oDir.toPath())) {
                        db4oDir.deleteRecursively()
                        executeCommand(
                            listOf("ln", "-s", file("projects/db4o-7.4").absolutePath, db4oDir.absolutePath)
                        )
                    }
                }
            }
        }
        
        // Handle plugins that only need Java version patching (no wrapper installation)
        BuildConfig.PLUGINS_NEEDING_JAVA_PATCH_ONLY.forEach { pluginName ->
            val pluginDir = file("projects/${pluginName}")
            if (pluginDir.exists()) {
                // Patch build.gradle for Java 8 compatibility
                val buildGradleFile = File(pluginDir, "build.gradle")
                if (buildGradleFile.exists()) {
                    val original = buildGradleFile.readText()
                    val patched = if (pluginName == "plugin-KeyUtils") {
                        BuildUtils.patchKeyUtilsBuildGradle(original)
                    } else {
                        BuildUtils.patchJavaVersion(original, true)
                    }
                    if (original != patched) {
                        File(pluginDir, "build.gradle.original").writeText(original)
                        buildGradleFile.writeText(patched)
                        println("Patched build.gradle for ${pluginName}")
                    }
                }
            }
        }
    }
}

// Task: Clean installed wrappers
val cleanInstalledWrappers = tasks.register("cleanInstalledWrappers") {
    description = "Remove temporarily installed Gradle wrappers and restore originals"
    group = "build"
    dependsOn("cleanupDb4oSupport")
    
    doLast {
        BuildConfig.PLUGINS_NEEDING_WRAPPER.forEach { pluginName ->
            val pluginDir = file("projects/${pluginName}")
            if (pluginDir.exists()) {
                // Remove wrapper files
                listOf("gradlew", "gradlew.bat", "gradle").forEach { fileName ->
                    val file = File(pluginDir, fileName)
                    if (file.exists()) {
                        if (file.isDirectory) file.deleteRecursively() else file.delete()
                    }
                }
                
                // Restore original build.gradle
                val originalBuildGradle = File(pluginDir, "build.gradle.original")
                if (originalBuildGradle.exists()) {
                    originalBuildGradle.copyTo(File(pluginDir, "build.gradle"), overwrite = true)
                    originalBuildGradle.delete()
                }
                
                // Remove db4o symlink
                val db4oDir = File(pluginDir, "db4o-7.4")
                if (db4oDir.exists() && Files.isSymbolicLink(db4oDir.toPath())) {
                    db4oDir.delete()
                    db4oDir.mkdir()
                }
            }
        }
        
        // Handle plugins that only need Java version patching cleanup (restore build.gradle only)
        BuildConfig.PLUGINS_NEEDING_JAVA_PATCH_ONLY.forEach { pluginName ->
            val pluginDir = file("projects/${pluginName}")
            if (pluginDir.exists()) {
                // Restore original build.gradle
                val originalBuildGradle = File(pluginDir, "build.gradle.original")
                if (originalBuildGradle.exists()) {
                    originalBuildGradle.copyTo(File(pluginDir, "build.gradle"), overwrite = true)
                    originalBuildGradle.delete()
                    println("Restored original build.gradle for ${pluginName}")
                }
            }
        }
    }
}

// Task: Create db4o JAR
val createDb4oJar = tasks.register("createDb4oJar") {
    description = "Create shared db4o JAR for plugins that need it"
    group = "build"
    dependsOn("setupAntPluginDb4o")
    
    val db4oJar = file("build/deps/${BuildConfig.DB4O_JAR_NAME}")
    outputs.file(db4oJar)
    
    doLast {
        if (BuildConfig.ALL_PLUGINS_NEEDING_DB4O.isNotEmpty()) {
            val db4oSrcDir = BuildConfig.ALL_PLUGINS_NEEDING_DB4O
                .map { file("projects/${it}/db4o-7.4/src") }
                .find { it.exists() }
            
            if (db4oSrcDir != null) {
                println("Creating db4o JAR...")
                val tempBuildDir = file("build/temp/db4o-build")
                tempBuildDir.mkdirs()
                db4oJar.parentFile.mkdirs()
                
                // Build only essential db4o classes - use JDK1.2 version for ObjectSet compatibility
                val db4ojDir = File(db4oSrcDir, "db4oj/core/src")
                val jdk12Dir = File(db4oSrcDir, "db4ojdk1.2/core/src") 
                
                if (db4ojDir.exists()) {
                    val tempSrcDir = File(buildDir, "temp/db4o-src")
                    tempSrcDir.mkdirs()
                    
                    // Copy all base db4oj sources
                    copy {
                        from(db4ojDir) 
                        into(tempSrcDir)
                        exclude("**/test/**")
                    }
                    
                    // Override with JDK1.2 versions for List compatibility
                    if (jdk12Dir.exists()) {
                        copy {
                            from(jdk12Dir)
                            into(tempSrcDir)
                            exclude("**/test/**")
                        }
                    }
                    
                    // Find all Java files in the merged source directory
                    val javaFiles = fileTree(tempSrcDir) {
                        include("**/*.java")
                    }.files.map { it.absolutePath }
                    
                    // Compile merged sources
                    val compileCmd = listOf("javac", 
                        "-d", tempBuildDir.absolutePath,
                        "-source", "8", 
                        "-target", "8") + javaFiles
                    if (executeCommand(compileCmd) == 0) {
                        // Create JAR
                        val jarCmd = listOf("jar", "cf", db4oJar.absolutePath, "-C", tempBuildDir.absolutePath, ".")
                        if (executeCommand(jarCmd) == 0) {
                            println("Successfully created db4o JAR: ${db4oJar}")
                        }
                    }
                    tempBuildDir.deleteRecursively()
                }
            }
        }
    }
}

// Task: Setup db4o for Ant plugins
val setupAntPluginDb4o = tasks.register("setupAntPluginDb4o") {
    description = "Set up db4o source symlinks for Ant plugins that require it"
    group = "build"
    
    doLast {
        val realDb4oDir = file("projects/db4o-7.4")
        if (realDb4oDir.exists()) {
            BuildConfig.ANT_PLUGINS_NEEDING_DB4O.forEach { pluginName ->
                val pluginDir = file("projects/${pluginName}")
                if (pluginDir.exists()) {
                    println("Setting up db4o source symlink for ${pluginName}...")
                    val db4oDir = File(pluginDir, "db4o-7.4")
                    db4oDir.mkdirs()
                    
                    val srcDir = File(db4oDir, "src")
                    if (!Files.isSymbolicLink(srcDir.toPath())) {
                        if (srcDir.exists()) srcDir.deleteRecursively()
                        executeCommand(
                            listOf("ln", "-sf", "${realDb4oDir.absolutePath}/src", srcDir.absolutePath)
                        )
                    }
                }
            }
        }
    }
}

// Task: Setup db4o for Gradle plugins
val setupGradlePluginDb4o = tasks.register("setupGradlePluginDb4o") {
    description = "Copy shared db4o JAR to Gradle plugins that need it"
    group = "build"
    dependsOn(createDb4oJar)
    
    doLast {
        val db4oJar = file("build/deps/${BuildConfig.DB4O_JAR_NAME}")
        if (db4oJar.exists()) {
            BuildConfig.GRADLE_PLUGINS_NEEDING_DB4O.forEach { pluginName ->
                val pluginDir = file("projects/${pluginName}")
                if (pluginDir.exists()) {
                    val pluginDb4oDir = File(pluginDir, "db4o-7.4")
                    pluginDb4oDir.mkdirs()
                    db4oJar.copyTo(File(pluginDb4oDir, "db4o.jar"), overwrite = true)
                    println("Copied db4o.jar to ${pluginName}")
                }
            }
        }
    }
}

// Task: Cleanup db4o support
val cleanupDb4oSupport = tasks.register("cleanupDb4oSupport") {
    description = "Clean up db4o setup for all plugins"
    group = "build"
    
    doLast {
        // Clean Ant plugins
        BuildConfig.ANT_PLUGINS_NEEDING_DB4O.forEach { pluginName ->
            val srcDir = file("projects/${pluginName}/db4o-7.4/src")
            if (Files.isSymbolicLink(srcDir.toPath())) {
                srcDir.delete()
                println("Removed db4o-7.4/src symlink for ${pluginName}")
            }
        }
        
        // Clean Gradle plugins
        BuildConfig.GRADLE_PLUGINS_NEEDING_DB4O.forEach { pluginName ->
            val db4oJar = file("projects/${pluginName}/db4o-7.4/db4o.jar")
            if (db4oJar.exists()) {
                db4oJar.delete()
                println("Removed db4o.jar from ${pluginName}")
            }
        }
    }
}

// Task: Build Gradle plugins
val buildGradlePlugins = tasks.register("buildGradlePlugins") {
    description = "Build all Gradle-based plugins"
    group = "build"
    dependsOn(buildFred, downloadDependencies, installGradleWrappers, setupGradlePluginDb4o)
    finalizedBy(cleanInstalledWrappers)
    
    doLast {
        gradlePlugins.forEach { plugin ->
            println("Building Gradle plugin: ${plugin.name}")
            
            val gradlewScript = file("${plugin.dir}/gradlew")
            val isWindows = System.getProperty("os.name").lowercase().contains("windows")
            val gradlewBat = file("${plugin.dir}/gradlew.bat")
            
            val skipTests = plugin.name in BuildConfig.PLUGINS_NEEDING_WRAPPER
            val gradleArgs = when {
                !isWindows && gradlewScript.exists() -> {
                    if (skipTests) {
                        listOf("bash", gradlewScript.absolutePath, "-p", plugin.dir.absolutePath, "clean", "jar", "-x", "compileTestJava", "-x", "test")
                    } else {
                        listOf("bash", gradlewScript.absolutePath, "-p", plugin.dir.absolutePath, "clean", "jar")
                    }
                }
                isWindows && gradlewBat.exists() -> {
                    if (skipTests) {
                        listOf("cmd", "/c", gradlewBat.absolutePath, "-p", plugin.dir.absolutePath, "clean", "jar", "-x", "compileTestJava", "-x", "test")
                    } else {
                        listOf("cmd", "/c", gradlewBat.absolutePath, "-p", plugin.dir.absolutePath, "clean", "jar")
                    }
                }
                else -> {
                    println("Warning: No Gradle wrapper found for ${plugin.name}")
                    null
                }
            }
            
            gradleArgs?.let { args ->
                if (executeCommand(args, workingDir = plugin.dir) == 0) {
                    println("Successfully built ${plugin.name}")
                }
            }
        }
    }
}

// Task: Build Ant plugins
val buildAntPlugins = tasks.register("buildAntPlugins") {
    description = "Build all Ant-based plugins"
    group = "build"
    dependsOn(buildFred, downloadDependencies, createDb4oJar)
    
    doLast {
        val db4oJar = file("build/deps/${BuildConfig.DB4O_JAR_NAME}")
        
        antPlugins.forEach { plugin ->
            val needsDb4o = plugin.name in BuildConfig.ANT_PLUGINS_NEEDING_DB4O || 
                           plugin.name in BuildConfig.ANT_PLUGINS_NEEDING_DB4O_JAR_ONLY
            
            // Delegate to plugin-specific build logic
            when (plugin.name) {
                "plugin-Freereader" -> FreereaderPlugin.buildFreereader(
                    plugin.dir, buildDir, db4oJar, ::executeCommand
                )
                "plugin-Library" -> LibraryPlugin.buildLibrary(
                    plugin.dir, buildDir, ::executeCommand
                )
                "plugin-SNMP" -> SNMPPlugin.buildSNMP(
                    plugin.dir, buildDir, ::executeCommand
                )
                "plugin-JSTUN" -> JSTUNPlugin.buildJSTUN(
                    plugin.dir, file("build/deps/wrapper.jar"), ::executeCommand
                )
                else -> {
                    // Standard Ant plugin build
                    println("Building Ant plugin: ${plugin.name}")
                    val antCommand = mutableListOf(
                        "ant", "clean", "dist",
                        "-Dsource-version=8",
                        "-Dtarget-version=8",
                        "-Dant.file.failonerror=false"
                    )
                    
                    if (needsDb4o && db4oJar.exists()) {
                        antCommand.addAll(listOf("-lib", db4oJar.absolutePath))
                    }
                    
                    if (executeCommand(antCommand, workingDir = plugin.dir) == 0) {
                        println("Successfully built ${plugin.name}")
                    }
                }
            }
        }
    }
}

// Task: Collect JARs
val collectJars = tasks.register("collectJars") {
    description = "Collect all built JAR files into build/libs directory"
    group = "build"
    dependsOn(buildGradlePlugins, buildAntPlugins)
    
    doLast {
        buildLibsDir.deleteRecursively()
        buildLibsDir.mkdirs()
        
        println("Collecting JARs into ${buildLibsDir.absolutePath}")
        
        var jarCount = 0
        allPlugins.forEach { plugin ->
            val jarFiles = fileTree(plugin.dir) {
                include("**/*.jar")
                exclude("**/gradle-wrapper.jar", "**/lib/**", "**/db4o-7.4/**", "**/gradle/**")
                exclude("**/gradle/wrapper/**", "**/gradle/gradle-witness.jar")
            }
            
            jarFiles.forEach { jarFile ->
                val targetName = "${plugin.name}-${jarFile.name}"
                jarFile.copyTo(file("${buildLibsDir}/${targetName}"), overwrite = true)
                println("Copying ${jarFile.name} from ${plugin.name}")
                jarCount++
            }
        }
        
        if (jarCount > 0) {
            println("\nCollected $jarCount JAR files:")
            buildLibsDir.listFiles { it.extension == "jar" }?.sorted()?.forEach {
                println("  - ${it.name}")
            }
        } else {
            println("\nNo JAR files were collected")
        }
    }
}

// Main build task
tasks.register("buildAll") {
    description = "Build all plugins and collect JARs"
    group = "build"
    dependsOn(collectJars)
    finalizedBy(cleanInstalledWrappers)
}

// Clean task
tasks.register("cleanAll") {
    description = "Clean all plugin builds and the collected JARs"
    group = "build"
    dependsOn(cleanInstalledWrappers)
    
    doLast {
        buildLibsDir.deleteRecursively()
        
        allPlugins.forEach { plugin ->
            println("Cleaning ${plugin.name}")
            
            val cleanCommand = when {
                plugin.isGradle -> {
                    val gradlewScript = file("${plugin.dir}/gradlew")
                    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
                    val gradlewBat = file("${plugin.dir}/gradlew.bat")
                    
                    when {
                        !isWindows && gradlewScript.exists() -> listOf("bash", gradlewScript.absolutePath, "clean")
                        isWindows && gradlewBat.exists() -> listOf("cmd", "/c", gradlewBat.absolutePath, "clean")
                        else -> listOf("gradle", "clean")
                    }
                }
                plugin.isAnt -> listOf("ant", "clean")
                else -> null
            }
            
            cleanCommand?.let { 
                BuildUtils.executeCommand(it, workingDir = plugin.dir, printErrors = false) 
            }
        }
    }
}

// Utility tasks
tasks.register("listPlugins") {
    description = "List all discovered plugins and their build types"
    group = "help"
    
    doLast {
        println("Gradle plugins (${gradlePlugins.size}):")
        gradlePlugins.forEach { println("  - ${it.name}") }
        
        println("\nAnt plugins (${antPlugins.size}):")
        antPlugins.forEach { println("  - ${it.name}") }
    }
}

tasks.register("buildAllVerbose") {
    description = "Build all plugins with verbose error output"
    group = "build"
    
    doFirst {
        System.setProperty("gradle.verbose", "true")
    }
    
    finalizedBy("buildAll")
}

tasks.register("buildGradleOnly") {
    description = "Build only Gradle-based plugins"
    group = "build"
    dependsOn(buildGradlePlugins)
    
    doLast {
        buildLibsDir.mkdirs()
        var jarCount = 0
        
        gradlePlugins.forEach { plugin ->
            val jarFiles = fileTree(plugin.dir) {
                include("**/*.jar")
                exclude("**/gradle-wrapper.jar", "**/lib/**", "**/libs/**", "**/db4o-7.4/**", "**/gradle/**")
            }
            
            jarFiles.forEach { jarFile ->
                val targetName = "${plugin.name}-${jarFile.name}"
                jarFile.copyTo(file("${buildLibsDir}/${targetName}"), overwrite = true)
                jarCount++
            }
        }
        
        println("\nCollected $jarCount JAR files from Gradle plugins")
    }
}

tasks.register("diagnoseBuildIssues") {
    description = "Diagnose common build issues with plugins"
    group = "help"
    
    doLast {
        println("=== Build Issues Diagnosis ===\n")
        
        println("1. Java Version: ${System.getProperty("java.version")}")
        println("   Plugins require Java 8+ for compilation")
        
        println("\n2. Freenet Dependencies:")
        val fredJar = file("projects/fred/build/output/freenet.jar")
        val fredExtJar = file("projects/fred/build/output/freenet-ext-29.jar")
        
        if (!file("projects/fred").exists()) {
            println("   ❌ Missing fred/ submodule - run 'git submodule update --init'")
        } else {
            println("   ✅ Fred submodule exists")
            println("   ${if (fredJar.exists() && fredExtJar.exists()) "✅" else "❌"} Fred build artifacts")
        }
        
        println("\n3. Plugin Summary:")
        println("   Total plugins: ${allPlugins.size}")
        println("   Gradle plugins: ${gradlePlugins.size}")
        println("   Ant plugins: ${antPlugins.size}")
        println("   Plugins needing db4o: ${BuildConfig.ALL_PLUGINS_NEEDING_DB4O.size}")
        
        println("\n4. Available Tasks:")
        println("   ./gradlew buildAll            - Build all plugins (default)")
        println("   ./gradlew buildGradleOnly     - Build only Gradle plugins")
        println("   ./gradlew buildAllVerbose     - Build with detailed error output")
        println("   ./gradlew listPlugins         - List all discovered plugins")
        println("   ./gradlew diagnoseBuildIssues - Show this diagnosis")
        println("   ./gradlew cleanAll            - Clean all builds")
    }
}

// Task: Create shadow JARs with relocated packages using proper Shadow plugin
val createShadowJars = tasks.register("createShadowJars") {
    description = "Create shadow JARs with relocated packages (freenet.* -> network.crypta.*)"
    group = "shadow"
    dependsOn(collectJars)
    
    doLast {
        shadowLibsDir.deleteRecursively()
        shadowLibsDir.mkdirs()
        
        println("Creating shadow JARs with package relocation...")
        println("Relocating: freenet.* -> network.crypta.*")
        
        val jarFiles = buildLibsDir.listFiles { it.extension == "jar" }?.sorted() ?: emptyList()
        var successCount = 0
        
        jarFiles.forEach { originalJar ->
            val shadowJarName = originalJar.nameWithoutExtension + "-shadow.jar"
            val shadowJar = file("${shadowLibsDir}/${shadowJarName}")
            
            println("Processing: ${originalJar.name} -> ${shadowJarName}")
            
            try {
                // Create a proper ShadowJar task for each JAR
                val tempShadowTask = tasks.create("tempShadow${originalJar.nameWithoutExtension}", ShadowJar::class.java) {
                    archiveFileName.set(shadowJarName)
                    destinationDirectory.set(shadowLibsDir)
                    from(zipTree(originalJar))
                    
                    // Relocate freenet packages with proper bytecode transformation
                    relocate("freenet", "network.crypta")
                    
                    // Exclude duplicates  
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    
                    // Preserve original manifest attributes, especially Plugin-Main-Class
                    manifest {
                        // Read original manifest
                        val originalManifestFile = zipTree(originalJar).matching { 
                            include("META-INF/MANIFEST.MF") 
                        }.singleFile
                        
                        if (originalManifestFile.exists()) {
                            val originalManifest = java.util.jar.Manifest(originalManifestFile.inputStream())
                            originalManifest.mainAttributes.forEach { key, value ->
                                attributes[key.toString()] = value.toString()
                            }
                        }
                    }
                }
                
                // Execute the shadow task
                tempShadowTask.actions.forEach { action ->
                    action.execute(tempShadowTask)
                }
                
                if (shadowJar.exists()) {
                    println("  ✓ Created ${shadowJarName}")
                    successCount++
                } else {
                    println("  ✗ Failed to create ${shadowJarName}")
                }
                
                // Note: Temp task cleanup not needed - tasks will be cleaned up after build
                
            } catch (e: Exception) {
                println("  ✗ Failed to create ${shadowJarName}: ${e.message}")
            }
        }
        
        println("\nSuccessfully created $successCount shadow JARs in ${shadowLibsDir}")
        
        shadowLibsDir.listFiles { it.extension == "jar" }?.sorted()?.forEach {
            println("  - ${it.name} (${it.length() / 1024} KB)")
        }
    }
}

// Task: Build all and create shadow JARs
tasks.register("buildAllWithShadow") {
    description = "Build all plugins and create shadow JARs with relocated packages"
    group = "build"
    dependsOn(collectJars, createShadowJars)
    finalizedBy(cleanInstalledWrappers)
}

// Set default task
defaultTasks("buildAll")