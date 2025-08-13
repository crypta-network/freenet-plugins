package plugins

import java.io.File

object JSTUNPlugin {
    fun buildJSTUN(pluginDir: File, wrapperJar: File, executeCommand: (List<String>, File?) -> Int) {
        println("Building Ant plugin: plugin-JSTUN")
        
        // Standard Ant plugin build with wrapper.jar on classpath
        val antCommand = mutableListOf(
            "ant", "clean", "dist",
            "-Dsource-version=8",
            "-Dtarget-version=8",
            "-Dant.file.failonerror=false"
        )
        
        // Add wrapper.jar to classpath if available
        if (wrapperJar.exists()) {
            antCommand.addAll(listOf("-lib", wrapperJar.absolutePath))
        }
        
        if (executeCommand(antCommand, pluginDir) == 0) {
            println("Successfully built plugin-JSTUN")
        }
    }
}