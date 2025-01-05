package at.woolph.caco.cli

import com.github.ajalt.mordant.terminal.ConversionResult
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.prompt

class PagePositionCalculator(
    val terminal: Terminal,
    val pocketsPerPageSide: Int = 9,
) {
    fun page() {
        do {
            val result = terminal.prompt<Int?>("Set number") {
                ConversionResult.Valid(it.toIntOrNull())
            }?.apply {
                printPageNumberAndPosition(pocketsPerPageSide, this)
            }
        } while(result != null)
    }

    fun printPageNumberAndPosition(pocketsPerPageSide: Int, setNumber: Int) {
        val it = setNumber - 1
        val page = (it / pocketsPerPageSide) / 2
        val pocket = it % pocketsPerPageSide
        val pageSide = if ((it / pocketsPerPageSide) % 2 == 0) "front" else "back"

        terminal.println("p${page + 1} $pageSide")

        for (x in 0..2) {
            for (y in 0..2) {
                terminal.print(if (x * 3 + y == pocket) "\u25AE" else "\u25AF")
            }
            terminal.println()
        }
    }
}
