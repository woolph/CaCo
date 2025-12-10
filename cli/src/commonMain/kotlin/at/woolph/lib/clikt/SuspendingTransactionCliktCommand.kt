/* Copyright 2025 Wolfgang Mayer */
package at.woolph.lib.clikt

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

abstract class SuspendingTransactionCliktCommand(
    name: String? = null,
) : SuspendingCliktCommand(name) {
  override suspend fun run() {
    suspendTransaction { runTransaction() }
  }

  open suspend fun runTransaction() {}
}
