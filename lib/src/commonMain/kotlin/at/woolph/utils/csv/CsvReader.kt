package at.woolph.utils.csv

import kotlinx.io.files.Path

expect class CsvReader(
  file: Path,
  explicitHeader: Array<String>? = null,
): AutoCloseable {
  val header: Map<String, Int>

  fun readNext(): CsvRecord?

  override fun close()
}
