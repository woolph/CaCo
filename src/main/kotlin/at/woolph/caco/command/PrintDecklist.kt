package at.woolph.caco.command

import at.woolph.caco.decks.DecklistPrinter
import at.woolph.caco.importer.deck.DeckboxDeckImporter
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import java.net.URI

class PrintDecklist: CliktCommand(name = "deckbox-deck") {
    val url by option(help="Deckbox decklist URL").convert { URI.create(it).toURL() }.prompt("Enter URL")
    val output by option().path(canBeDir = true, canBeFile = true)

    override fun run() = runBlocking<Unit> {
      val decklistPrinter = output?.let {
        DecklistPrinter.Pdf(it)
      } ?: DecklistPrinter.Terminal(terminal)

      DeckboxDeckImporter().importDeck(url).let { decklistPrinter.print(listOf(it)) }
    }
}