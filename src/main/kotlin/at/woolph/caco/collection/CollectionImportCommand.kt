package at.woolph.caco.collection

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.function.Predicate
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.readAttributes
import kotlin.io.path.useDirectoryEntries

class CollectionImportCommand : CliktCommand(name = "import") {
  val format by option(help = "The format of import file")
    .enum<CollectionFileFormat>()
    .default(CollectionFileFormat.ARCHIDEKT)
  val clearBeforeImport by option().boolean().default(true)
  val after by option(help = "Restrict import to only lines updated after this date").convert { LocalDate.parse(it).atTime(23, 59, 59, 999_999_999).toInstant(ZoneOffset.UTC) }
  val before by option(help = "Restrict import to only lines updated before this date").convert { LocalDate.parse(it).atStartOfDay().toInstant(ZoneOffset.UTC) }

  val file by argument(help = "The file to import").path(mustExist = true).default(
    Path(
      System.getProperty("user.home"),
      "Downloads"
    )
  )

  override fun run() {
    val afterPredicate = after?.let { Predicate<Instant> { toBeTestedInstant -> toBeTestedInstant.isAfter(it) }}
    val beforePredicate = before?.let { Predicate<Instant> { toBeTestedInstant -> toBeTestedInstant.isBefore(it) }}
    val datePredicate: Predicate<Instant> = sequenceOf(afterPredicate, beforePredicate).filterNotNull().fold(Predicate { true }) { acc, predicate -> acc.and(predicate) }
    when (format) {
      CollectionFileFormat.DECKBOX -> importDeckbox(getDeckboxImportFile(file), datePredicate = datePredicate, clearBeforeImport = clearBeforeImport)
      CollectionFileFormat.ARCHIDEKT -> importArchidekt(getArchidektImportFile(file), datePredicate = datePredicate, clearBeforeImport = clearBeforeImport)
    }
  }

  fun getDeckboxImportFile(file: Path): Path =
    if (file.isDirectory()) {
      file.useDirectoryEntries { entries ->
        entries.filter { it.fileName.toString().let { it.startsWith("Inventory") && it.endsWith(".csv") } }
          .maxByOrNull { it.readAttributes<BasicFileAttributes>().lastModifiedTime() }
          ?: throw IllegalArgumentException("No Deckbox import file found in directory $file")
      }
    } else {
      file
    }

  fun getArchidektImportFile(file: Path): Path =
    if (file.isDirectory()) {
      file.useDirectoryEntries { entries ->
        entries.filter {
          it.fileName.toString().let { it.startsWith("archidekt-collection-export-") && it.endsWith(".csv") }
        }
          .maxByOrNull { it.readAttributes<BasicFileAttributes>().lastModifiedTime() }
          ?: throw IllegalArgumentException("No Archidekt import file found in directory $file")
      }
    } else {
      file
    }
}