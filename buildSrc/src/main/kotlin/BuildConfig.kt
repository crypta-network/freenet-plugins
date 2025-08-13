object BuildConfig {
    const val JAVA_VERSION = "8"
    const val GRADLE_WRAPPER_VERSION = "4.10.3"
    const val DB4O_JAR_NAME = "db4o-7.4.jar"
    
    val PLUGINS_NEEDING_WRAPPER = listOf("plugin-WebOfTrust", "plugin-Freetalk")
    val PLUGINS_NEEDING_JAVA_PATCH_ONLY = listOf("plugin-FlogHelper", "plugin-KeyUtils", "plugin-KeepAlive")
    val ANT_PLUGINS_NEEDING_DB4O = listOf("plugin-XMLLibrarian", "plugin-XMLSpider")
    val ANT_PLUGINS_NEEDING_DB4O_JAR_ONLY = listOf("plugin-Freereader")
    val GRADLE_PLUGINS_NEEDING_DB4O = listOf("plugin-WebOfTrust", "plugin-Freetalk")
    val ALL_PLUGINS_NEEDING_DB4O = ANT_PLUGINS_NEEDING_DB4O + GRADLE_PLUGINS_NEEDING_DB4O
    
    val EXTERNAL_DEPENDENCIES = mapOf(
        "snakeyaml-1.5.jar" to "https://repo1.maven.org/maven2/org/yaml/snakeyaml/1.5/snakeyaml-1.5.jar",
        "xom-1.3.8.jar" to "https://repo1.maven.org/maven2/xom/xom/1.3.8/xom-1.3.8.jar",
        "bcprov-jdk15on-1.70.jar" to "https://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk15on/1.70/bcprov-jdk15on-1.70.jar",
        "wrapper-delta-pack-3.6.2.tar.gz" to "https://download.tanukisoftware.com/wrapper/3.6.2/wrapper-delta-pack-3.6.2.tar.gz"
    )
}