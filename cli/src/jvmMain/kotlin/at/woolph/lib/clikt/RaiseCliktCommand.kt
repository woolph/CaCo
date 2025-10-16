/* Copyright 2025 Wolfgang Mayer */
package at.woolph.lib.clikt

import arrow.core.raise.Raise
import arrow.core.raise.either
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.CliktError
import org.slf4j.Logger

abstract class RaiseCliktCommand<T : CliktError>(
    name: String,
    val log: Logger,
) : SuspendingCliktCommand(name) {
  override suspend fun run() {
    either { run() }.onLeft { throw it }
  }

  abstract suspend fun Raise<T>.run()
}
