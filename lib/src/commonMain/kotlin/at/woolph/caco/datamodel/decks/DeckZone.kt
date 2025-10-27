package at.woolph.caco.datamodel.decks

enum class DeckZone(
  val isPartOfDeck: Boolean = true,
  val deckPart: DeckPart? = DeckPart.MAINBOARD,
  /** when this is true, the given DeckZone must contain cards in deck lists */
  val isMandatory: Boolean = isPartOfDeck,
) {
  COMMAND_ZONE,
  MAINBOARD,
  SIDEBOARD(isMandatory = false, deckPart = DeckPart.SIDEBOARD),
  COMPANION(isMandatory = false, deckPart = DeckPart.SIDEBOARD),
  MAYBE_MAINBOARD(isPartOfDeck = false),
  MAYBE_SIDEBOARD(isPartOfDeck = false, deckPart = DeckPart.SIDEBOARD),
  ;
}
