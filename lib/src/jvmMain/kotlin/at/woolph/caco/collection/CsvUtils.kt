/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.collection

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.utils.csv.CsvReader
import at.woolph.utils.csv.CsvRecord
import at.woolph.utils.csv.CsvWriter
import at.woolph.utils.csv.IntentionallySkippedException
import kotlinx.io.files.Path
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.function.Predicate

private val log = LoggerFactory.getLogger("at.woolph.caco.collection.ImportCsv")

fun importSequence(
  file: Path,
  notImportedOutputFile: Path = Path("not-imported.csv"),
  datePredicate: Predicate<Instant> = Predicate { true },
  mapper: arrow.core.raise.Raise<Throwable>.(CsvRecord) -> CardCollectionItem,
): Sequence<Either<Throwable, CardCollectionItem>> = sequence {
  println("importing collection export file $file")
  CsvWriter(notImportedOutputFile).use { writer ->

    CsvReader(file).use { reader ->
      writer.write(buildList {
        addAll(reader.header.keys)
        add("Error")
      }.toTypedArray())

      yieldAll(
          generateSequence { reader.readNext() }
              .map { record ->
                either {
                      mapper(record).also {
                        ensure(datePredicate.test(it.dateAdded)) {
                          IntentionallySkippedException("skipped due to date restriction")
                        }
                      }
                    }
                    .onLeft {
                      writer.write(
                          buildList {
                                addAll(record.data)
                                add(it.message ?: "")
                              }
                              .toTypedArray(),
                      )
                    }
              }
      )
    }
  }
}

fun import(
  file: Path,
  notImportedOutputFile: Path = Path("not-imported.csv"),
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

fun Iterable<CardCollectionItem>.export(
    file: Path,
    columnExtractors: Map<String, CardCollectionItem.() -> String>,
) {
  println("exporting collection to $file")
  CsvWriter(file).use { writer ->
    val columnExtractors = columnExtractors.entries
    writer.write(columnExtractors.map { it.key }.toTypedArray())
    filter(CardCollectionItem::isNotEmpty).forEach { item ->
      writer.write(columnExtractors.map { it.value(item) }.toTypedArray())
    }
  }
}
