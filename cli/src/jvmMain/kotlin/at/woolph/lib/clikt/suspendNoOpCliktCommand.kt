/* Copyright 2025 Wolfgang Mayer */
package at.woolph.lib.clikt

import com.github.ajalt.clikt.command.SuspendingNoOpCliktCommand

fun suspendNoOpCliktCommand(name: String, block: SuspendingNoOpCliktCommand.() -> Unit = {}) =
  object : SuspendingNoOpCliktCommand(name) {}.apply(block)