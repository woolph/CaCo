package at.woolph.caco.command

import at.woolph.caco.cli.CollectionPagePreview
import at.woolph.libs.prompt
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import kotlin.io.path.createDirectories

class PrintCollectionBinderPageView: CliktCommand(name = "collection-pages") {
    val output by option().path(canBeDir = true, canBeFile = false).required()
    val sets by argument(help="The set code of the cards to be entered").multiple().prompt("Enter the set codes to be imported/updated")

    override fun run() = runBlocking<Unit> {
      output.createDirectories()
      CollectionPagePreview(terminal).apply {
        sets.forEach { set ->
          printLabel(set, output.resolve("${set}.pdf"))
        }
      }
    }
}
