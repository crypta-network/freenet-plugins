package plugins

import java.io.File

object FreemailPlugin {
    
    fun buildFreemail(
        pluginDir: File,
        executeCommand: (List<String>, File?) -> Int
    ): Boolean {
        println("Building Gradle plugin: plugin-Freemail")
        
        // Apply WebPage.java patch for template loader path
        val webPageFile = File(pluginDir, "src/main/java/org/freenetproject/freemail/ui/web/WebPage.java")
        var originalContent: String? = null
        
        if (webPageFile.exists()) {
            originalContent = webPageFile.readText()
            val patchedContent = originalContent.replace(
                """loader.setPrefix("/resources/templates/");""",
                """loader.setPrefix("templates/");"""
            )
            
            if (originalContent != patchedContent) {
                webPageFile.writeText(patchedContent)
                println("  Patched WebPage.java template loader path")
            }
        }
        
        try {
            // Build with gradlew
            val gradlewScript = File(pluginDir, "gradlew")
            val isWindows = System.getProperty("os.name").lowercase().contains("windows")
            val gradlewBat = File(pluginDir, "gradlew.bat")
            
            val gradleArgs = when {
                !isWindows && gradlewScript.exists() -> {
                    listOf("bash", gradlewScript.absolutePath, "-p", pluginDir.absolutePath, "clean", "jar")
                }
                isWindows && gradlewBat.exists() -> {
                    listOf("cmd", "/c", gradlewBat.absolutePath, "-p", pluginDir.absolutePath, "clean", "jar")
                }
                else -> {
                    println("Warning: No Gradle wrapper found for plugin-Freemail")
                    return false
                }
            }
            
            val exitCode = executeCommand(gradleArgs, pluginDir)
            
            if (exitCode == 0) {
                println("Successfully built plugin-Freemail")
                return true
            } else {
                println("Failed to build plugin-Freemail")
                return false
            }
        } finally {
            // Restore original content if patched
            if (originalContent != null && webPageFile.exists()) {
                webPageFile.writeText(originalContent)
                println("  Restored original WebPage.java")
            }
        }
    }
}