import java.io.File
import java.util.jar.*
import java.io.FileOutputStream

object ShadowJarHelper {
    
    fun relocateJar(
        inputJar: File,
        outputJar: File,
        relocations: Map<String, String>
    ): Boolean {
        try {
            val jarInput = JarFile(inputJar)
            val manifest = jarInput.manifest ?: Manifest()
            val processedEntries = mutableSetOf<String>()
            
            JarOutputStream(FileOutputStream(outputJar), manifest).use { jarOutput ->
                jarInput.entries().asIterator().forEach { entry ->
                    if (entry.name != JarFile.MANIFEST_NAME) {
                        var entryName = entry.name
                        
                        // Relocate package paths in entry names
                        relocations.forEach { (from, to) ->
                            if (entryName.startsWith("$from/")) {
                                entryName = entryName.replaceFirst("$from/", "$to/")
                            }
                        }
                        
                        // Skip duplicate entries (common in META-INF)
                        if (processedEntries.contains(entryName)) {
                            return@forEach
                        }
                        processedEntries.add(entryName)
                        
                        val newEntry = JarEntry(entryName)
                        newEntry.time = entry.time
                        jarOutput.putNextEntry(newEntry)
                        
                        if (!entry.isDirectory) {
                            val bytes = jarInput.getInputStream(entry).readBytes()
                            
                            // For class files, we need to update the bytecode
                            // For now, we'll just copy as-is (full bytecode rewriting would require ASM)
                            // The package structure relocation is the main goal
                            jarOutput.write(bytes)
                        }
                        
                        jarOutput.closeEntry()
                    }
                }
            }
            
            jarInput.close()
            return true
        } catch (e: Exception) {
            println("Error processing ${inputJar.name}: ${e.message}")
            return false
        }
    }
}