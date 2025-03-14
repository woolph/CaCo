package at.woolph.caco.command

import at.woolph.caco.cli.manabase.ColorIdentity
import at.woolph.caco.cli.manabase.DecklistEntry
import at.woolph.caco.cli.manabase.SelectionCriterion
import at.woolph.caco.cli.manabase.generateManabase
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.double

class PrintManaBase : SuspendingCliktCommand(name = "generate-manabase") {
  val colorIdentity by option(help = "The color identity to generate the mana base for").convert {
    ColorIdentity.Companion(
      it
    )
  }.prompt("Enter colorIdentity:")
  val basicLandTypeFactors by option(help = "basicLandTypeFactors").double().default(0.1)
  val fastStartFactor by option(help = "fastStartFactor").double().default(1.2)
  val maxPricePerCard by option(help = "maxPricePerCard").double().default(Double.MAX_VALUE)

  override suspend fun run() {
    val selectionCriterion = SelectionCriterion(
      colorIdentity,
      basicLandTypeFactors = basicLandTypeFactors,
      fastStartFactor = fastStartFactor,
      maxPricePerCard = maxPricePerCard,
    )

    generateManabase(
      selectionCriterion,
      generateSequence { terminal.readLineOrNull(false) }.takeWhile { it.isNotBlank() }
        .map { DecklistEntry(it.removePrefix("1 ")) }.toList()
    ).forEach { println(it) }
  }
}
