/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli.command

import arrow.core.raise.Raise
import at.woolph.caco.collection.CollectionFileFormat
import at.woolph.caco.collection.importArchidekt
import at.woolph.caco.collection.importDeckbox
import at.woolph.lib.clikt.NoFileFoundError
import at.woolph.lib.clikt.RaiseCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.FileSystems
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
import org.slf4j.LoggerFactory

class CollectionImport :
    RaiseCliktCommand<NoFileFoundError>(
        name = "import",
        log = LoggerFactory.getLogger(CollectionImport::class.java),
    ) {
  val format by
      option("--format", "-f", help = "The format of import file")
          .enum<CollectionFileFormat>()
          .default(CollectionFileFormat.ARCHIDEKT)
  val clearBeforeImport by option().boolean().default(true)
  val after by
      option(help = "Restrict import to only lines updated after this date").convert {
        LocalDate.parse(it).atTime(23, 59, 59, 999_999_999).toInstant(ZoneOffset.UTC)
      }
  val before by
      option(help = "Restrict import to only lines updated before this date").convert {
        LocalDate.parse(it).atStartOfDay().toInstant(ZoneOffset.UTC)
      }

  val file by
      argument(help = "The file to import")
          .path(mustExist = true)
          .default(
              Path(
                  System.getProperty("user.home"),
                  "Downloads",
              ),
          )

  /** make configurable */
  val deckboxGlobPattern = "glob:**/Inventory*.csv"
  val archidektGlobPattern = "glob:**/archidekt-collection-export-*.csv"

  override suspend fun Raise<NoFileFoundError>.run() {
    val afterPredicate =
        after?.let { Predicate<Instant> { toBeTestedInstant -> toBeTestedInstant.isAfter(it) } }
    val beforePredicate =
        before?.let { Predicate<Instant> { toBeTestedInstant -> toBeTestedInstant.isBefore(it) } }
    val datePredicate: Predicate<Instant> =
        sequenceOf(afterPredicate, beforePredicate).filterNotNull().fold(
            Predicate { true },
        ) { acc, predicate ->
          acc.and(predicate)
        }
    when (format) {
      CollectionFileFormat.DECKBOX ->
          importDeckbox(
              getImportFile(file, deckboxGlobPattern),
              datePredicate = datePredicate,
              clearBeforeImport = clearBeforeImport,
          )
      CollectionFileFormat.ARCHIDEKT ->
          importArchidekt(
              getImportFile(file, archidektGlobPattern),
              datePredicate = datePredicate,
              clearBeforeImport = clearBeforeImport,
          )
    }
  }

  private fun Raise<NoFileFoundError>.getImportFile(
      file: Path,
      globPattern: String,
  ): kotlinx.io.files.Path = kotlinx.io.files.Path(
      if (file.isDirectory()) {
        val pathMatcher = FileSystems.getDefault().getPathMatcher(globPattern)
        file.useDirectoryEntries { entries ->
          entries.filter(pathMatcher::matches).maxByOrNull {
            it.readAttributes<BasicFileAttributes>().lastModifiedTime()
          }
              ?: raise(
                  NoFileFoundError(
                          file,
                          "The given file \"$file\" is a directory, therefore it should contain a file matching the pattern \"$globPattern\", but no matching file was found (the given file may also be the file to be imported!).",
                          "file",
                      )
                      .also { it.context = this@CollectionImport.currentContext },
              )
        }
      } else {
        file
      }.toString())
}
