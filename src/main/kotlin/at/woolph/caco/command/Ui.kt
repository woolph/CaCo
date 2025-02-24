package at.woolph.caco.command

import at.woolph.caco.gui.MyApp
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import tornadofx.launch

class Ui: CliktCommand() {
    override val treatUnknownOptionsAsArgs: Boolean = true
    val args by argument().multiple()

    override fun run() {
      launch<MyApp>(*args.toTypedArray())
    }
}