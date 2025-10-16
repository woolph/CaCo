/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.command

import at.woolph.caco.cli.PagePositionCalculator
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.terminal.ConversionResult
import com.github.ajalt.mordant.terminal.prompt

class PrintPagePositions : SuspendingCliktCommand(name = "page-position") {
    val rowsPerPage by option(help = "The number of rows per page").int().default(3)
    val columnsPerPage by option(help = "The number of columns per page").int().default(3)
    val loadingBackside by option(help = "Flag whether backside is loadable too").boolean().default(true)

    override suspend fun run() {
      val pagePositionCalculator = PagePositionCalculator(PagePositionCalculator.PageMetadata("", rowsPerPage, columnsPerPage, loadingBackside))

      generateSequence {
        terminal.prompt("Set number") { ConversionResult.Valid(it.toIntOrNull()) }
      }.map { pagePositionCalculator.printPageNumberAndPosition(it) }
        .forEach {
          terminal.println("p${it.page + 1} ${it.face}")

          for (x in 0..<rowsPerPage) {
            for (y in 0..<columnsPerPage) {
              terminal.print(if (x * 3 + y == it.pocket) "\u25AE" else "\u25AF")
            }
            terminal.println()
          }
        }
    }
}
