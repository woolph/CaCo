package at.woolph.utils.pdf

expect class SizedFont {
  fun withSize(size: Float): SizedFont

  fun withRelativeSize(sizeFactor: Float): SizedFont

  /** determines the width of the given text */
  fun getWidth(text: String): Float

  val size: Float
  val totalHeight: Float
  val descent: Float
  val height: Float

  fun adjustedTextToFitWidth(
    originalText: String?,
    maxWidth: Float,
    minFontSize: Float = size,
    block: (text: String, sizedFont: SizedFont) -> Unit
  )
}
