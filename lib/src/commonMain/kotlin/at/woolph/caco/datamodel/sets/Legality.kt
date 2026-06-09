/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.sets

import at.woolph.caco.datamodel.decks.Format
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
  ;

  fun deckLimit(format: Format) = when (this) {
    Legality.Legal -> format.deckLimit
    Legality.Restricted -> format.restrictedDeckLimit ?: throw IllegalArgumentException("the format $format does not have a restricted list and therefore no restricted deck limit")
    Legality.NotLegal, Legality.Banned -> 0
  }

}
