package at.woolph.caco.collection

import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.utils.Either
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Instant
import java.util.function.Predicate
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter

private val log = LoggerFactory.getLogger("at.woolph.caco.collection.ImportCsv")

fun import(
  file: Path,
  notImportedOutputFile: Path = Path.of("not-imported.csv"),
  datePredicate: Predicate<Instant> =  Predicate { true },
  clearBeforeImport: Boolean = false,
  mapper: (CsvRecord) -> Either<CardCollectionItem, String>,
) {
  println("importing collection export file $file")
  transaction {
    if (clearBeforeImport) {
      println("current collection possessions are cleared!")
      CardPossessions.deleteAll()
    }
    CSVWriter(notImportedOutputFile.bufferedWriter()).use { writer ->
      CSVReader(file.bufferedReader()).use { reader ->
        val header = reader.readNext().also {
          writer.writeNext(buildList {
            addAll(it)
            add("Error")
          }.toTypedArray(), false)
        }.withIndex().associate { it.value to it.index }

        var countOfUnparsed = 0
        var countOfSkipped = 0
        var importedCards = 0
        generateSequence { reader.readNext() }.filter { it.size > 1 }.map { CsvRecord(header, it) }
          .forEach {
          try {
            when (val result = mapper(it)) {
              is Either.Left -> {
                if (datePredicate.test(result.value.dateAdded)) {
                  result.value.addToCollection()
                  importedCards++
                } else {
                  log.warn("not imported due to date restriction")
                  countOfSkipped++
                  writer.writeNext(buildList {
                    addAll(it.data)
                    add("not imported due to date restriction")
                  }.toTypedArray(), false)
                }
              }
              is Either.Right -> {
                log.warn("not imported due ${result.value}")
              }
            }
          } catch (e: Exception) {
            log.error("unable to import: ${e.message}", if (log.isDebugEnabled) e else null)
            countOfUnparsed++
            writer.writeNext(buildList {
              addAll(it.data)
              add("${e.message}")
            }.toTypedArray(), false)
          }
        }
        if (countOfSkipped > 0) {
          println("$countOfSkipped were skipped due to date predicate!!")
        }
        if (countOfUnparsed > 0) {
          println("unable to import $countOfUnparsed lines")
        } else {
          println("all cards imported successfully ($importedCards in total)!!")
        }
      }
    }
  }
}

data class CsvRecord(
  private val header: Map<String, Int>,
  val data: Array<String>,
) {
  operator fun get(column: String): String? = header[column]?.let { data[it] }
}

fun Iterable<CardCollectionItem>.export(file: Path, columnExtractors: Map<String, CardCollectionItem.() -> String>) {
  println("exporting collection to $file")
  CSVWriter(file.bufferedWriter()).use { writer ->
    val columnExtractors = columnExtractors.entries
    writer.writeNext(columnExtractors.map { it.key }.toTypedArray(), false)
    filter(CardCollectionItem::isNotEmpty).forEach { item ->
      writer.writeNext(columnExtractors.map { it.value(item) }.toTypedArray(), false)
    }
  }
}
