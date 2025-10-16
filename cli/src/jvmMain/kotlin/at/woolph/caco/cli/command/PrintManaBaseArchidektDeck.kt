/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.command

import at.woolph.caco.cli.manabase.ColorIdentity
import at.woolph.caco.cli.manabase.SelectionCriterion
import at.woolph.caco.cli.manabase.generateManabase
import at.woolph.caco.cli.manabase.toDecklistEntries
import at.woolph.caco.cli.manabase.toDecklistEntryCards
import at.woolph.caco.decks.ArchidektDeckImporter
import at.woolph.lib.clikt.ProgressTrackerWrapper
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.widgets.progress.completed
import com.github.ajalt.mordant.widgets.progress.percentage
import com.github.ajalt.mordant.widgets.progress.progressBar
import com.github.ajalt.mordant.widgets.progress.progressBarContextLayout
import com.github.ajalt.mordant.widgets.progress.text
import com.github.ajalt.mordant.widgets.progress.timeRemaining
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class PrintManaBaseArchidektDeck : SuspendingCliktCommand(name = "archidekt-manabase") {
  val deckId by option(help = "ID of the deck").int().required()

  val minBasicLandCount by option(help = "minBasicLandCount").int()
  val basicLandTypeFactors by option(help = "basicLandTypeFactors").double().default(0.5)
  val fastStartFactor by option(help = "fastStartFactor").double().default(0.8)
  val maxPricePerCard by option(help = "maxPricePerCard").double().default(Double.MAX_VALUE)

  override suspend fun run() = coroutineScope {
    val progress =
        progressBarContextLayout<String> {
              percentage()
              progressBar()
              completed(style = terminal.theme.success)
              timeRemaining(style = TextColors.magenta)
              text { "$context" }
            }
            .animateInCoroutine(terminal, context = "")

    val job = launch { progress.execute() }
    val deckList = ArchidektDeckImporter(ProgressTrackerWrapper(progress)).importDeck(deckId)
    job.cancel("everything is done")
    println()

    newSuspendedTransaction {
      val decklistCommandZone = deckList.commandZone.toDecklistEntries().toDecklistEntryCards()
      val decklistMainboard = deckList.mainboard.toDecklistEntries().toDecklistEntryCards()

      val colorIdentity =
          decklistCommandZone.fold(ColorIdentity.COLORLESS) { acc, entry ->
            acc + entry.card.colorIdentity
          }

      val selectionCriterion =
          SelectionCriterion(
              colorIdentity,
              minBasicLandCount = minBasicLandCount ?: colorIdentity.colorIdentity.size,
              basicLandTypeFactors = basicLandTypeFactors,
              fastStartFactor = fastStartFactor,
              maxPricePerCard = maxPricePerCard,
          )

      generateManabase(
              selectionCriterion,
              decklistCommandZone + decklistMainboard,
          )
          .forEach { println(it) }
    }
  }
}
