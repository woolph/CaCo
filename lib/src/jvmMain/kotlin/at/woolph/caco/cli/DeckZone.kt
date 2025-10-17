package at.woolph.caco.cli

enum class DeckZone(
  val isPartOfDeck: Boolean = true,
  /** when this is true, the given DeckZone must contain cards in deck lists */
  val isMandatory: Boolean = isPartOfDeck,
) {
  COMMAND_ZONE,
  MAIN_BOARD,
  SIDE_BOARD(isMandatory = false),
  MAYBE_BOARD(isPartOfDeck = false),
}
