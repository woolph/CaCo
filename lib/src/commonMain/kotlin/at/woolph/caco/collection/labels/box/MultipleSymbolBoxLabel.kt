/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.collection.labels.box

interface MultipleSymbolBoxLabel : BoxLabel {
  val rows: Int
  val icons: List<ByteArray?>
}
