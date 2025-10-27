package at.woolph.caco.datamodel.decks

enum class Format(
  val shortName: String,
  val zones: Set<DeckZone> = setOf(DeckZone.MAINBOARD, DeckZone.SIDEBOARD),
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
  Commander("CMD", zones = setOf(DeckZone.MAINBOARD, DeckZone.COMMAND_ZONE), isSingleton = true),
  PauperCommander("PCMD", zones = setOf(DeckZone.MAINBOARD, DeckZone.COMMAND_ZONE), isSingleton = true),
  Oathbreaker("OBR", zones = setOf(DeckZone.MAINBOARD, DeckZone.COMMAND_ZONE), isSingleton = true),
  CanadianHighlander("CHL", zones = setOf(DeckZone.MAINBOARD), isSingleton = true),
  Brawl("BRL", zones = setOf(DeckZone.MAINBOARD, DeckZone.COMMAND_ZONE), isSingleton = true),
  Cube("CUB", zones = setOf(DeckZone.MAINBOARD)),
  BattleBox("BBX", zones = setOf(DeckZone.MAINBOARD)),
}