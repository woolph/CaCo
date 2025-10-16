/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.command

import at.woolph.caco.masterdata.import.importCardsOfSet
import at.woolph.caco.masterdata.import.importSet
import at.woolph.lib.clikt.SuspendingTransactionCliktCommand
import at.woolph.lib.clikt.prompt
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import java.util.*
import org.slf4j.LoggerFactory

class UpdateSets : SuspendingTransactionCliktCommand(name = "update-sets") {
  val setCodes by
      argument(help = "sets to be imported")
          .multiple()
          .prompt("Enter the set codes to be imported/updated")

  override suspend fun runTransaction() {
    setCodes.forEach { setCode ->
      try {
        echo("importing set $setCode")
        importSet(setCode.lowercase(Locale.getDefault())).apply { importCardsOfSet() }
      } catch (ex: Exception) {
        log.error("error while importing ${setCodes}", ex)
      }
    }
  }

  companion object {
    val log = LoggerFactory.getLogger(this::class.java.declaringClass)
  }
}
