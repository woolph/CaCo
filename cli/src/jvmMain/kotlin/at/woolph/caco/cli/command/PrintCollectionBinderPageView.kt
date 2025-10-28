/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.command

import at.woolph.caco.cli.command.util.CollectionPagePreview
import at.woolph.lib.clikt.prompt
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import kotlin.io.path.createDirectories

class PrintCollectionBinderPageView : SuspendingCliktCommand(name = "collection-pages") {
  val output by option("--output", "-o").path(canBeDir = true, canBeFile = false).required()
  val sets by
      argument(help = "The set code of the cards to be entered")
          .multiple()
          .prompt("Enter the set codes to be imported/updated")

  override suspend fun run() {
    output.createDirectories()
    CollectionPagePreview(terminal).apply {
      sets.forEach { set -> printLabel(set, output.resolve("$set.pdf")) }
    }
  }
}
