package at.woolph.caco.importer.deck

import at.woolph.caco.cli.DeckList
import at.woolph.caco.datamodel.decks.Format
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList

class DeckDiff(
    val terminal: Terminal,
) {
    suspend fun diffCommanderDecks() = coroutineScope {
        val archidektDecks = async {
            ArchidektDeckImporter().importDecks("woolph").filter { it.format == Format.Commander }.toList()
        }

        DeckboxDeckImporter().importDeckboxDecks("woolph").take(20).filter { it.format == Format.Commander }.mapNotNull { deck ->
            archidektDecks.await().find { it.commandZone == deck.commandZone }?.let { Pair(deck, it) }
        }.collect { (deck1, deck2) ->
            terminal.println("Diffing decks \"${deck1.commandZone.keys.joinToString(" & ") { it }}\"")
            diffDeck(deck1, deck2)
        }
    }

    fun diffDeck(deck1: DeckList, deck2: DeckList) {
        deck1.mainboard.filter { it.key !in deck2.mainboard.keys }.forEach {
            terminal.println("Card ${it} is in deck1 but not in deck2")
        }
        deck2.mainboard.filter { it.key !in deck1.mainboard.keys }.forEach {
            terminal.println("Card ${it} is in deck2 but not in deck1")
        }
        deck1.mainboard
            .mapNotNull { deck2.mainboard[it.key]?.let { deck2Value -> Triple(it.key, it.value, deck2Value) } }
            .filter{ (_, value1, value2) -> value1 != value2 }
            .forEach { (card, value1, value2) ->
                terminal.println("Card ${card} is $value1 times in deck1 but $value2 times in deck2")
            }
    }
}

suspend fun main() {
    DeckDiff(Terminal()).diffCommanderDecks()
}