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
                val columnWidth = pageFormat.width/columns

                val magicLogoHPadding = 10f
                val titleXPosition = (columnWidth + fontTitle.size)*0.5f - 10f //64f
                val titleYPosition = 200f
                val subTitleXPosition = titleXPosition+22f
                val subTitleYPosition = titleYPosition+30f

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
                                            adjustTextToFitWidth(_subTitle, fontSubTitle, maxTitleWidth, 10f).let { (title, font) ->
                                                drawText90(title, font, subTitleXPosition + columnWidth*i, subTitleYPosition, fontSubColor)
                                            }
                                        }

                                        adjustTextToFitWidth(set.title, fontTitle, maxTitleWidth, 36f).let { (title, font) ->
                                            drawText90(title, font, titleXPosition + columnWidth*i, titleYPosition, fontColor)
                                        }

                                        drawImage(mtgLogo, magicLogoHPadding + columnWidth*i, logoYPosition, mtgLogoWidth, mtgLogoHeight)

                                        frame(5f, 842f-120f+15f, 5f, 5f) {
                                            drawText(set.code.uppercase(), fontCode, HorizontalAlignment.CENTER, 40f, fontColor)
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
