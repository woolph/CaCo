/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.command

import at.woolph.caco.cli.DeckListBuilder
import at.woolph.caco.datamodel.decks.DeckZone
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.datamodel.sets.Finish
import at.woolph.utils.Quadruple
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.terminal
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CheckDecklistMissingCards : SuspendingCliktCommand(name = "check-deck") {
  override suspend fun run() {

    terminal.println("Enter decklist:")

    var currentDeckZone: DeckZone? = DeckZone.MAINBOARD // null is metainfo section
    val deckList = generateSequence { terminal.readLineOrNull(false) }
      .takeWhile { it != "EOF" }
      .filter { it.isNotBlank() }
      .fold(DeckListBuilder()) { deckListBuilder, line ->
         when (line) {
          "About" -> currentDeckZone = null
          "Deck", "Mainboard", "Main" -> currentDeckZone = DeckZone.MAINBOARD
          "Sideboard" -> currentDeckZone = DeckZone.SIDEBOARD
          "Commander" -> currentDeckZone = DeckZone.COMMAND_ZONE
          "Maybeboard" -> currentDeckZone = DeckZone.MAYBE_SIDEBOARD
           else -> {
             if (currentDeckZone == null) { // metainfo section
               try {
                 val tokens = line.split(" ", limit = 2)
                 val propertyName = tokens[0]
                 val propertyValue = tokens[1]

                 when (propertyName) {
                   "Name" -> deckListBuilder.name = propertyValue
                   else -> println("decklist metainfo property \"$propertyName\" is unknown")
                 }
               } catch (t: Throwable) {
                 println("line \"line\" can't be parsed as decklist metainfo property")
                 t.printStackTrace()
               }
              } else {
                try {
                  val tokens = line.split(" ", limit = 2)
                  val amount = tokens[0].toInt()
                  val cardName = tokens[1]

                  deckListBuilder.add(currentDeckZone, cardName, amount)
                } catch (t: Throwable) {
                  println("line \"line\" can't be parsed as decklist entry")
                  t.printStackTrace()
                }
             }
           }
        }
        deckListBuilder
      }.build()

    val neededCards = newSuspendedTransaction {
      deckList.deckZones.filter { it.key.isPartOfDeck } .flatMap { (deckZone, cardList) ->
        cardList.mapNotNull { (cardName, amount) ->
          val cards = Card.find { Cards.name match cardName }
          val lowestPrice = cards.mapNotNull { card -> Finish.entries.mapNotNull { card.prices(it) }.minOrNull() }.minOrNull()
          val possessionAmount = cards.sumOf { it.possessions.count() }
          if (possessionAmount < amount) {
            Quadruple(deckZone, cardName, amount - possessionAmount, lowestPrice)
          } else {
            null
          }
        }
      }
    }

    println("Deck: ${deckList.name}")
    println("Total estimated costs for deck completion: ${neededCards.sumOf { (_,_,_,cost) -> cost?.value ?: 0.0 }}")
    neededCards.groupBy { it.t1 }.forEach { (deckZone, cardList) ->
      println("$deckZone")
      cardList.forEach { (_, cardName, amount, _) ->
        println("$amount $cardName")
      }
    }
  }
}
