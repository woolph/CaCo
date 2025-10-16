/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli

import at.woolph.caco.datamodel.decks.Format

data class DeckList(
    val name: String,
    val format: Format,
    val mainboard: Map<String, Int>,
    val commandZone: Map<String, Int> = emptyMap(),
    val sideboard: Map<String, Int> = emptyMap(),
    val maybeboard: Map<String, Int> = emptyMap(),
)
