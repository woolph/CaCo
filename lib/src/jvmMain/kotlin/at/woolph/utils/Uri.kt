package at.woolph.utils

import kotlinx.serialization.Serializable
import java.net.URI

@Serializable(with = UriSerializer::class)
actual class Uri actual constructor(value: String) {
  private val uri = URI(value)

  fun toURL() = uri.toURL()

  override fun toString(): String = uri.toString()
}
