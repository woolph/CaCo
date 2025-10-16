/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.collection

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import at.woolph.caco.datamodel.collection.CardPossessions
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import java.nio.file.Path
import java.time.Instant
import java.util.function.Predicate
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("at.woolph.caco.collection.ImportCsv")

class IntentionallySkippedException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

fun importSequence(
    file: Path,
    notImportedOutputFile: Path = Path.of("not-imported.csv"),
    datePredicate: Predicate<Instant> = Predicate { true },
    mapper: arrow.core.raise.Raise<Throwable>.(CsvRecord) -> CardCollectionItem,
): Sequence<Either<Throwable, CardCollectionItem>> = sequence {
  println("importing collection export file $file")
  CSVWriter(notImportedOutputFile.bufferedWriter()).use { writer ->
    CSVReader(file.bufferedReader()).use { reader ->
      val header =
          reader
              .readNext()
              .also {
                writer.writeNext(
                    buildList {
                          addAll(it)
                          add("Error")
                        }
                        .toTypedArray(),
                    false,
                )
              }
              .withIndex()
              .associate { it.value to it.index }

      yieldAll(
          generateSequence { reader.readNext() }
              .filter { it.size > 1 }
              .map { CsvRecord(header, it) }
              .map { record ->
                either {
                      mapper(record).also {
                        ensure(datePredicate.test(it.dateAdded)) {
                          IntentionallySkippedException("skipped due to date restriction")
                        }
                      }
                    }
                    .onLeft {
                      writer.writeNext(
                          buildList {
                                addAll(record.data)
                                add(it.message)
                              }
                              .toTypedArray(),
                          false,
                      )
                    }
              }
      )
    }
  }
}

fun import(
    file: Path,
    notImportedOutputFile: Path = Path.of("not-imported.csv"),
    datePredicate: Predicate<Instant> = Predicate { true },
    clearBeforeImport: Boolean = false,
    mapper: arrow.core.raise.Raise<Throwable>.(CsvRecord) -> CardCollectionItem,
) {
  println("importing collection export file $file")
  transaction {
    if (clearBeforeImport) {
      println("current collection possessions are cleared!")
      CardPossessions.deleteAll()
    }
    var countOfSkipped = 0
    var countOfImportError = 0
    var importedCards = 0

    importSequence(file, notImportedOutputFile, datePredicate, mapper).forEach { either ->
      either
          .onRight {
            it.addToCollection()
            importedCards++
          }
          .onLeft {
            when (it) {
              is IntentionallySkippedException -> {
                log.warn(it.message, if (log.isDebugEnabled) it else null)
                countOfSkipped++
              }

              else -> {
                log.error(it.message, if (log.isDebugEnabled) it else null)
                countOfImportError++
              }
            }
          }
    }
    if (countOfSkipped > 0) {
      println("skipped to import $countOfSkipped lines")
    }
    if (countOfImportError > 0) {
      println("unable to import $countOfImportError lines")
    } else {
      println("all cards imported successfully ($importedCards in total)!!")
    }
  }
}

data class CsvRecord(
    private val header: Map<String, Int>,
    val data: Array<String>,
) {
  operator fun get(column: String): String? = header[column]?.let { data[it] }
}

fun Iterable<CardCollectionItem>.export(
    file: Path,
    columnExtractors: Map<String, CardCollectionItem.() -> String>,
) {
  println("exporting collection to $file")
  CSVWriter(file.bufferedWriter()).use { writer ->
    val columnExtractors = columnExtractors.entries
    writer.writeNext(columnExtractors.map { it.key }.toTypedArray(), false)
    filter(CardCollectionItem::isNotEmpty).forEach { item ->
      writer.writeNext(columnExtractors.map { it.value(item) }.toTypedArray(), false)
    }
  }
}
