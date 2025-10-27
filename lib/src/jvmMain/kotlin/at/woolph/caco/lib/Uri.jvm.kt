package at.woolph.caco.lib

import java.net.URI

actual class Uri actual constructor(value: String) {
  private val uri = URI(value)

  fun toURL() = uri.toURL()

  override fun toString(): String = uri.toString()
}
