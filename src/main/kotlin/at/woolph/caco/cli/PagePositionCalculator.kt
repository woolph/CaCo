/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli

import com.github.ajalt.mordant.terminal.ConversionResult
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.prompt

class PagePositionCalculator(
    val terminal: Terminal,
    val rowsPerPage: Int = 3,
    val columnsPerPage: Int = 3,
    val loadableBackside: Boolean = true,
    val tapped: Boolean = false,
) {
    val pocketsPerPageSide = rowsPerPage * columnsPerPage
    val pocketsPerPage = if (loadableBackside) pocketsPerPageSide * 2 else pocketsPerPageSide

    fun page() {
        do {
            val result =
                terminal
                    .prompt<Int?>("Set number") {
                        ConversionResult.Valid(it.toIntOrNull())
                    }?.apply {
                        printPageNumberAndPosition(this)
                    }
        } while (result != null)
    }

    fun printPageNumberAndPosition(setNumber: Int) {
        val it = setNumber - 1
        val page = (it / pocketsPerPage)
        val pocket = it % pocketsPerPageSide
        val pageSide = if (!loadableBackside || (it / pocketsPerPageSide) % 2 == 0) "front" else "back"

        terminal.println("p${page + 1} $pageSide")

        for (x in 0..<rowsPerPage) {
            for (y in 0..<columnsPerPage) {
                terminal.print(if (x * 3 + y == pocket) "\u25AE" else "\u25AF")
            }
            terminal.println()
        }
    }
}
