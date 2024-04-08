package at.woolph.caco.cli

import at.woolph.caco.binderlabels.*
import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.libs.pdf.Font
import at.woolph.libs.pdf.createPdfDocument
import at.woolph.libs.pdf.drawBorder
import at.woolph.libs.pdf.drawText
import at.woolph.libs.pdf.frame
import at.woolph.libs.pdf.page
import be.quodlibet.boxable.HorizontalAlignment
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
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

open class GenericPileSeparator(override val code: String, override val title: String): PileSeparator {
    override val icon: ByteArray? by lazy { URI("https://c2.scryfall.com/file/scryfall-symbols/sets/default.svg?1647835200").renderSvgAsMythic() }
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
                    SimpleSetPileSeparator("mkm"),
                    SimpleSetPileSeparator("m21"),
                    SimpleSetPileSeparator("dom"),
                    SimpleSetPileSeparator("m20"),
                    SimpleSetPileSeparator("m19"),
                    SimpleSetPileSeparator("rna"),
                    SimpleSetPileSeparator("grn"),
                    SimpleSetPileSeparator("war"),
                )

                val fontColor = Color.BLACK
                val fontFamily72Black = PDType0Font.load(this, javaClass.getResourceAsStream("/fonts/72-Black.ttf"))

                val pageFormat = PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width)

                val columns = 4
                val rows = 2
                val itemsPerPage = columns * rows

                val columnWidth = pageFormat.width/columns
                val rowHeight = pageFormat.height/rows

                val magicCardHeight = 88f*dotsPerMillimeter

                val margin = 12f
                val defaultGapSize = 5f

                val bannerHeight = rowHeight - magicCardHeight - margin

                val fontCode = Font(fontFamily72Black, bannerHeight * 0.9f)

                val maximumWidth = columnWidth - 2*margin
                val desiredHeight = bannerHeight * 0.9f
                val maximumIconWidth = bannerHeight
                val offsetX = (maximumWidth - maximumIconWidth) * 0.5f

                separators.chunked(itemsPerPage).forEachIndexed { pageIndex, separatorItems ->
                    page(pageFormat) {
                        println("column separator page #$pageIndex")
                        separatorItems.chunked(columns).forEachIndexed { rowIndex, rowItems ->
                            rowItems.forEachIndexed { columnIndex, columnItem ->
                                val borderWidth = 2f
                                frame(columnWidth*columnIndex, rowHeight*rowIndex, (columnWidth)*(columns-columnIndex-1), (rowHeight)*(rows-rowIndex-1)) {
                                    drawBorder(borderWidth, fontColor)
                                    frame(margin, margin, margin, margin) {
                                        if (columnItem != BlankPileSeparator) {
                                            drawText(
                                                columnItem.code.uppercase(),
                                                fontCode,
                                                HorizontalAlignment.CENTER,
                                                0f,
                                                fontCode.height,
                                                fontColor
                                            )
                                            columnItem.icon?.toPDImage(this@page)
                                                ?.let {
                                                    drawAsImageCentered(it, maximumIconWidth, desiredHeight, -offsetX, 0f)
                                                    drawAsImageCentered(it, maximumIconWidth, desiredHeight, +offsetX, 0f)
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
}

fun main() {
    PileSeparators().printLabel("C:\\Users\\001121673\\private\\magic\\CardSeparators.pdf")
}
