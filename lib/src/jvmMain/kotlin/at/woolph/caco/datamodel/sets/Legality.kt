/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.sets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Legality {
  Legal,
  @SerialName("not_legal") NotLegal,
  Restricted,
  Banned,
}
