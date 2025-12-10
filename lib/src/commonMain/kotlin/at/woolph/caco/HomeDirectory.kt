/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

val DEFAULT_HOME_DIRECTORY: HomeDirectory = HomeDirectory()

data class HomeDirectory(val path: Path) {
  constructor() :
      this(
          Path(System.getenv("${ENVVAR_PREFIX}_HOME") ?: "${System.getProperty("user.home")}/.caco")
      )

  init {
    SystemFileSystem.createDirectories(path)
  }

  fun resolve(vararg sub: String): Path = Path(path, *sub)

  override fun toString(): String = path.toString()

  companion object {
    val ENVVAR_PREFIX: String = "CACO"
  }
}
