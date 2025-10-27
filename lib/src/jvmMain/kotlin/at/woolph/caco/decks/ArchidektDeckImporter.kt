/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.decks

import at.woolph.utils.ProgressTracker
import at.woolph.caco.cli.DeckList
import at.woolph.caco.datamodel.decks.DeckZone
import at.woolph.caco.datamodel.decks.Format
import kotlinx.coroutines.flow.*

class ArchidektDeckImporter(
    val progress: ProgressTracker<String, Int>? = null,
) {
  suspend fun importDecks(userName: String) =
      getListOfDeckLinks(userName).map { deck -> importDeck(deck) }

  suspend fun getListOfDeckLinks(userName: String): Flow<Deck> = flow {
    progress?.updateContext("fetching list of decks from $userName")

    emitAll(
        paginatedDataRequest<ArchidektPaginatedData<Deck>, Deck>(
                "https://archidekt.com/api/decks/?owner=$userName"
            )
            .filter { !it.theorycrafted }
            .onEach { progress?.advance(1) }
    )
  }

  suspend fun importDeck(deck: Deck): DeckList = importDeck(deck.id)

  suspend fun importDeck(deckId: Int): DeckList {
    progress?.updateContext("fetching deck from https://archidekt.com/api/decks/${deckId}/")
    val archidektDecklist = request<ArchidektDecklist>("https://archidekt.com/api/decks/${deckId}/")

    val format =
        when (archidektDecklist.deckFormat) {
          3 -> Format.Commander
          else -> Format.Unknown
        }

    val commanders =
        archidektDecklist.cards
            .filter { it.categories.contains("Commander") }
            .associate { it.card.oracleCard.name to it.quantity }
    val mainboard =
        archidektDecklist.cards
            .filter { it.categories.none { it in setOf("Commander", "Maybeboard", "Sideboard") } }
            .associate { it.card.oracleCard.name to it.quantity }
    val sideboard =
        archidektDecklist.cards
            .filter { it.categories.contains("Sideboard") }
            .associate { it.card.oracleCard.name to it.quantity }
    val maybeboard =
        archidektDecklist.cards
            .filter { it.categories.contains("Maybeboard") }
            .associate { it.card.oracleCard.name to it.quantity }

    return when (format) {
      Format.Commander,
      Format.Oathbreaker,
      Format.Brawl ->
          DeckList(
              archidektDecklist.name,
              format,
              deckZones = mapOf(
                DeckZone.COMMAND_ZONE to commanders,
                DeckZone.MAINBOARD to mainboard,
                DeckZone.MAYBE_MAINBOARD to maybeboard,
              ),
          )
      else ->
          DeckList(
              archidektDecklist.name,
              format,
              deckZones = mapOf(
                DeckZone.COMMAND_ZONE to commanders,
                DeckZone.SIDEBOARD to sideboard,
                DeckZone.MAYBE_MAINBOARD to maybeboard,
              ),
          )
    }
  }
}
