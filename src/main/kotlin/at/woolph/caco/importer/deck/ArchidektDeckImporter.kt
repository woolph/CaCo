package at.woolph.caco.importer.deck

import at.woolph.caco.cli.DeckList
import at.woolph.caco.datamodel.decks.Format
import com.github.ajalt.mordant.animation.coroutines.CoroutineProgressTaskAnimator
import com.github.ajalt.mordant.animation.progress.advance
import kotlinx.coroutines.flow.*

class ArchidektDeckImporter(
    val progress: CoroutineProgressTaskAnimator<String>? = null,
) {
    suspend fun importDecks(userName: String) =
        getListOfDeckLinks(userName).map { deck ->
            importDeck(deck)
        }

    suspend fun getListOfDeckLinks(userName: String): Flow<Deck> = flow {
        progress?.update { context = "fetching list of decks from $userName" }

        emitAll(paginatedDataRequest<ArchidektPaginatedData<Deck>, Deck>("https://archidekt.com/api/decks/?owner=$userName")
            .filter { !it.theorycrafted }
            .onEach {
                progress?.advance(1)
            })
    }

    suspend fun importDeck(deck: Deck): DeckList {
        progress?.update { context = "fetching deck from https://archidekt.com/api/decks/${deck.id}/" }
        val archidektDecklist = request<ArchidektDecklist>("https://archidekt.com/api/decks/${deck.id}/")

        val format = when(deck.deckFormat) {
            3 -> Format.Commander
            else -> Format.Unknown
        }

        val commanders = archidektDecklist.cards.filter { it.categories.contains("Commander") }.associate {
            it.card.oracleCard.name to it.quantity
        }
        val mainboard = archidektDecklist.cards.filter { it.categories.none { it in setOf("Commander", "Maybeboard", "Sideboard")} }.associate {
            it.card.oracleCard.name to it.quantity
        }
        val sideboard = archidektDecklist.cards.filter { it.categories.contains("Sideboard") }.associate {
            it.card.oracleCard.name to it.quantity
        }
        val maybeboard = archidektDecklist.cards.filter { it.categories.contains("Maybeboard") }.associate {
            it.card.oracleCard.name to it.quantity
        }

        return when(format) {
            Format.Commander, Format.Oathbreaker, Format.Brawl ->
                DeckList(
                    deck.name,
                    format,
                    mainboard,
                    commandZone = commanders,
                    maybeboard = maybeboard,
                )
            else ->
                DeckList(
                    deck.name,
                    format,
                    mainboard,
                    sideboard = sideboard,
                    maybeboard = maybeboard,
                )
        }
    }
}
