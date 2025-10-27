package at.woolph.utils.csv

data class CsvRecord(
    private val header: Map<String, Int>,
    val data: Array<String>,
) {
  operator fun get(column: String): String? = header[column]?.let { data[it] }
}
