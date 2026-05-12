/* Copyright 2026 Wolfgang Mayer */
package at.woolph.utils.csv

import kotlinx.io.files.Path

expect class CsvWriter(
    file: Path,
) : AutoCloseable {
  fun write(data: Array<String>)

  fun write(record: CsvRecord)

  override fun close()
}
