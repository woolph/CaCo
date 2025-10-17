/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli

import at.woolph.caco.datamodel.decks.Format

data class DeckList(
    val name: String,
    val format: Format,
    val deckZones: Map<DeckZone, Map<String, Int>> = mutableMapOf(
      DeckZone.MAIN_BOARD to mutableMapOf()
    ),
) {
  val mainboard: Map<String, Int> get() = deckZones[DeckZone.MAIN_BOARD]!!
  val commandZone: Map<String, Int> get() = deckZones[DeckZone.COMMAND_ZONE] ?: emptyMap()
  val sideboard: Map<String, Int> get() = deckZones[DeckZone.SIDE_BOARD] ?: emptyMap()
  val maybeboard: Map<String, Int> get() = deckZones[DeckZone.MAYBE_BOARD] ?: emptyMap()
}
