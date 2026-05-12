/* Copyright 2026 Wolfgang Mayer */
package at.woolph.utils

import java.net.URI
import kotlinx.serialization.Serializable

@Serializable(with = UriSerializer::class)
actual class Uri actual constructor(value: String) {
  private val uri = URI(value)

  fun toURL() = uri.toURL()

  override fun toString(): String = uri.toString()
}
