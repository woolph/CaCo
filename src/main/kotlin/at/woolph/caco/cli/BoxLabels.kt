package at.woolph.caco.cli

import at.woolph.caco.binderlabels.*
import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.libs.pdf.*
import be.quodlibet.boxable.HorizontalAlignment
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.net.URI
import java.nio.file.Path

interface BoxLabel {
    val title: String
    val subtitle: String? get() = null
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

object CommanderStapelsBoxLabel: OneSymbolBoxLabel {
    override val title: String = "CMD Staples"
    override val icon: ByteArray? by lazy { URI("https://c2.scryfall.com/file/scryfall-symbols/sets/cmd.svg?1647835200").renderSvgAsMythic() }
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

interface DualSymbolBoxLabel: BoxLabel {
    val icon: ByteArray?
    val icon2: ByteArray?
}

object SnowCoveredBasicsAndWastes: OneSymbolBoxLabel {
    override val title = "Snow Basics"
    override val subtitle = "and Wastes"
    override val icon: ByteArray? by lazy { URI("https://svgs.scryfall.io/card-symbols/S.svg").renderSvg() }
}

class AwaitingCatalogizationBoxLabel(index: Int? = null, override val subtitle: String? = null): OneSymbolBoxLabel {
    override val title = "Catalogize ${index ?: ""}"
    override val icon: ByteArray? by lazy { URI("https://svgs.scryfall.io/sets/wth.svg").renderSvgAsMythic() }
}

class ArtSeriesLabel(index: Int? = null, override val subtitle: String? = null): OneSymbolBoxLabel {
    override val title = "Art Series ${index ?: ""}"
    override val icon: ByteArray? by lazy { URI("https://svgs.scryfall.io/sets/pbook.svg").renderSvgAsMythic() }
}

class AwaitingCollectionBoxLabel(index: Int, override val subtitle: String? = null): OneSymbolBoxLabel {
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
object PromoBoxLabel: MultipleSymbolBoxLabel {
    override val title: String = "DUP"
    override val icons: List<ByteArray> by lazy { listOf(
        URI("https://c2.scryfall.com/file/scryfall-symbols/sets/star.svg?1624852800").renderSvgAsMythic()!!
    ) }
}

class BoxLabels {
    fun printLabel(file: Path, labels: List<BoxLabel>) {
        transaction {
            createPdfDocument(file) {
                val fontColor = Color.BLACK

                val fontFamilyPlanewalker = loadType0Font(javaClass.getResourceAsStream("/fonts/PlanewalkerBold-xZj5.ttf")!!)
                val fontFamily72Black = loadType0Font(javaClass.getResourceAsStream("/fonts/72-Black.ttf")!!)

                val pageFormat = PDRectangle.A4

                val columns = 3
                val rows = 2
                val itemsPerPage = columns * rows

                val columnWidth = pageFormat.width/columns
                val rowHeight = pageFormat.height/rows

                val margin = 12f
                val defaultGapSize = 5f

                val foldingLine = 80.8f * 90f/89.8f * dotsPerMillimeter

                val bannerHeight = 15f* dotsPerMillimeter - margin

                val fontCode = Font(fontFamilyPlanewalker, bannerHeight * 0.7f)
                val fontCode2 = Font(fontFamily72Black, bannerHeight * 0.7f)
                val fontSubtitle = Font(fontFamilyPlanewalker, 6f)

                val maximumWidth = columnWidth - 2*margin
                val desiredHeight = bannerHeight * 0.8f
                val maximumIconWidth = bannerHeight

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
                                        moveTo(box.lowerLeftX, box.lowerLeftY + foldingLine)
                                        lineTo(box.upperRightX, box.lowerLeftY + foldingLine)
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
                                                        drawAsImageLeft(it, maximumIconWidth, desiredHeight, 0f, box.height - desiredHeight)
                                                    }
                                                columnItem.subtitle?.let {
                                                    drawText(
                                                        it,
                                                        fontSubtitle,
                                                        HorizontalAlignment.LEFT,
                                                        maximumIconWidth + defaultGapSize + 8f,
                                                        box.height + fontSubtitle.descent + 2f,
                                                        fontColor
                                                    )

                                                }
                                            }
                                            is DualSymbolBoxLabel -> {
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
                                                        drawAsImageLeft(it, maximumIconWidth, desiredHeight, 0f, box.height - desiredHeight)
                                                    }
                                                columnItem.icon2?.toPDImage(this@page)
                                                    ?.let {
                                                        drawAsImageRight(it, maximumIconWidth, desiredHeight, 0f, box.height - desiredHeight)
                                                    }
                                                columnItem.subtitle?.let {
                                                    drawText(
                                                        it,
                                                        fontSubtitle,
                                                        HorizontalAlignment.LEFT,
                                                        maximumIconWidth + defaultGapSize + 8f,
                                                        box.height + fontSubtitle.descent + 2f,
                                                        fontColor
                                                    )
                                                }
                                            }
                                            is MultipleSymbolBoxLabel -> {
                                                drawText(
                                                    columnItem.title,
                                                    fontCode2,
                                                    HorizontalAlignment.LEFT,
                                                    0f,
                                                    box.height + fontCode.descent + 2f,
                                                    fontColor
                                                )
                                                val width = fontCode2.getWidth(columnItem.title) + defaultGapSize
                                                val multiIconHeight = desiredHeight * 0.5f
                                                val multiIconWidth = maximumIconWidth * 0.5f
                                                val gapX = 3.0f
                                                val gapY = multiIconHeight*0.67f

                                                if (columnItem.icons.size <= 4) {
                                                    columnItem.icons.map { it.toPDImage(this@page) }
                                                        .forEachIndexed { i, icon ->
                                                            drawAsImageLeft(
                                                                icon,
                                                                maximumIconWidth,
                                                                desiredHeight,
                                                                width + maximumIconWidth * i,
                                                                box.height - desiredHeight
                                                            )
                                                        }
                                                } else {
                                                    columnItem.icons.map { it.toPDImage(this@page) }
                                                        .forEachIndexed { i, icon ->
                                                            val displacingY = if (i%2 == 0) gapY else 0.0f
                                                            val displacingX = (multiIconWidth * 0.5f + gapX) * i

                                                            drawAsImageLeft(
                                                                icon,
                                                                multiIconWidth,
                                                                multiIconHeight,
                                                                width + displacingX,
                                                                box.height - multiIconHeight - displacingY
                                                            )
                                                        }
                                                }
                                                columnItem.subtitle?.let {
                                                    drawText(
                                                        it,
                                                        fontSubtitle,
                                                        HorizontalAlignment.LEFT,
                                                        8f,
                                                        box.height + fontSubtitle.descent + 2f,
                                                        fontColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun main() {
    val romanNumerals = mapOf(
        1 to "I",
        2 to "II",
        3 to "III",
        4 to "IV",
        5 to "V",
        6 to "VI",
        7 to "VII",
        8 to "VIII",
        9 to "IX",
        10 to "X",
        11 to "XI",
        12 to "XII",
    )
    Databases.init()
    BoxLabels().printLabel(Path.of("C:\\Users\\001121673\\private\\magic\\BoxLabels.pdf"), listOf(
//                    PlainsBoxLabel,
//                    IslandBoxLabel,
//                    SwampBoxLabel,
//                    MountainBoxLabel,
//                    ForestBoxLabel,
//                    GenericBoxLabel("Deck Building"),
//                    AwaitingCatalogizationBoxLabel(1),
//                    AwaitingCatalogizationBoxLabel(2),
//                    AwaitingCollectionBoxLabel(1),
//                    AwaitingCollectionBoxLabel(2),
//                    PlanechaseBoxLabel(1),
//                    PlanechaseBoxLabel(2),
//        CommanderStapelsBoxLabel,
//                    DuplicateBoxLabel("bbd"),
//                    DuplicateBoxLabel("khm"),
//                    DuplicateBoxLabel("mh2"),
//
//        DuplicateBoxLabel("rix"),
//        DuplicateBoxLabel("rix", "kld", "aer", "akh", "hou"),
//        DuplicateBoxLabel("soi", "emn"),
//        object: DuplicateBoxLabel("m21") {
//            override val subtitle: String = "Core Set 2021 Pt. I"
//        },
//        object: DuplicateBoxLabel("m21") {
//            override val subtitle: String = "Core Set 2021 Pt. II"
//        },
//        DuplicateBoxLabel("mid"),
//        DuplicateBoxLabel("vow"),
//        DuplicateBoxLabel("neo"),
//        DuplicateBoxLabel("bbd"),
//        DuplicateBoxLabel("afr"),
//        PromoBoxLabel,
//        AwaitingCatalogizationBoxLabel(subtitle = "Lands, Double-Sided Tokens & Placeholder"),
//        AwaitingCollectionBoxLabel(1, "Older sets (w/o binder)"),
//        AwaitingCollectionBoxLabel(2, "Older sets (w/o binder)"),
//        AwaitingCollectionBoxLabel(3, "Older sets (w/o binder)"),
//        AwaitingCollectionBoxLabel(4, "Commander sets (w/o binder)"),
//        AwaitingCollectionBoxLabel(5, "Masters & Duel Decks"),
//        SnowCoveredBasicsAndWastes,
//        ArtSeriesLabel(),
        object: DuplicateBoxLabel("por", "p02", "ptk", "usg", "ulg", "uds", "inv", "pls", "apc", "mmq", "nem", "pcy") {
            override val subtitle: String = "Older Sets"
        },
        object: DuplicateBoxLabel( "ust", "unh", "unf", "hop", "pc2", "pca", "jmp", "j22", "j25", "gnt", "gn2", "gn3") {
            override val subtitle: String = "Weird Sets"
        },
        object: DuplicateBoxLabel("2ed", "3ed", "4ed", "5ed", "6ed", "7ed", "8ed", "9ed", "itp") {
            override val subtitle: String = "White-Bordered Sets"
        },
        *(listOf("dft", "fdn", "dsk", "blb", "big", "otp", "otj", "clu", "mkm", "rex", "lci", "woe", "mat", "mul", "mom", "one", "bot", "bro", "dmu", "snc", "neo",
            ).reversed().chunked(12).withIndex().map { (i, it) ->
            object: DuplicateBoxLabel(*it.toTypedArray()) {
                override val subtitle: String = "Standard Sets ${romanNumerals[i+1]}"
            }
        }).toTypedArray(),
        object: DuplicateBoxLabel( "pspl", "spg",){
            override val subtitle: String = "Promos & Special Guests"
        },
        object: DuplicateBoxLabel("c16", "c17", "c18", "c19", "c20", "c21", "cm2", "cmr", "cmm", "clb", "m3c", "scd", ) {
            override val subtitle: String = "Commander Sets I"
        },
        DuplicateBoxLabel("stx", "sta"),
        object: DuplicateBoxLabel(  "mid", "vow"){
            override val subtitle: String = "incl. Innistrad: Double Feature"
        },
        DuplicateBoxLabel("mh1", "mh2", "mh3", ),
        object: GenericBoxLabel("Deck Building") {
            override val subtitle: String = "Commander"
            override val icon: ByteArray? by lazy { URI("https://c2.scryfall.com/file/scryfall-symbols/sets/cmd.svg?1647835200").renderSvgAsMythic() }
        },
        object: GenericBoxLabel("Deck Building") {
            override val subtitle: String = "Pioneer & Pauper"
        },
//        *(listOf("fdc", "dsc", "blc" , "otc", "mkc", "lcc", "woc", "moc", "onc", "brc", "dmc", "ncc", "nec", "voc", "mic", "afc", "khc", "znc").reversed().chunked(12).withIndex().map { (i, it) ->
//            object: DuplicateBoxLabel(*it.toTypedArray()) {
//                override val subtitle: String = "Commander Sets ${romanNumerals[i+2]}"
//            }
//        }).toTypedArray(),
//        object: DuplicateBoxLabel("pip", "who", "40k", "ltr", "ltc", "acr") {
//            override val subtitle: String = "UB Sets"
//        },
    ))
}
