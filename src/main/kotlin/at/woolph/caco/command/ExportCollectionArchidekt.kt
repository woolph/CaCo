package at.woolph.caco.command

import at.woolph.caco.exporter.collection.exportArchidekt
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.types.path
import kotlin.io.path.Path

class ExportCollectionArchidekt: CliktCommand(name = "collection-archidekt") {
    val file by argument(help="The file to export").path().default(
      Path(
        System.getProperty("user.home"),
        "Downloads"
      )
    )
    override fun run() {
      exportArchidekt(file)
    }
}
