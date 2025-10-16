/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.decks

import at.woolph.caco.cli.DeckBuildingListPrinter
import at.woolph.caco.cli.DeckList
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.isDirectory

interface DecklistPrinter {
  suspend fun print(decks: Collection<DeckList>)

  class Pdf(
      val output: Path,
  ) : DecklistPrinter {
    val decklistPrinter = DeckBuildingListPrinter()

    override suspend fun print(decks: Collection<DeckList>) {
      decks.forEach { deck ->
        decklistPrinter.printList(
            listOf(deck),
            (if (output.isDirectory()) {
                  output
                      .resolve(
                          deck.format.name,
                      )
                      .resolve("${deck.name.replace(Regex("[\\[|\\]*\"]"),"")}.pdf")
                } else {
                  output
                })
                .createParentDirectories(),
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
