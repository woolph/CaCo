package at.woolph.caco.collection

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.readAttributes
import kotlin.io.path.useDirectoryEntries

class CollectionImportCommand: CliktCommand(name = "import") {
  val format by option(help="The format of import file")
    .enum<CollectionFileFormat>()
    .default(CollectionFileFormat.ARCHIDEKT)

    val file by argument(help="The file to import").path(mustExist = true).default(
      Path(
        System.getProperty("user.home"),
        "Downloads"
      )
    )
    override fun run() {
      when(format) {
        CollectionFileFormat.DECKBOX -> importDeckbox(getDeckboxImportFile(file))
        CollectionFileFormat.ARCHIDEKT -> importArchidekt(getArchidektImportFile(file))
      }
    }

  fun getDeckboxImportFile(file: Path): Path =
    if(file.isDirectory()) {
      file.useDirectoryEntries { entries ->
        entries.filter { it.fileName.toString().let { it.startsWith("Inventory") && it.endsWith(".csv")} }
          .maxByOrNull { it.readAttributes<BasicFileAttributes>().lastModifiedTime() }
          ?: throw IllegalArgumentException("No Deckbox import file found in directory $file")
      }
    } else {
      file
    }

  fun getArchidektImportFile(file: Path): Path =
    if(file.isDirectory()) {
      file.useDirectoryEntries { entries ->
        entries.filter { it.fileName.toString().let { it.startsWith("archidekt-collection-export-") && it.endsWith(".csv")} }
          .maxByOrNull { it.readAttributes<BasicFileAttributes>().lastModifiedTime() }
          ?: throw IllegalArgumentException("No Archidekt import file found in directory $file")
      }
    } else {
      file
    }
}