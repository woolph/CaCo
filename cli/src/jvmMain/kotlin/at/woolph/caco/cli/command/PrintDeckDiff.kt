/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.command

import at.woolph.caco.decks.DeckDiff
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.table.table

class PrintDeckDiff : SuspendingCliktCommand(name = "archidekt-deckdiff") {
  val userName by option(help = "Archidekt").prompt("Enter URL")
  val output by option().path(canBeDir = true, canBeFile = true)

  override suspend fun run() {
    DeckDiff().diffCommanderDecks(userName).collect { (deck1, deck2, deckdiff) ->
      terminal.println(
          table {
            captionTop("Diffing decks \"${deck1.commandZone.keys.joinToString(" & ") { it }}\"")
            body {
              deckdiff.forEach { (card, amount1, amount2) ->
                row(card, amount1 ?: "", amount2 ?: "")
              }
            }
          },
      )
    }
  }
}
