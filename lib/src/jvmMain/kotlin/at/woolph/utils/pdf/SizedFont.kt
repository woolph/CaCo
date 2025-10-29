package at.woolph.utils.pdf

import be.quodlibet.boxable.utils.FontUtils
import org.apache.pdfbox.pdmodel.font.PDFont

actual class SizedFont(
  val family: PDFont,
  actual val size: Float,
) {
  actual fun withSize(size: Float) = SizedFont(family, size)

  actual fun withRelativeSize(sizeFactor: Float) = withSize(size * sizeFactor)

  actual fun getWidth(text: String): Float = family.getStringWidth(text) / 1000 * size

  actual val totalHeight = FontUtils.getHeight(family, size)
  actual val descent = FontUtils.getDescent(family, size)
  actual val height = totalHeight + descent // family.fontDescriptor.fontBoundingBox.height / 1000 * size

  actual fun adjustedTextToFitWidth(
    originalText: String?,
    maxWidth: Float,
    minFontSize: Float,
    block: (text: String, sizedFont: SizedFont) -> Unit
  ) {
    var shortendText = originalText ?: return
    var shrinkedFont = this
    val shrinkFactor = 0.95f

    while (
      shrinkedFont.getWidth(shortendText) > maxWidth &&
      shrinkedFont.size * shrinkFactor > minFontSize
    ) {
      shrinkedFont = shrinkedFont.withRelativeSize(shrinkFactor)
    }

    while (shrinkedFont.getWidth(shortendText) > maxWidth && shortendText.length > 4) {
      shortendText = shortendText.dropLast(4) + "..."
    }

    block(shortendText, shrinkedFont)
  }
}