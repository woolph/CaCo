/* Copyright 2025 Wolfgang Mayer */
package at.woolph.utils.pdf

import org.apache.pdfbox.pdmodel.font.PDFont

actual class Font(
    val family: PDFont,
) {
  actual fun withSize(size: Float): SizedFont =
    SizedFont(family, size)
}
