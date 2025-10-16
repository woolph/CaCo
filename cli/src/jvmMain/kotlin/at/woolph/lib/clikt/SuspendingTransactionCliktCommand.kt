/* Copyright 2025 Wolfgang Mayer */
package at.woolph.lib.clikt

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

abstract class SuspendingTransactionCliktCommand(
    name: String? = null,
) : SuspendingCliktCommand(name) {
    override suspend fun run() {
        newSuspendedTransaction {
            runTransaction()
        }
    }

    open suspend fun runTransaction() {}
}
