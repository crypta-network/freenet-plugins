import java.io.File

object FlogHelperPlugin {
    
    fun buildFlogHelper(pluginDir: File, executeCommand: (List<String>, File?) -> Int) {
        println("Building FlogHelper plugin with manifest typo fix...")
        
        val buildGradleFile = File(pluginDir, "build.gradle")
        val originalContent = buildGradleFile.readText()
        var wasPatched = false
        
        try {
            // Fix the typo: flophelper -> floghelper
            val patchedContent = originalContent.replace(
                "'plugins.flophelper.FlogHelper'",
                "'plugins.floghelper.FlogHelper'"
            )
            
            // Only patch if the typo is present
            if (originalContent != patchedContent) {
                println("  Patching Plugin-Main-Class typo: flophelper -> floghelper")
                buildGradleFile.writeText(patchedContent)
                wasPatched = true
            }
            
            // Build with gradlew
            val gradlewScript = File(pluginDir, "gradlew")
            val isWindows = System.getProperty("os.name").lowercase().contains("windows")
            val gradlewBat = File(pluginDir, "gradlew.bat")
            
            val gradleArgs = when {
                !isWindows && gradlewScript.exists() -> {
                    listOf("bash", "./gradlew", "clean", "jar")
                }
                isWindows && gradlewBat.exists() -> {
                    listOf("cmd", "/c", "gradlew.bat", "clean", "jar")
                }
                else -> {
                    println("  Warning: No Gradle wrapper found for FlogHelper")
                    null
                }
            }
            
            // Create temporary settings.gradle for build isolation
            val originalSettings = File(pluginDir, "settings.gradle")
            val tempSettings = File(pluginDir, "settings.gradle.temp")
            val hasOriginalSettings = originalSettings.exists()
            
            if (hasOriginalSettings) {
                originalSettings.copyTo(tempSettings, overwrite = true)
            }
            
            // Create minimal settings.gradle to isolate the build
            originalSettings.writeText("rootProject.name = 'FlogHelper'")
            
            try {
                gradleArgs?.let { args ->
                    if (executeCommand(args, pluginDir) == 0) {
                        println("Successfully built plugin-FlogHelper")
                    } else {
                        println("Failed to build plugin-FlogHelper")
                    }
                }
            } finally {
                // Restore original settings
                originalSettings.delete()
                if (hasOriginalSettings) {
                    tempSettings.copyTo(originalSettings)
                    tempSettings.delete()
                }
            }
            
        } finally {
            // Restore original build.gradle if it was patched
            if (wasPatched) {
                buildGradleFile.writeText(originalContent)
                println("  Restored original build.gradle")
            }
        }
    }
}