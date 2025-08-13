package plugins

import java.io.File

object LibraryPlugin {
    fun buildLibrary(pluginDir: File, buildDir: File, executeCommand: (List<String>, File?) -> Int) {
        println("Building Ant plugin: plugin-Library")
        
        val sourceFile = File(pluginDir, "src/plugins/Library/util/SkeletonBTreeMap.java")
        val tempSourceDir = File(buildDir, "temp-build-files/plugin-Library/src/plugins/Library/util")
        tempSourceDir.mkdirs()
        val tempSourceFile = File(tempSourceDir, "SkeletonBTreeMap.java")
        
        // Create a patched version with correct generic types
        val content = sourceFile.readText()
            .replace("Map<PullTask<SkeletonNode>, ProgressTracker<SkeletonNode, ?>> ids = null;", 
                    "Map<PullTask<SkeletonNode>, ProgressTracker<SkeletonNode, ? extends Progress>> ids = null;")
            .replace("ProgressTracker<SkeletonNode, ?> ntracker = null;;", 
                    "ProgressTracker<SkeletonNode, ? extends Progress> ntracker = null;")
            .replace("ids = new LinkedHashMap<PullTask<SkeletonNode>, ProgressTracker<SkeletonNode, ?>>();",
                    "ids = new LinkedHashMap<PullTask<SkeletonNode>, ProgressTracker<SkeletonNode, ? extends Progress>>();")
            .replace("import plugins.Library.util.exec.TaskCompleteException;",
                    "import plugins.Library.util.exec.TaskCompleteException;\nimport plugins.Library.util.exec.Progress;")
        tempSourceFile.writeText(content)
        
        // Copy the patched file to the plugin's source directory temporarily
        val originalBackup = File(pluginDir, "src/plugins/Library/util/SkeletonBTreeMap.java.backup")
        sourceFile.copyTo(originalBackup, overwrite = true)
        tempSourceFile.copyTo(sourceFile, overwrite = true)
        
        try {
            // Standard Ant plugin build
            val antCommand = mutableListOf(
                "ant", "clean", "dist",
                "-Dsource-version=8",
                "-Dtarget-version=8",
                "-Dant.file.failonerror=false"
            )
            
            if (executeCommand(antCommand, pluginDir) == 0) {
                println("Successfully built plugin-Library")
            }
        } finally {
            // Always restore the original file
            originalBackup.copyTo(sourceFile, overwrite = true)
            originalBackup.delete()
        }
    }
}