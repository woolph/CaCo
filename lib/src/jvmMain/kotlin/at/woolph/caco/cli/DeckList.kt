/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli

import at.woolph.caco.datamodel.decks.DeckZone
import at.woolph.caco.datamodel.decks.Format

data class DeckList(
    val name: String,
    val format: Format,
    val deckZones: Map<DeckZone, Map<String, Int>> = mutableMapOf(
      DeckZone.MAINBOARD to mutableMapOf()
    ),
) {
  val mainboard: Map<String, Int> get() = deckZones[DeckZone.MAINBOARD]!!
  val commandZone: Map<String, Int> get() = deckZones[DeckZone.COMMAND_ZONE] ?: emptyMap()
  val sideboard: Map<String, Int> get() = (deckZones[DeckZone.SIDEBOARD] ?: emptyMap()) + (deckZones[DeckZone.COMPANION] ?: emptyMap())
  val maybeMainboard: Map<String, Int> get() = deckZones[DeckZone.MAYBE_MAINBOARD] ?: emptyMap()
  val maybeSideboard: Map<String, Int> get() = deckZones[DeckZone.MAYBE_SIDEBOARD] ?: emptyMap()
}
