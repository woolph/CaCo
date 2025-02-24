package at.woolph.caco.command

import at.woolph.caco.importer.collection.importDeckbox
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.readAttributes
import kotlin.io.path.useDirectoryEntries

class ImportDeckboxCollection: CliktCommand(name = "deckbox-collection") {
    val file by argument(help="The file to import").path(mustExist = true).default(
      Path(
        System.getProperty("user.home"),
        "Downloads"
      )
    )
    override fun run() {
            if(file.isDirectory()) {
                file.useDirectoryEntries { entries ->
                    entries.filter { it.fileName.toString().let { it.startsWith("Inventory") && it.endsWith(".csv")} }
                        .maxByOrNull { it.readAttributes<BasicFileAttributes>().lastModifiedTime() }
                }
            } else {
                file
            }?.let {
              importDeckbox(it)
            }
    }
}