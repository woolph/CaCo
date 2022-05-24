package at.woolph.caco.cli

import org.jline.terminal.Terminal

class Output(
    val terminal: Terminal
) {
    fun println() {
        terminal.writer().println()
        terminal.flush()
    }

    fun println(message: String) {
        terminal.writer().println(message)
        terminal.flush()
    }

    fun print(message: String) {
        terminal.writer().print(message)
        terminal.flush()
    }
}
