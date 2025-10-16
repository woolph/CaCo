/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.gui

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
  Window(
      onCloseRequest = ::exitApplication,
      title = "KotlinProject",
  ) {
    App()
  }
}
