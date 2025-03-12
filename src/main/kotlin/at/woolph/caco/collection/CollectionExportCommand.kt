package at.woolph.caco.collection

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.path
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.io.path.Path

class CollectionExportCommand: CliktCommand(name = "export") {
  val format by option(help="The format to export the entered cards to")
    .enum<CollectionFileFormat>()
    .default(CollectionFileFormat.ARCHIDEKT)

    val file by argument(help="The file to export").path().default(
      Path(
        System.getProperty("user.home"),
        "Downloads"
      )
    )
    override fun run() {
      transaction {
        when(format) {
          CollectionFileFormat.ARCHIDEKT -> CardCollectionItem.getFromDatabase().exportArchidekt(file)
          CollectionFileFormat.DECKBOX -> CardCollectionItem.getFromDatabase().exportDeckbox(file)
        }
      }
    }
}
