package at.woolph.utils.csv

import com.opencsv.CSVWriter
import kotlinx.io.asOutputStream
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

actual class CsvWriter actual constructor(file: Path) : AutoCloseable {
  private val csvWriter =
    CSVWriter(SystemFileSystem.sink(file).buffered().asOutputStream().bufferedWriter())

  actual fun write(data: Array<String>) {
    csvWriter.writeNext(data, false)
  }

  actual fun write(record: CsvRecord) {
    write(record.data)
  }

  actual override fun close() {
    csvWriter.close()
  }

}