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

interface BoxLabel {
    val title: String
}

interface OneSymbolBoxLabel: BoxLabel {
    val icon: ByteArray?
}

object PromosBoxLabel: OneSymbolBoxLabel {
    override val title: String = "Promos"
    override val icon: ByteArray? by lazy { URI("https://c2.scryfall.com/file/scryfall-symbols/sets/star.svg?1624852800").renderSvgAsMythic() }
}

object BlankBoxLabel: BoxLabel {
    override val title: String = ""
}

open class GenericBoxLabel(override val title: String): OneSymbolBoxLabel {
    override val icon: ByteArray? by lazy { URI("https://c2.scryfall.com/file/scryfall-symbols/sets/default.svg?1647835200").renderSvgAsMythic() }
}

object PlainsBoxLabel: OneSymbolBoxLabel {
    override val title = "Plains"
    override val icon: ByteArray? by lazy { URI("https://svgs.scryfall.io/card-symbols/W.svg").renderSvg() }
}

object IslandBoxLabel: OneSymbolBoxLabel {
    override val title = "Island"
    override val icon: ByteArray? by lazy { URI("https://svgs.scryfall.io/card-symbols/U.svg").renderSvg() }
}

object SwampBoxLabel: OneSymbolBoxLabel {
    override val title = "Swamp"
    override val icon: ByteArray? by lazy { URI("https://svgs.scryfall.io/card-symbols/B.svg").renderSvg() }
}

object MountainBoxLabel: OneSymbolBoxLabel {
    override val title = "Mountain"
    override val icon: ByteArray? by lazy { URI("https://svgs.scryfall.io/card-symbols/R.svg").renderSvg() }
}

object ForestBoxLabel: OneSymbolBoxLabel {
    override val title = "Forest"
    override val icon: ByteArray? by lazy { URI("https://svgs.scryfall.io/card-symbols/G.svg").renderSvg() }
}

class PlanechaseBoxLabel(index: Int): OneSymbolBoxLabel {
    override val title = "Planechase $index"
    override val icon: ByteArray? by lazy { URI("https://svgs.scryfall.io/card-symbols/CHAOS.svg").renderSvg() }
}

class AwaitingCatalogizationBoxLabel(index: Int): OneSymbolBoxLabel {
    override val title = "Catalogize $index"
    override val icon: ByteArray? by lazy { URI("https://svgs.scryfall.io/sets/wth.svg").renderSvgAsMythic() }
}

class AwaitingCollectionBoxLabel(index: Int): OneSymbolBoxLabel {
    override val title = "To Be Filed $index"
    override val icon: ByteArray? by lazy { URI("https://svgs.scryfall.io/sets/ath.svg").renderSvgAsMythic() }
}

interface MultipleSymbolBoxLabel: BoxLabel {
    val icons: List<ByteArray>
}

open class DuplicateBoxLabel(vararg codes: String): MultipleSymbolBoxLabel {
    private val sets = transaction {
        codes.map {
            CardSet.findById(it) ?: throw IllegalArgumentException("no set with code $it found")
        }
    }
    override val title: String = "DUP"
    override val icons: List<ByteArray> by lazy { sets.mapNotNull { it.icon.renderSvgAsMythic() } }
}

class BoxLabels {
    fun printLabel(file: String) {
        Databases.init()

        transaction {
            createPdfDocument {
                val labels: List<BoxLabel> = listOf(
                    PlainsBoxLabel,
                    IslandBoxLabel,
                    SwampBoxLabel,
                    MountainBoxLabel,
                    ForestBoxLabel,
                    GenericBoxLabel("Deck Building"),
                    AwaitingCatalogizationBoxLabel(1),
                    AwaitingCatalogizationBoxLabel(2),
                    AwaitingCollectionBoxLabel(1),
                    AwaitingCollectionBoxLabel(2),
                    PlanechaseBoxLabel(1),
                    PlanechaseBoxLabel(2),
                    DuplicateBoxLabel("neo", "nec", "bbd"),
                    DuplicateBoxLabel("mkm"),
                    DuplicateBoxLabel("m21"),
                    DuplicateBoxLabel("dom"),
                    DuplicateBoxLabel("m20"),
                )

                val fontColor = Color.BLACK
                val fontFamilyPlanewalker = PDType0Font.load(this, javaClass.getResourceAsStream("/fonts/PlanewalkerBold-xZj5.ttf"))
                val fontFamily72Black = PDType0Font.load(this, javaClass.getResourceAsStream("/fonts/72-Black.ttf"))

                val pageFormat = PDRectangle.A4

                val columns = 3
                val rows = 2
                val itemsPerPage = columns * rows

                val columnWidth = pageFormat.width/columns
                val rowHeight = pageFormat.height/rows

                val margin = 12f
                val defaultGapSize = 5f

                val foldingLine = 90f * dotsPerMillimeter // FIXME measure real foldline position

                val bannerHeight = 15f*dotsPerMillimeter - margin

                val fontCode = Font(fontFamilyPlanewalker, bannerHeight * 0.7f)
                val fontCode2 = Font(fontFamily72Black, bannerHeight * 0.7f)

                val maximumWidth = columnWidth - 2*margin
                val desiredHeight = bannerHeight * 0.9f
                val maximumIconWidth = bannerHeight
                val offsetX = (maximumWidth - maximumIconWidth) * 0.5f

                labels.chunked(itemsPerPage).forEachIndexed { pageIndex, separatorItems ->
                    page(pageFormat) {
                        println("column separator page #$pageIndex")
                        separatorItems.chunked(columns).forEachIndexed { rowIndex, rowItems ->
                            rowItems.forEachIndexed { columnIndex, columnItem ->
                                val borderWidth = 2f
                                frame(columnWidth*columnIndex, rowHeight*rowIndex, (columnWidth)*(columns-columnIndex-1), (rowHeight)*(rows-rowIndex-1)) {
                                    drawBorder(borderWidth, fontColor)
                                    contentStream.apply {
                                        setLineWidth(1f)
                                        setStrokingColor(Color.GRAY)
                                        addLine(box.lowerLeftX, box.lowerLeftY + foldingLine, box.upperRightX, box.lowerLeftY + foldingLine)
                                        stroke()
                                    }
                                    frame(margin, margin, margin, margin) {
                                        when (columnItem) {
                                            is BlankBoxLabel -> {}
                                            is OneSymbolBoxLabel -> {
                                                drawText(
                                                    columnItem.title,
                                                    fontCode,
                                                    HorizontalAlignment.LEFT,
                                                    maximumIconWidth + defaultGapSize,
                                                    box.height + fontCode.descent,
                                                    fontColor
                                                )
                                                columnItem.icon?.toPDImage(this@page)
                                                    ?.let {
                                                        drawAsImageLeft(it, maximumIconWidth, desiredHeight, 0f, box.height-bannerHeight)
                                                    }
                                            }
                                            is MultipleSymbolBoxLabel -> {
                                                drawText(
                                                    columnItem.title,
                                                    fontCode2,
                                                    HorizontalAlignment.LEFT,
                                                    0f,
                                                    box.height + fontCode.descent,
                                                    fontColor
                                                )
                                                val width = fontCode2.getWidth(columnItem.title)
                                                columnItem.icons.map { it.toPDImage(this@page) }.forEachIndexed { i, icon ->
                                                    drawAsImageLeft(icon, maximumIconWidth, desiredHeight, width + maximumIconWidth * i, box.height-bannerHeight)
                                                }
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
    BoxLabels().printLabel("C:\\Users\\001121673\\BoxLabels.pdf")
}
