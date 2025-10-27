package at.woolph.utils.csv

import com.opencsv.CSVReader
import kotlinx.io.asInputStream
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

actual fun importSequence(
  file: Path,
  explicitHeader: Array<String>?,
): Sequence<CsvRecord> = sequence {
  CSVReader(SystemFileSystem.source(file).buffered().asInputStream().bufferedReader()).use { reader ->
    val header = (explicitHeader ?: reader
        .readNext())
        .withIndex()
        .associate { it.value to it.index }

    yieldAll(
      generateSequence { reader.readNext() }
        .filter { it.size > 1 }
        .map { CsvRecord(header, it) }
    )
  }
}
