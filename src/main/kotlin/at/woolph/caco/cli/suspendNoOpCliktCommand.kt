/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli

import com.github.ajalt.clikt.command.SuspendingNoOpCliktCommand

fun suspendNoOpCliktCommand(name: String, block: SuspendingNoOpCliktCommand.() -> Unit = {}) =
  object : SuspendingNoOpCliktCommand(name) {}.apply(block)