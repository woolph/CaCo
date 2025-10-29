/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.command

import at.woolph.caco.cli.CollectionPagePreview
import at.woolph.lib.clikt.ProgressTrackerWrapper
import at.woolph.lib.clikt.prompt
import at.woolph.utils.io.toKotlinxPath
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.widgets.progress.completed
import com.github.ajalt.mordant.widgets.progress.percentage
import com.github.ajalt.mordant.widgets.progress.progressBar
import com.github.ajalt.mordant.widgets.progress.progressBarContextLayout
import com.github.ajalt.mordant.widgets.progress.text
import com.github.ajalt.mordant.widgets.progress.timeRemaining
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.io.path.createDirectories

class PrintCollectionBinderPageView : SuspendingCliktCommand(name = "page-preview") {
  val output by option("--output", "-o").path(canBeDir = true, canBeFile = false).required()
  val sets by
      argument(help = "The set code of the cards to be entered")
          .multiple()
          .prompt("Enter the set codes to be imported/updated")

  override suspend fun run(): Unit = coroutineScope {
    val progress =
      progressBarContextLayout<String> {
        percentage()
        progressBar()
        completed(style = terminal.theme.success)
        timeRemaining(style = TextColors.magenta)
        text { context }
      }
        .animateInCoroutine(terminal, context = "fetching cards")

    launch { progress.execute() }

    output.createDirectories()
    CollectionPagePreview(ProgressTrackerWrapper(progress)).apply {
      sets.forEach { set ->
        terminal.println("Generating collection page preview for set $set")
        printLabel(set, output.resolve("$set.pdf").toKotlinxPath())
      }
    }
  }
}
