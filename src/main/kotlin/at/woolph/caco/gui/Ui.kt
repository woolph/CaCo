package at.woolph.caco.gui

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import tornadofx.launch

class Ui: SuspendingCliktCommand() {
    override val treatUnknownOptionsAsArgs: Boolean = true
    val args by argument().multiple()

    override suspend fun run() {
      launch<MyApp>(*args.toTypedArray())
    }
}
