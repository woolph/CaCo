/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.gui

class Greeting {
  private val platform = getPlatform()

  fun greet(): String {
    return "Hello, ${platform.name}!"
  }
}
