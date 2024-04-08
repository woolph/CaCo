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

            val mapLabels = mapOf(4 to labelsWide)

            val pageFormat = PDRectangle.A4

            mapLabels.forEach { (columns, labels) ->
                val margin = 12f
                val defaultGapSize = 5f
                val columnWidth = pageFormat.width/columns
                val maximumWidth = columnWidth - 2*margin
                val maximumHeight = pageFormat.height - 2*margin

                val logoYPosition = 0f

                val mtgLogoWidth = maximumWidth
                val mtgLogoHeight = mtgLogo.height.toFloat()/mtgLogo.width.toFloat()*mtgLogoWidth

                val maxTitleWidth = maximumHeight - mtgLogoHeight - 2 * defaultGapSize - 80f

                val titleXPosition = (maximumWidth + fontTitle.height) * 0.5f
                val titleYPosition = maxTitleWidth + mtgLogoHeight + defaultGapSize
                val subTitleXPosition = titleXPosition + fontSubTitle.height + defaultGapSize
                val subTitleYPosition = titleYPosition - 30f

                labels.chunked(columns).forEachIndexed { pageIndex, pageLabels ->
                    page(pageFormat) {
                        println("column label page #$pageIndex")
                        pageLabels.forEachIndexed { i, set ->
                            if (set != BlankLabel) {
                                frame(columnWidth*i, 0f, (columnWidth)*(columns-i-1), 0f) {
                                    backgroundColor?.let { drawBackground(it) }
                                    drawBorder(1f, fontColor)

                                    frame(margin, margin, margin, margin) {
                                        set.subTitle?.let { _subTitle ->
                                            adjustTextToFitWidth(_subTitle, fontSubTitle, maxTitleWidth, 10f).let { (title, font) ->
                                                drawText(title, font, fontColor, subTitleXPosition, subTitleYPosition, 90.0)
                                            }
                                        }

                                        adjustTextToFitWidth(set.title, fontTitle, maxTitleWidth, 36f).let { (title, font) ->
                                            drawText(title, font, fontColor, titleXPosition, titleYPosition, 90.0)
                                        }

                                        drawAsImageCentered(mtgLogo, mtgLogoWidth, mtgLogoHeight, 0f, logoYPosition)

                                        drawText(set.code.uppercase(), fontCode, HorizontalAlignment.CENTER, 0f, titleYPosition + defaultGapSize + fontCode.height + 20f, fontColor)
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
    LegoBinderLabels().printLabel("C:\\Users\\001121673\\private\\LegoBinderLabels.pdf")
}
