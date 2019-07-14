package at.woolph.pdf

import be.quodlibet.boxable.utils.FontUtils
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType1Font

open class Font(val family: PDFont, val size: Float) {
    fun getWidth(text: String): Float {
        return family.getStringWidth(text) / 1000 * size
    }

    val height = FontUtils.getHeight(family, size)	// family.fontDescriptor.fontBoundingBox.height / 1000 * size
    val descent = FontUtils.getDescent(family, size)
    val totalHeight = height + descent

    fun relative(sizeFactor: Float) = Font(this.family, this.size*sizeFactor)
}
