import java.io.File
import java.nio.file.Files
import java.nio.file.Path

object BuildUtils {
    fun executeCommand(
        command: List<String>,
        workingDir: File? = null,
        printOutput: Boolean = false,
        printErrors: Boolean = true
    ): Int {
        return try {
            val processBuilder = ProcessBuilder(command).apply {
                workingDir?.let { directory(it) }
                redirectErrorStream(true)
            }
            
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            when {
                exitCode != 0 && printErrors -> {
                    println("Command failed (exit code: $exitCode): ${command.joinToString(" ")}")
                    if (System.getProperty("gradle.verbose", "false") == "true" || printOutput) {
                        println(output)
                    }
                }
                printOutput -> println(output)
            }
            
            exitCode
        } catch (e: Exception) {
            println("Error executing command: ${e.message}")
            -1
        }
    }

    fun createSymlink(target: Path, link: Path): Boolean {
        return try {
            link.parent?.toFile()?.mkdirs()
            if (Files.exists(link)) {
                if (Files.isSymbolicLink(link)) {
                    Files.delete(link)
                } else {
                    link.toFile().deleteRecursively()
                }
            }
            Files.createSymbolicLink(link, target)
            true
        } catch (e: Exception) {
            println("Failed to create symlink from $link to $target: ${e.message}")
            false
        }
    }

    fun downloadFile(url: String, targetFile: File): Boolean {
        return try {
            if (!targetFile.exists()) {
                println("Downloading ${targetFile.name}...")
                targetFile.parentFile.mkdirs()
                java.net.URI(url).toURL().openStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                println("Downloaded ${targetFile.name}")
            }
            true
        } catch (e: Exception) {
            println("Failed to download ${targetFile.name}: ${e.message}")
            false
        }
    }

    fun patchJavaVersion(content: String, isGradle: Boolean): String {
        return if (isGradle) {
            content
                .replace(Regex("sourceCompatibility\\s*=\\s*1\\.7"), "sourceCompatibility = 1.8")
                .replace(Regex("targetCompatibility\\s*=\\s*1\\.7"), "targetCompatibility = 1.8")
                .replace(Regex("sourceCompatibility\\s*=\\s*7"), "sourceCompatibility = 8")
                .replace(Regex("targetCompatibility\\s*=\\s*7"), "targetCompatibility = 8")
                .replace("JavaVersion.VERSION_1_7", "JavaVersion.VERSION_1_8")
                .replace("sourceCompatibility = targetCompatibility = 7", "sourceCompatibility = targetCompatibility = 8")
                .replace("\"-Djavac.source.version=\" + sourceCompatibility", "\"-Djavac.source.version=8\"")
                .replace("\"-Djavac.target.version=\" + targetCompatibility", "\"-Djavac.target.version=8\"")
                .replace("archiveBaseName = ", "baseName = ")
                .replace("destinationDirectory = ", "destinationDir = ")
        } else {
            content
                .replace(Regex("source=\"1\\.5\""), "source=\"8\"")
                .replace(Regex("target=\"1\\.5\""), "target=\"8\"")
                .replace(Regex("source-version.*value=\"1\\.5\""), "source-version\" value=\"8\"")
                .replace(Regex("\\-Djavac\\.source\\.version=\"?1\\.5\"?"), "-Djavac.source.version=8")
                .replace(Regex("\\-Djavac\\.target\\.version=\"?1\\.5\"?"), "-Djavac.target.version=8")
        }
    }

    fun patchKeyUtilsBuildGradle(content: String): String {
        return content
            .replace(Regex("sourceCompatibility\\s*=\\s*1\\.7"), "sourceCompatibility = 1.8")
            .replace(Regex("targetCompatibility\\s*=\\s*1\\.7"), "targetCompatibility = 1.8")
            .replace(
                "compile group: 'org.freenetproject', name: 'fred', version: 'build+'",
                "compileOnly files('../fred/build/libs/freenet.jar')\n    compileOnly files('../fred/lib/freenet-ext.jar')"
            )
            .replace(
                "getMTime(\"src/main/java/plugins/KeyUtils/Version.java\")",
                "new Date()"
            )
    }
}