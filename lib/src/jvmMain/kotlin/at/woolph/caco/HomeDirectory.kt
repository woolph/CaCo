package at.woolph.caco

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

data class HomeDirectory(val path: Path) {
  constructor(): this(Path(System.getenv("${ENVVAR_PREFIX}_HOME") ?: "${System.getProperty("user.home")}/.caco"))
  init {  path.createDirectories() }
  fun resolve(vararg sub: String): Path = sub.fold(path, Path::resolve)

  override fun toString(): String = path.toString()

  companion object {

    val ENVVAR_PREFIX: String = "CACO"
  }
}