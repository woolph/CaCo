package at.woolph.caco.cli

import at.woolph.caco.binderlabels.*
import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.libs.pdf.Font
import at.woolph.libs.pdf.createPdfDocument
import at.woolph.libs.pdf.drawBorder
import at.woolph.libs.pdf.drawImage
import at.woolph.libs.pdf.drawText
import at.woolph.libs.pdf.frame
import at.woolph.libs.pdf.page
import be.quodlibet.boxable.HorizontalAlignment
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.net.URI


interface PileSeparator {
    val code: String
    val title: String
    val icon: ByteArray?
}

object PromosPileSeparator: PileSeparator {
    override val code: String = "PRM"
    override val title: String = "Promos & Specials"
    override val icon: ByteArray? by lazy { URI("https://c2.scryfall.com/file/scryfall-symbols/sets/star.svg?1624852800").renderSvgAsMythic() }
}

object BlankPileSeparator: PileSeparator {
    override val code: String = ""
    override val title: String = ""
    override val icon: ByteArray? = null
}

open class GenericPileSeparator(override val code: String, override val title: String, override val subTitle: String? = null): MapLabelItem {
    override val mainIcon: ByteArray? by lazy { URI("https://c2.scryfall.com/file/scryfall-symbols/sets/default.svg?1647835200").renderSvgAsMythic() }
}

open class SimpleSetPileSeparator(override val code: String): PileSeparator {
    private val set = transaction {
        CardSet.findById(code) ?: throw IllegalArgumentException("no set with code $code found")
    }
    override val title: String get() = set.name
    override val icon: ByteArray? by lazy { set.icon.renderSvgAsMythic() }
}

class PileSeparators {
    fun printLabel(file: String) {
        Databases.init()

        transaction {
            createPdfDocument {
                val separators: List<PileSeparator> = listOf(
                    PromosPileSeparator,
                    SimpleSetPileSeparator("neo"),
                    SimpleSetPileSeparator("nec"),
                    SimpleSetPileSeparator("mkm"),
                    SimpleSetPileSeparator("m21"),
                    SimpleSetPileSeparator("dom"),
                )

                val fontColor = Color.BLACK

                val fontFamilyPlanewalker = PDType0Font.load(this, javaClass.getResourceAsStream("/fonts/PlanewalkerBold-xZj5.ttf"))
                val fontFamily72Black = PDType0Font.load(this, javaClass.getResourceAsStream("/fonts/72-Black.ttf"))

                val pageFormat = PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width)
                val dotsPerMillimeter = pageFormat.height/210f

                val columns = 4
                val rows = 2
                val itemsPerPage = columns * rows

                val columnWidth = pageFormat.width/columns
                val rowHeight = pageFormat.height/rows

                val bannerHeight = rowHeight - 88f*dotsPerMillimeter

                val fontCode = Font(fontFamily72Black, bannerHeight*0.9f)
                val fontTitle = Font(fontFamilyPlanewalker, bannerHeight*0.9f)


                val titleXPosition = (columnWidth + fontTitle.size)*0.5f - 10f //64f
                val titleYPosition = 164f

                val maximumWidth = columnWidth-20f
                val desiredHeight = 64f
                val marginTop = 15f
                val marginBottom = marginTop

                val maxTitleWidth = columnWidth - 15f

                separators.chunked(itemsPerPage).forEachIndexed { pageIndex, separatorItems ->
                    page(pageFormat) {
                        println("column separator page #$pageIndex")
                        separatorItems.chunked(columns).forEachIndexed { rowIndex, rowItems ->
                            rowItems.forEachIndexed { columnIndex, columnItem ->
                                val borderWidth = 2f
                                frame(columnWidth*columnIndex+borderWidth, rowHeight*rowIndex+borderWidth, (columnWidth)*(columns-columnIndex-1)+borderWidth, (rowHeight)*(rows-rowIndex-1)+borderWidth) {
                                    drawBorder(borderWidth, fontColor)
                                    if (columnItem != BlankPileSeparator) {
                                        drawText(columnItem.code.uppercase(), fontCode, HorizontalAlignment.LEFT, 55f, fontColor)
                                        columnItem.icon?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidth, desiredHeight, columnIndex, columnWidth, 0f, 5f) }
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
}

fun main() {
    PileSeparators().printLabel("C:\\Users\\001121673\\CardSeparators.pdf")
}
