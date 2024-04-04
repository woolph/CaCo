package at.woolph.caco.cli

import at.woolph.libs.pdf.*
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject

fun adjustTextToFitWidth(originalText: String, font: Font, maxWidth: Float, minFontSize: Float): Pair<String, Font> {
    val shrinkFactor = 0.95f
    var shortendText = originalText
    var shrinkedFont = font

    while (shrinkedFont.getWidth(shortendText) > maxWidth && shrinkedFont.size*shrinkFactor > minFontSize) {
        shrinkedFont = shrinkedFont.relative(shrinkFactor)
    }

    while (shrinkedFont.getWidth(shortendText) > maxWidth) {
        shortendText = shortendText.substring(0, shortendText.length-4) + "..."
    }

    return shortendText to shrinkedFont
}

//set.subTitle?.let { _subTitle ->
//    when(titleAdjustment) {
//        TitleAdjustment.TEXT_CUT -> {
//            var title = _subTitle
//            while (fontTitle.getWidth(title) > maxTitleWidth) {
//                title = title.substring(0, title.length-4) + "..."
//            }
//            drawText90(title, fontSubTitle, subTitleXPosition + columnWidth*i, subTitleYPosition, fontSubColor)
//        }
//        TitleAdjustment.FONT_SIZE -> {
//            var fontSubTitleAdjusted = fontSubTitle
//            while (fontSubTitleAdjusted.getWidth(_subTitle) > maxTitleWidth) {
//                fontSubTitleAdjusted = fontSubTitleAdjusted.relative(0.95f)
//            }
//            drawText90(_subTitle, fontSubTitleAdjusted, subTitleXPosition + columnWidth*i, subTitleYPosition, fontSubColor)
//        }
//    }
//}

fun ByteArray.toPDImage(page: Page) = PDImageXObject.createFromByteArray(page.document, this ,null)

fun Node.drawAsImage(subIcon: PDImageXObject, maximumWidth: Float, desiredHeight: Float, i: Int, columnWidth: Float, xOffsetIcons: Float, yOffsetIcons: Float) {
    val heightScale = desiredHeight/subIcon.height
    val desiredWidth = subIcon.width*heightScale
    val (actualWidth, actualHeight) = if (desiredWidth > maximumWidth) {
        maximumWidth to subIcon.height*maximumWidth/subIcon.width
    } else {
        desiredWidth to desiredHeight
    }
    drawImage(subIcon, columnWidth*0.5f + xOffsetIcons + columnWidth*i - actualWidth*0.5f, yOffsetIcons+(desiredHeight-actualHeight)*0.5f, actualWidth, actualHeight)
}
