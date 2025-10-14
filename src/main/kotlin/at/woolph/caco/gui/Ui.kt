/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.gui

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import io.nacular.doodle.application.application
import org.kodein.di.instance

class Ui : SuspendingCliktCommand() {
    override suspend fun run() {
        application(modules = listOf(
            PointerModule,
            PathModule,
            FontModule,
            basicLabelBehavior(),
            nativeTextFieldBehavior(),
            Module(name = "AppModule") {
                bindSingleton<Animator> { AnimatorImpl(instance(), instance()) }
            }
        ))  {
            MyApp(
                display = instance(),
                fonts = instance(),
                animator = instance(),
                textMetrics = instance(),
                pathMetrics = instance(),
                theme = instance(),
                themeManager = instance(),
                textFieldStyler = instance(),
            )
        }
    }
}
