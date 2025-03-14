package at.woolph.caco.command

import at.woolph.caco.decks.DecklistPrinter
import at.woolph.caco.decks.DeckboxDeckImporter
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.path
import java.net.URI

class PrintDecklist: SuspendingCliktCommand(name = "deckbox-deck") {
    val url by option(help="Deckbox decklist URL").convert { URI.create(it).toURL() }.prompt("Enter URL")
    val output by option().path(canBeDir = true, canBeFile = true)

    override suspend fun run() {
      val decklistPrinter = output?.let {
        DecklistPrinter.Pdf(it)
      } ?: DecklistPrinter.Terminal(terminal)

      DeckboxDeckImporter().importDeck(url).let { decklistPrinter.print(listOf(it)) }
    }
}