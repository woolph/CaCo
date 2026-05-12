/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.datamodel.decks

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Format(
    val shortName: String,
    val zones: Set<DeckZone> = setOf(DeckZone.MAINBOARD, DeckZone.SIDEBOARD),
    val isSingleton: Boolean = false,
) {
  Unknown("???", zones = DeckZone.entries.toSet()),
  Standard("STD"),
  Future("FTR"),
  Historic("HSC"),
  Timeless("TML"),
  Gladiator("GLD"),
  Pioneer("PNR"),
  Modern("MDN"),
  Legacy("LGC"),
  Pauper("PPR"),
  Vintage("VTG"),
  Penny("PNY"),
  Commander("EDH", zones = setOf(DeckZone.MAINBOARD, DeckZone.COMMAND_ZONE), isSingleton = true),
  Oathbreaker("OBR", zones = setOf(DeckZone.MAINBOARD, DeckZone.COMMAND_ZONE), isSingleton = true),
  StandardBrawl(
      "SBL",
      zones = setOf(DeckZone.MAINBOARD, DeckZone.COMMAND_ZONE),
      isSingleton = true,
  ),
  Brawl("BRL", zones = setOf(DeckZone.MAINBOARD, DeckZone.COMMAND_ZONE), isSingleton = true),
  Alchemy("ALC"),
  PauperCommander(
      "PCMD",
      zones = setOf(DeckZone.MAINBOARD, DeckZone.COMMAND_ZONE),
      isSingleton = true,
  ),
  @SerialName("duel") DuelCommander("DCMD"),
  Oldschool("OSCL"),
  Premodern("PMDN"),
  PrEDH("PEDH", zones = setOf(DeckZone.MAINBOARD, DeckZone.COMMAND_ZONE), isSingleton = true),
  CanadianHighlander("CHL", zones = setOf(DeckZone.MAINBOARD), isSingleton = true),
  Cube("CUB", zones = setOf(DeckZone.MAINBOARD)),
  BattleBox("BBX", zones = setOf(DeckZone.MAINBOARD)),
}
