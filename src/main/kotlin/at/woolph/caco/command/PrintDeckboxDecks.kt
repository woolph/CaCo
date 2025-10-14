/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.command

import at.woolph.caco.decks.DeckboxDeckImporter
import at.woolph.caco.decks.DecklistPrinter
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.path
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
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlin.io.path.createDirectories
import kotlin.io.path.createParentDirectories
import kotlin.io.path.isDirectory

class PrintDeckboxDecks : SuspendingCliktCommand(name = "deckbox-decks") {
    val username by option(help = "Deckbox username").prompt("Enter the username of the deckbox user")
    val output by option().path(canBeDir = true, canBeFile = true)

    override suspend fun run() =
        coroutineScope {
            val progress =
                progressBarContextLayout<String> {
                    percentage()
                    progressBar()
                    completed(style = terminal.theme.success)
                    timeRemaining(style = TextColors.magenta)
                    text { "$context" }
                }.animateInCoroutine(terminal, context = "")

            val job = launch { progress.execute() }

            val decklistPrinter =
                output?.let {
                    if (it.isDirectory()) {
                        DecklistPrinter.Pdf(it.createDirectories())
                    } else {
                        DecklistPrinter.PdfOneFile(it.createParentDirectories())
                    }
                } ?: DecklistPrinter.Terminal(terminal)

            decklistPrinter.print(DeckboxDeckImporter(progress).importDeckboxDecks(username).take(20).toList())

            job.cancel("everything is done")
        }
}
