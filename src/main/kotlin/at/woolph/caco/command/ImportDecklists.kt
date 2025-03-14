package at.woolph.caco.command

import at.woolph.caco.decks.DeckboxDeckImporter
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt

class ImportDecklists : SuspendingCliktCommand(name = "deckbox-decks") {
  val username by option(help = "Deckbox username").prompt("Enter the username of the deckbox user")

  override suspend fun run() {
    DeckboxDeckImporter().importDeckboxDecks(username).collect {
      // TODO into database
    }
  }
}