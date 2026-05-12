/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.labels.box

interface MultipleSymbolBoxLabel : BoxLabel {
  val rows: Int
  val icons: List<ByteArray?>
}
