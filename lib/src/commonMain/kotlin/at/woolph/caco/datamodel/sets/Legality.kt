/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.sets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Legality(
  val isAllowedToBePlayed: Boolean,
) {
  Legal(true),
  @SerialName("not_legal") NotLegal(false),
  Restricted(true),
  Banned(false),
}
