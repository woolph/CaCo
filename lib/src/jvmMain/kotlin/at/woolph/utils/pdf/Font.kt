/* Copyright 2025 Wolfgang Mayer */
package at.woolph.utils.pdf

import be.quodlibet.boxable.utils.FontUtils
import org.apache.pdfbox.pdmodel.font.PDFont

open class Font(
    val family: PDFont,
    val size: Float,
) {
  fun getWidth(text: String): Float = family.getStringWidth(text) / 1000 * size

  val totalHeight = FontUtils.getHeight(family, size)
  val descent = FontUtils.getDescent(family, size)
  val height = totalHeight + descent // family.fontDescriptor.fontBoundingBox.height / 1000 * size

  fun relative(sizeFactor: Float) = Font(this.family, this.size * sizeFactor)

  fun adjustedTextToFitWidth(
    originalText: String?,
    maxWidth: Float,
    minFontSize: Float = size,
    block: (text: String, font: Font) -> Unit
  ) {
    var shortendText = originalText ?: return
    var shrinkedFont = this
    val shrinkFactor = 0.95f

    while (
      shrinkedFont.getWidth(shortendText) > maxWidth &&
      shrinkedFont.size * shrinkFactor > minFontSize
    ) {
      shrinkedFont = shrinkedFont.relative(shrinkFactor)
    }

    while (shrinkedFont.getWidth(shortendText) > maxWidth && shortendText.length > 4) {
      shortendText = shortendText.substring(0, shortendText.length - 4) + "..."
    }

    block(shortendText, shrinkedFont)
  }
}
