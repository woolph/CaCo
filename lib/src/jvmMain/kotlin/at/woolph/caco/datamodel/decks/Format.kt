/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.decks

import at.woolph.caco.cli.DeckZone

enum class Format(
  val shortName: String,
  val zones: Set<DeckZone> = setOf(DeckZone.MAIN_BOARD, DeckZone.SIDE_BOARD),
  val isSingleton: Boolean = false,
) {
  Unknown("???", zones = DeckZone.entries.toSet()),
  Standard("STD"),
  Historic("HSC"),
  Modern("MDN"),
  Pauper("PPR"),
  Legacy("LGC"),
  Vintage("VTG"),
  Pioneer("PNR"),
  Commander("CMD", zones = setOf(DeckZone.MAIN_BOARD, DeckZone.COMMAND_ZONE), isSingleton = true),
  PauperCommander("PCMD", zones = setOf(DeckZone.MAIN_BOARD, DeckZone.COMMAND_ZONE), isSingleton = true),
  Oathbreaker("OBR", zones = setOf(DeckZone.MAIN_BOARD, DeckZone.COMMAND_ZONE), isSingleton = true),
  CanadianHighlander("CHL", zones = setOf(DeckZone.MAIN_BOARD), isSingleton = true),
  Brawl("BRL", zones = setOf(DeckZone.MAIN_BOARD, DeckZone.COMMAND_ZONE), isSingleton = true),
  Cube("CUB", zones = setOf(DeckZone.MAIN_BOARD)),
  BattleBox("BBX", zones = setOf(DeckZone.MAIN_BOARD)),
}
