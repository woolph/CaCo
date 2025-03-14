package at.woolph.caco.collection

import at.woolph.caco.cli.PagePositionCalculator
import com.github.ajalt.clikt.command.CoreSuspendingCliktCommand
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking

class PrintPagePositions: SuspendingCliktCommand(name = "page-position") {
    val rowsPerPage by option(help="The number of rows per page").int().default(3)
    val columnsPerPage by option(help="The number of columns per page").int().default(3)
    val loadingBackside by option(help="Flag whether backside is loadable too").boolean().default(true)
    val tapped by option(help="Flag whether cards are loaded 'tapped'").boolean().default(false)

    override suspend fun run() {
      PagePositionCalculator(terminal, rowsPerPage, columnsPerPage, loadingBackside, tapped).page()
    }
}
