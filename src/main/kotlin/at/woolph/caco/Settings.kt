package at.woolph.caco

import at.woolph.libs.files.config.ConfigurationFile
import at.woolph.libs.files.path
import java.nio.file.Path
import java.nio.file.Paths

object Settings: ConfigurationFile(path(System.getProperty("user.home"), "AppData", "Roaming", "CaCo.properties")) {
	var tempSVNCopy by config() { if(it.isNullOrBlank()) null else Paths.get(it) }
}
