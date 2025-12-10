package at.woolph.caco.labels.box

interface MultipleSymbolBoxLabel : BoxLabel {
  val rows: Int
  val icons: List<ByteArray?>
}
