/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.decks

import at.woolph.caco.cli.DeckList
import at.woolph.caco.datamodel.decks.Format
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList

class DeckDiff() {
    suspend fun diffCommanderDecks(userName: String) =
        coroutineScope {
            val archidektDecks =
                async {
                    ArchidektDeckImporter().importDecks(userName).filter { it.format == Format.Commander }.toList()
                }

            DeckboxDeckImporter()
                .importDeckboxDecks(userName)
                .take(20)
                .filter { it.format == Format.Commander }
                .mapNotNull { deck ->
                    archidektDecks.await().find { it.commandZone == deck.commandZone }?.let { Pair(deck, it) }
                }.map { (deck1, deck2) ->
                  Triple(deck1, deck2, diffDeck(deck1, deck2))
                }
        }

    fun diffDeck(
        deck1: DeckList,
        deck2: DeckList,
    ): List<Triple<String, Int?, Int?>> = buildList {
      deck1.mainboard.filter { it.key !in deck2.mainboard.keys }.forEach { (card, amount1) ->
        add(Triple(card, amount1, null))
      }
      deck2.mainboard.filter { it.key !in deck1.mainboard.keys }.forEach { (card, amount2) ->
        add(Triple(card, null, amount2))
      }
      deck1.mainboard
        .mapNotNull { deck2.mainboard[it.key]?.let { deck2Value -> Triple(it.key, it.value, deck2Value) } }
        .filter { (_, amount1, amount2) -> amount1 != amount2 }
        .forEach { (card, amount1, amount2) ->
          add(Triple(card, amount1, amount2))
        }
    }
}
