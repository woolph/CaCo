package at.woolph.caco.cli

data class DecklistEntry(
    val cardName: String,
    val count: Int = 1,
) {
  override fun toString() = "$count ${cardName}"
}
