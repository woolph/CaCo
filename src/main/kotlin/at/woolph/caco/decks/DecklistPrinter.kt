/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.decks

import at.woolph.caco.cli.DeckBuildingListPrinter
import at.woolph.caco.cli.DeckList
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.isDirectory

sealed interface DecklistPrinter {
    suspend fun print(decks: Collection<DeckList>)

    class Terminal(
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

    class Pdf(
        val output: Path,
    ) : DecklistPrinter {
        val decklistPrinter = DeckBuildingListPrinter()

        override suspend fun print(decks: Collection<DeckList>) {
            decks.forEach { deck ->
                decklistPrinter.printList(
                    listOf(deck),
                    (
                        if (output.isDirectory()) {
                            output
                                .resolve(
                                    deck.format.name,
                                ).resolve("${deck.name.replace(Regex("[\\[|\\]*\"]"),"")}.pdf")
                        } else {
                            output
                        }
                    ).createParentDirectories(),
                )
            }
        }
    }

    class PdfOneFile(
        val output: Path,
    ) : DecklistPrinter {
        val decklistPrinter = DeckBuildingListPrinter()

        override suspend fun print(decks: Collection<DeckList>) {
            decklistPrinter.printList(
                decks,
                output.createParentDirectories(),
            )
        }
    }
}
