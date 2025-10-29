/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.command

import at.woolph.caco.binderlabels.determineBinderLabels
import at.woolph.caco.binderlabels.printBinderLabel
import at.woolph.lib.clikt.SuspendingTransactionCliktCommand
import at.woolph.lib.clikt.prompt
import at.woolph.utils.io.toKotlinxPath
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path

class PrintCollectionBinderLabels : SuspendingTransactionCliktCommand(name = "binder-labels") {
  val output by option("--output", "-o").path(canBeDir = false, canBeFile = true).required()
  val sets by
      argument(help = "The set code of the cards to be entered")
          .multiple()
          .prompt("Enter the set codes to be imported/updated")

  override suspend fun runTransaction() {
    val labels = determineBinderLabels(
      thresholdTooMuchPages = 45, // TODO parameterize and fine tune defaults the max card count per page, so that the labels are not too small
      thresholdTooFewPages = 7,
    )

    printBinderLabel(
        file = output.toKotlinxPath(),
        labels = labels.toList(),
        labelsPerPage = 5, // TODO parameterize
      )
  }
}
