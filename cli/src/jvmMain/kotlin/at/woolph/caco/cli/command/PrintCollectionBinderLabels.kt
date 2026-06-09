/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.command

import at.woolph.caco.binderlabels.determineBinderLabels
import at.woolph.caco.binderlabels.printBinderLabel
import at.woolph.lib.clikt.SuspendingTransactionCliktCommand
import at.woolph.utils.io.toKotlinxPath
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path

class PrintCollectionBinderLabels : SuspendingTransactionCliktCommand(name = "binder-labels") {
  val output by option("--output", "-o").path(canBeDir = false, canBeFile = true).required()
  val labelsPerPage by option("--labels-per-page", "-p").int().default(5)

  override suspend fun runTransaction() {
    val labels =
        determineBinderLabels(
            thresholdTooMuchPages = 45,
            thresholdTooFewPages = 12,
        )

    printBinderLabel(
        file = output.toKotlinxPath(),
        labels = labels.toList(),
        labelsPerPage = labelsPerPage,
    )
  }
}
