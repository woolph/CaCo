/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.gui

class JVMPlatform {
  val name: String = "Java ${System.getProperty("java.version")}"
}

fun getPlatform() = JVMPlatform()
