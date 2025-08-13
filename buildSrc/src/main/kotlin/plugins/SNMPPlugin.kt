package plugins

import java.io.File

object SNMPPlugin {
    fun buildSNMP(pluginDir: File, buildDir: File, executeCommand: (List<String>, File?) -> Int) {
        println("Building Ant plugin: plugin-SNMP")
        
        val dataStatsFile = File(pluginDir, "src/plugins/SNMP/snmplib/DataStatisticsInfo.java")
        val snmpStarterFile = File(pluginDir, "src/plugins/SNMP/snmplib/SNMPStarter.java")
        
        val tempSourceDir = File(buildDir, "temp-build-files/plugin-SNMP/src/plugins/SNMP/snmplib")
        tempSourceDir.mkdirs()
        
        // Backup original files
        val dataStatsBackup = File(pluginDir, "src/plugins/SNMP/snmplib/DataStatisticsInfo.java.backup")
        val snmpStarterBackup = File(pluginDir, "src/plugins/SNMP/snmplib/SNMPStarter.java.backup")
        dataStatsFile.copyTo(dataStatsBackup, overwrite = true)
        snmpStarterFile.copyTo(snmpStarterBackup, overwrite = true)
        
        try {
            // Patch DataStatisticsInfo.java - replace getTotalStatistics() with compatible code
            val dataStatsContent = dataStatsFile.readText()
                .replace(
                    "int stats[][] = collector.getTotalStatistics();\n\t\tfor (int i = 0 ; i < blocks ; i++)\n\t\t\tres += stats[i][in?1:0];",
                    "// getTotalStatistics() no longer exists, use getTotalIO() instead\n\t\tlong[] io = collector.getTotalIO();\n\t\tres = (int)(io[in?1:0] / Math.max(1, blocks)); // Approximate per-block average"
                )
            dataStatsFile.writeText(dataStatsContent)
            
            // Patch SNMPStarter.java - replace STATISTICS_ENTRIES with fixed value
            val snmpStarterContent = snmpStarterFile.readText()
                .replace(
                    "for (int i = 0 ; i < IOStatisticCollector.STATISTICS_ENTRIES ; i++) {",
                    "// STATISTICS_ENTRIES no longer exists, use fixed value for basic I/O stats\n\t\tfor (int i = 0 ; i < 2 ; i++) { // 0=total, 1=basic stats"
                )
            snmpStarterFile.writeText(snmpStarterContent)
            
            // Standard Ant plugin build
            val antCommand = mutableListOf(
                "ant", "clean", "dist",
                "-Dsource-version=8",
                "-Dtarget-version=8",
                "-Dant.file.failonerror=false"
            )
            
            if (executeCommand(antCommand, pluginDir) == 0) {
                println("Successfully built plugin-SNMP")
            }
        } finally {
            // Always restore original files
            dataStatsBackup.copyTo(dataStatsFile, overwrite = true)
            snmpStarterBackup.copyTo(snmpStarterFile, overwrite = true)
            dataStatsBackup.delete()
            snmpStarterBackup.delete()
        }
    }
}