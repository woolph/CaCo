package at.woolph.utils.csv

import kotlinx.io.files.Path

expect fun importSequence(
  file: Path,
  /** if set to null, first line will be interpreted as headers */
  explicitHeader: Array<String>? = null,
): Sequence<CsvRecord>