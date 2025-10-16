package at.woolph.lib.clikt

import at.woolph.caco.cli.DeckList
import at.woolph.caco.decks.DecklistPrinter

class TerminalDecklistPrinter(
        val terminal: com.github.ajalt.mordant.terminal.Terminal,
) : DecklistPrinter {
    override suspend fun print(decks: Collection<DeckList>) {
        decks.forEach { deck ->
            terminal.println("DeckName: ${deck.name}")
            terminal.println("Format: ${deck.format}")
            sequenceOf(
                "Commander:" to deck.commandZone,
                "Mainboard:" to deck.mainboard,
                "Sideboard:" to deck.sideboard,
                "Maybeboard:" to deck.maybeboard,
            ).filter { it.second.isNotEmpty() }
                .forEach { (sectionName, sectionCards) ->
                    terminal.println(sectionName)
                    sectionCards.forEach { (name, count) ->
                        terminal.println("$count $name")
                    }
                }
        }
    }
}