package at.woolph.utils.csv

import com.opencsv.CSVReader
import kotlinx.io.asInputStream
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

actual class CsvReader actual constructor(file: Path, explicitHeader: Array<String>?) :
  AutoCloseable {
  private val csvReader = CSVReader(SystemFileSystem.source(file).buffered().asInputStream().bufferedReader())
  actual val header: Map<String, Int>

  init {
    header = (explicitHeader ?: csvReader
      .readNext())
      .withIndex()
      .associate { it.value to it.index }
  }

  actual fun readNext(): CsvRecord? =
    generateSequence {
      csvReader.readNext()
    }.filter { it.size > 1 }
      .map { CsvRecord(header, it) }
      .firstOrNull()

  actual override fun close() {
    csvReader.close()
  }
}