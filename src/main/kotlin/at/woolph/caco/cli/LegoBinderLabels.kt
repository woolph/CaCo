package at.woolph.caco.cli

import at.woolph.caco.binderlabels.BlankLabel
import at.woolph.caco.binderlabels.GenericLabel
import at.woolph.caco.binderlabels.MapLabelItem
import at.woolph.caco.binderlabels.darkLabels
import at.woolph.libs.pdf.Font
import at.woolph.libs.pdf.createPdfDocument
import at.woolph.libs.pdf.drawBackground
import at.woolph.libs.pdf.drawBorder
import at.woolph.libs.pdf.drawImage
import at.woolph.libs.pdf.drawText
import at.woolph.libs.pdf.drawText90
import at.woolph.libs.pdf.frame
import at.woolph.libs.pdf.page
import be.quodlibet.boxable.HorizontalAlignment
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.awt.Color

class LegoBinderLabels(
//    val inputReader: LineReader,
//    val output: OutputStream,
) {
    fun printLabel(file: String) {
        createPdfDocument {
            val titleAdjustment = TitleAdjustment.FONT_SIZE
            val labelsWide : List<MapLabelItem> = listOf(
                GenericLabel("01", "BAUANLEITUNGEN", "Ninjago & Marvel"),
                GenericLabel("02", "BAUANLEITUNGEN", "Minecraft & Super Mario"),
                GenericLabel("03", "BAUANLEITUNGEN", "City, Creator, & Dots"),
                GenericLabel("04", "BAUANLEITUNGEN", "Speed & Technic"),
            )

            val fontColor = if (darkLabels) Color.WHITE else Color.BLACK
            val fontSubColor = if (darkLabels) Color.LIGHT_GRAY else Color.GRAY
            val backgroundColor: Color? = if (darkLabels) Color.BLACK else null

            val mtgLogo = PDImageXObject.createFromFile("./LEGO_logo.svg.png", this)

            val fontFamilyPlanewalker = PDType0Font.load(this, javaClass.getResourceAsStream("/fonts/Legothick.ttf"))
            val fontFamily72Black = PDType0Font.load(this, javaClass.getResourceAsStream("/fonts/72-Black.ttf"))

            val fontTitle = Font(fontFamilyPlanewalker, 64f)
            val fontSubTitle = Font(fontFamily72Black, 16f)
            val fontCode = Font(fontFamily72Black, 28f)
            val fontCodeCommanderSubset = Font(fontFamily72Black, 14f)

            val mapLabels = mapOf(4 to labelsWide)

            val pageFormat = PDRectangle.A4

            mapLabels.forEach { (columns, labels) ->
                val columnWidth = pageFormat.width/columns

                val magicLogoHPadding = 10f
                val titleXPosition = (columnWidth + fontTitle.size)*0.5f - 10f //64f
                val titleYPosition = 200f
                val subTitleXPosition = titleXPosition+22f
                val subTitleYPosition = titleYPosition+30f

                val maximumWidth = columnWidth-20f
                val desiredHeight = 64f
                val logoYPosition = 48f

                val mtgLogoWidth = columnWidth-2*magicLogoHPadding
                val mtgLogoHeight = mtgLogo.height.toFloat()/mtgLogo.width.toFloat()*mtgLogoWidth

                val magicLogoYPosition = 842f-14f-mtgLogoHeight
                val maxTitleWidth = magicLogoYPosition-titleYPosition

                for(pageIndex in 0.. (labels.size-1)/columns) {
                    page(pageFormat) {
                        println("column label page #$pageIndex")
                        val columnWidth = box.width/columns

                        for (i in 0 until columns) {
                            labels.getOrNull(columns*pageIndex + i)?.let { set ->
                                if (set != BlankLabel) {
                                    frame(columnWidth*i+1f, 0f, (columnWidth)*(columns-i-1)+1f, 0f) {
                                        backgroundColor?.let { drawBackground(it) }
                                        drawBorder(1f, fontColor)

//                                        drawImage(mtgLogo, magicLogoHPadding + columnWidth*i, magicLogoYPosition, mtgLogoWidth, mtgLogoHeight)

                                        set.subTitle?.let { _subTitle ->
                                            when(titleAdjustment) {
                                                TitleAdjustment.TEXT_CUT -> {
                                                    var title = _subTitle
                                                    while (fontTitle.getWidth(title) > maxTitleWidth) {
                                                        title = title.substring(0, title.length-4) + "..."
                                                    }
                                                    drawText90(title, fontSubTitle, subTitleXPosition + columnWidth*i, subTitleYPosition, fontSubColor)
                                                }
                                                TitleAdjustment.FONT_SIZE -> {
                                                    var fontSubTitleAdjusted = fontSubTitle
                                                    while (fontSubTitleAdjusted.getWidth(_subTitle) > maxTitleWidth) {
                                                        fontSubTitleAdjusted = fontSubTitleAdjusted.relative(0.95f)
                                                    }
                                                    drawText90(_subTitle, fontSubTitleAdjusted, subTitleXPosition + columnWidth*i, subTitleYPosition, fontSubColor)
                                                }
                                            }
                                        }

                                        when(titleAdjustment) {
                                            TitleAdjustment.TEXT_CUT -> {
                                                var title = set.title
                                                while (fontTitle.getWidth(title) > maxTitleWidth) {
                                                    title = title.substring(0, title.length-4) + "..."
                                                }
                                                drawText90(title, fontTitle, titleXPosition + columnWidth*i, titleYPosition, fontColor)
                                            }
                                            TitleAdjustment.FONT_SIZE -> {
                                                var fontTitleAdjusted = fontTitle
                                                while (fontTitleAdjusted.getWidth(set.title) > maxTitleWidth) {
                                                    fontTitleAdjusted = fontTitleAdjusted.relative(0.95f)
                                                }
                                                drawText90(set.title, fontTitleAdjusted, titleXPosition + columnWidth*i, titleYPosition, fontColor)
                                            }
                                        }


                                        drawImage(mtgLogo, magicLogoHPadding + columnWidth*i, logoYPosition, mtgLogoWidth, mtgLogoHeight)
//                                        set.mainIcon?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidth, desiredHeight, i, columnWidth, 0f, 68f) }

                                        frame(5f, 842f-120f+15f, 5f, 5f) {
                                            //								drawBackground(Color.WHITE)
                                            //								drawBorder(1f, Color.BLACK)
                                            drawText(set.code.uppercase(), fontCode, HorizontalAlignment.CENTER, 40f, fontColor)
//                                            set.subCode?.let { subCode ->
//                                                //											drawText("&", fontCodeCommanderSubset.relative(0.75f), HorizontalAlignment.CENTER, 32f, fontSubColor)
//                                                drawText(subCode.uppercase(), fontCodeCommanderSubset, HorizontalAlignment.CENTER, 25f, fontSubColor)
//                                            }
//
//                                            val maximumWidthSub = 20f
//                                            val desiredHeightSub = 20f
//                                            val xOffsetIcons = 32f
//                                            val yOffsetIcons = 58f
//
//                                            set.subIconRight?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidthSub, desiredHeightSub, i, columnWidth, xOffsetIcons, yOffsetIcons) }
//                                            set.subIconLeft?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidthSub, desiredHeightSub, i, columnWidth, -xOffsetIcons, yOffsetIcons) }
//
//                                            val xOffsetIcons2 = 32f+15f
//                                            val yOffsetIcons2 = 58f+25f
//
//                                            set.subIconRight2?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidthSub, desiredHeightSub, i, columnWidth, xOffsetIcons2, yOffsetIcons2) }
//                                            set.subIconLeft2?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidthSub, desiredHeightSub, i, columnWidth, -xOffsetIcons2, yOffsetIcons2) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            save(file)
        }
    }
}

fun main() {
    LegoBinderLabels().printLabel("C:\\Users\\001121673\\LegoBinderLabels.pdf")
}
