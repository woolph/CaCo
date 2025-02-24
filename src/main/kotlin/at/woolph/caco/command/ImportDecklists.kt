package at.woolph.caco.command

import at.woolph.caco.importer.deck.DeckboxDeckImporter
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import kotlinx.coroutines.runBlocking

class ImportDecklists: CliktCommand(name = "deckbox-decks") {
    val username by option(help="Deckbox username").prompt("Enter the username of the deckbox user")

    override fun run() = runBlocking<Unit> {
      DeckboxDeckImporter().importDeckboxDecks(username).collect {
        // TODO into database
      }
    }
}