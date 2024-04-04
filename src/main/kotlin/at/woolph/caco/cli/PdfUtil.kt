package at.woolph.caco.cli

import at.woolph.libs.pdf.*
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlin.math.max

fun adjustTextToFitWidth(originalText: String, font: Font, maxWidth: Float, minFontSize: Float): Pair<String, Font> {
    val shrinkFactor = 0.95f
    var shortendText = originalText
    var shrinkedFont = font

    while (shrinkedFont.getWidth(shortendText) > maxWidth && shrinkedFont.size*shrinkFactor > minFontSize) {
        shrinkedFont = shrinkedFont.relative(shrinkFactor)
    }

    while (shrinkedFont.getWidth(shortendText) > maxWidth && shortendText.length > 4) {
        shortendText = shortendText.substring(0, shortendText.length-4) + "..."
    }

    return shortendText to shrinkedFont
}

fun ByteArray.toPDImage(page: Page) = PDImageXObject.createFromByteArray(page.document, this ,null)

fun Node.drawAsImageCentered(subIcon: PDImageXObject, maximumWidth: Float, desiredHeight: Float, xOffsetIcons: Float, yOffsetIcons: Float) {
    val heightScale = desiredHeight/subIcon.height
    val desiredWidth = subIcon.width*heightScale
    val (actualWidth, actualHeight) = if (desiredWidth > maximumWidth) {
        maximumWidth to subIcon.height*maximumWidth/subIcon.width
    } else {
        desiredWidth to desiredHeight
    }
    drawImage(subIcon, box.width*0.5f + xOffsetIcons - actualWidth*0.5f, yOffsetIcons+(desiredHeight-actualHeight)*0.5f, actualWidth, actualHeight)
}
fun Node.drawAsImageLeft(subIcon: PDImageXObject, maximumWidth: Float, desiredHeight: Float, xOffsetIcons: Float, yOffsetIcons: Float) {
    val heightScale = desiredHeight/subIcon.height
    val desiredWidth = subIcon.width*heightScale
    val (actualWidth, actualHeight) = if (desiredWidth > maximumWidth) {
        maximumWidth to subIcon.height*maximumWidth/subIcon.width
    } else {
        desiredWidth to desiredHeight
    }
    drawImage(subIcon, xOffsetIcons, yOffsetIcons+(desiredHeight-actualHeight)*0.5f, actualWidth, actualHeight)
}

val dotsPerMillimeter = PDRectangle.A4.width/210f