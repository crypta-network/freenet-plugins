package plugins

import java.io.File

object FreereaderPlugin {
    fun buildFreereader(pluginDir: File, buildDir: File, db4oJar: File, executeCommand: (List<String>, File?) -> Int) {
        println("Building Ant plugin: plugin-Freereader")
        
        val buildXml = File(pluginDir, "build.xml")
        val tempBuildXml = File(buildDir, "temp-build-files/plugin-Freereader-build.xml")
        tempBuildXml.parentFile.mkdirs()
        
        // Create a modified build.xml that uses Java 8
        val content = buildXml.readText()
            .replace("source=\"1.6\"", "source=\"8\"")
            .replace("target=\"1.6\"", "target=\"8\"")
        tempBuildXml.writeText(content)
        
        val antCommand = mutableListOf(
            "ant", "-f", tempBuildXml.absolutePath,
            "-Dbasedir=${pluginDir.absolutePath}",
            "clean", "main",
            "-Dant.file.failonerror=false"
        )
        
        // Add db4o if needed
        if (db4oJar.exists()) {
            antCommand.addAll(listOf("-lib", db4oJar.absolutePath))
        }
        
        if (executeCommand(antCommand, pluginDir) == 0) {
            println("Successfully built plugin-Freereader")
        }
    }
}