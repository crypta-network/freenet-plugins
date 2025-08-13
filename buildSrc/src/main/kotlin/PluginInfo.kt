import java.io.File

data class PluginInfo(
    val dir: File,
    val name: String,
    val isGradle: Boolean,
    val isAnt: Boolean
)