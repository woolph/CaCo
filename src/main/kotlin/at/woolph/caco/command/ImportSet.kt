package at.woolph.caco.command

import at.woolph.caco.importer.sets.importCardsOfSet
import at.woolph.caco.importer.sets.importSet
import at.woolph.libs.prompt
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.util.Locale

class ImportSet: CliktCommand(name = "sets") {
    val setCodes by argument(help="sets to be imported").multiple().prompt("Enter the set codes to be imported/updated")
    override fun run() = runBlocking {
      newSuspendedTransaction {
        setCodes.forEach { setCode ->
          try {
            echo("importing set $setCode")
            importSet(setCode.lowercase(Locale.getDefault())).apply {
              importCardsOfSet()
            }
          } catch (ex: Exception) {
            log.error("error while importing ${setCodes}", ex)
          }
        }
      }
    }

    companion object {
        val log = LoggerFactory.getLogger(this::class.java.declaringClass)
    }
}
