package at.woolph.caco.cli

import at.woolph.caco.binderlabels.*
import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.libs.pdf.*
import org.apache.pdfbox.pdmodel.common.PDRectangle
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

class ReturnToCollectionBoxLabel(override val subtitle: String? = "Returning Destructed Decks"): OneSymbolBoxLabel {
    override val title = "To Be Filed"
    override val icon: ByteArray? by lazy { URI("https://svgs.scryfall.io/sets/ath.svg").renderSvgAsMythic() }
}

class AwaitingCollectionBoxLabel(index: Int, override val subtitle: String? = null): OneSymbolBoxLabel {
    override val title = "To Be Filed $index"
    override val icon: ByteArray? by lazy { URI("https://svgs.scryfall.io/sets/ath.svg").renderSvgAsMythic() }
}

interface MultipleSymbolBoxLabel: BoxLabel {
    val rows: Int
    val icons: List<ByteArray?>
}

class CollectionBoxLabel(override val subtitle: String? = null, vararg codes: String?): MultipleSymbolBoxLabel {
    private val sets = transaction {
        codes.map {
            it?.let { CardSet.findById(it) ?: throw IllegalArgumentException("no set with code $it found") }
        }
    }
    override val rows: Int = 3
    override val title: String = "COL"
    override val icons: List<ByteArray?> by lazy { sets.map { it?.icon?.renderSvgAsMythic() } }
}

class DuplicateBoxLabel(override val subtitle: String? = null, vararg codes: String): MultipleSymbolBoxLabel {
    private val sets = transaction {
        codes.map {
            CardSet.findById(it) ?: throw IllegalArgumentException("no set with code $it found")
        }
    }
    override val rows: Int = when(codes.size) {
        in 0..4 -> 1
        in 5 .. 14 -> 2
        else -> 3
    }

    override val title: String = "DUP"
    override val icons: List<ByteArray> by lazy { sets.mapNotNull { it.icon.renderSvgAsMythic() } }
}

object PromoBoxLabel: MultipleSymbolBoxLabel {
    override val rows: Int = 1
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
                val maximumIconWidth = desiredHeight

                labels.chunked(itemsPerPage).forEachIndexed { pageIndex, separatorItems ->
                    page(pageFormat) {
                        println("box label page #$pageIndex")
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

                                                val iconHeight = desiredHeight / columnItem.rows
                                                val iconWidth = maximumIconWidth / columnItem.rows
                                                val gapX = iconWidth + 2f * columnItem.rows
                                                val gapXWithinColummn = iconWidth * 0.75f
                                                val gapY = iconHeight * 0.75f

                                                columnItem.icons.map { it?.toPDImage(this@page) }
                                                    .forEachIndexed { i, nullableIcon ->
                                                        nullableIcon?.let { icon ->
                                                            val row = i % columnItem.rows
                                                            val column = i / columnItem.rows

                                                            val displacingX = gapX * column + gapXWithinColummn * row
                                                            val displacingY = gapY * (columnItem.rows - row - 1)

                                                            drawAsImageLeft(
                                                                icon,
                                                                iconWidth,
                                                                iconHeight,
                                                                width + displacingX,
                                                                box.height - iconHeight - displacingY
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
    BoxLabels().printLabel(Path.of(".\\BoxLabels.pdf"), listOf(
        PlanechaseBoxLabel(1),
        PlanechaseBoxLabel(2),
        DuplicateBoxLabel("rix"),
        DuplicateBoxLabel("rix", "kld", "aer", "akh", "hou"),
        DuplicateBoxLabel("soi", "emn"),
        DuplicateBoxLabel(subtitle = "Core Set 2021 Pt. I", "m21"),
        DuplicateBoxLabel(subtitle = "Core Set 2021 Pt. II", "m21"),
        DuplicateBoxLabel("mid"),
        DuplicateBoxLabel("vow"),
        DuplicateBoxLabel("neo"),
        DuplicateBoxLabel("bbd"),
        DuplicateBoxLabel("afr"),
        AwaitingCollectionBoxLabel(1, "Older sets (w/o binder)"),
        AwaitingCollectionBoxLabel(2, "Older sets (w/o binder)"),
        AwaitingCollectionBoxLabel(3, "Older sets (w/o binder)"),
        AwaitingCollectionBoxLabel(4, "Commander sets (w/o binder)"),
        AwaitingCollectionBoxLabel(5, "Masters & Duel Decks"),
        SnowCoveredBasicsAndWastes,
        ArtSeriesLabel(),
        DuplicateBoxLabel(subtitle = "Older Sets","por", "p02", "ptk", "usg", "ulg", "uds", "inv", "pls", "apc", "mmq", "nem", "pcy"),
        DuplicateBoxLabel(subtitle = "Weird Sets", "ust", "unh", "unf", "hop", "pc2", "pca", "jmp", "j22", "j25", "gnt", "gn2", "gn3"),
        DuplicateBoxLabel(subtitle = "White-Bordered Sets", "2ed", "3ed", "4ed", "5ed", "6ed", "7ed", "8ed", "9ed", "itp"),
        DuplicateBoxLabel(subtitle = "Promos & Special Guests", "pspl", "spg",),
        DuplicateBoxLabel(subtitle = "Commander Sets I", "c16", "c17", "c18", "c19", "c20", "c21", "cm2", "cmr", "cmm", "clb", "m3c", "scd", ),
        DuplicateBoxLabel("stx", "sta"),
        DuplicateBoxLabel(subtitle = "incl. Innistrad: Double Feature",  "mid", "vow"),
        DuplicateBoxLabel("mh1", "mh2", "mh3", ),
        object: GenericBoxLabel("Deck Building") {
            override val subtitle: String = "Commander"
            override val icon: ByteArray? by lazy { URI("https://c2.scryfall.com/file/scryfall-symbols/sets/cmd.svg?1647835200").renderSvgAsMythic() }
        },
        object: GenericBoxLabel("Deck Building") {
            override val subtitle: String = "Pioneer & Pauper"
        },
        AwaitingCatalogizationBoxLabel(subtitle = "Double-Sided Tokens, Lands, Promos, & Placeholder"),
        DuplicateBoxLabel("rvr", "inr", ),
        *listOf("tdm", "dft", "fdn", "dsk", "blb", "big", "otp", "otj", "clu", "mkm", "rex", "lci", "woe", "mat", "mul", "mom", "one", "bot", "bro", "dmu", "snc", "neo",
        ).reversed().chunked(12).withIndex().map { (i, it) ->
            DuplicateBoxLabel(subtitle="Standard Sets ${romanNumerals[i+1]}", *it.toTypedArray())
        }.takeLast(1).toTypedArray(),
        *listOf(
            listOf(
            "3ed", "7ed", "8ed", "9ed", "10e", "m10", "m11", "m12", "m13", "m14", "m15",
            "ori", "a25", "ima", "ema", "mma", "mm2", "mm3", ),

            listOf(
            "fem", "arn", "leg",

            "ice", "hml", "all", "csp", null, null,  "mir", "vis", "wth",   "tmp", "sth", "exo",   "ody", "tor", "jud",
            "ons", "lgn", "scg",   "mrd", "dst", "5dn", ),

            listOf(
                "chk", "bok", "sok",   "rav", "gpt", "dis",

            "tsp", "plc", "fut",    "lrw", "mor", null,   "eve", "shm", null,   "ala", "con", "arb",
            "zen", "wwk", "roe",   "som", "mbs", "nph",   "isd", "dka", "avr",
            ),
            listOf(
            "rtr", "gtc", "dgm",   "ths", "bng", "jou",   "ktk", "frf", "dtk",
            "bfz", "ogw", null, ),
            listOf(
                "dd1", "dd2", "ddc", "ddd", "dde", "ddf", "ddg", "ddh", "ddi", "ddj", "ddk", "ddl", "ddm", "ddn",
                "ddo",  "ddp", "ddq", "ddr", "dds", "ddt", "ddu", "gs1", "pd2", "md1", "w16", "w17",
                ),
            listOf("cmd", "c13", "c14", "c15", "c16", "c17", "cma", "cmm", null, "arc", "e01", "e02",
                "drb", "v09", "v10", "v11", "v12", "v13", "v14", "v15", "v16", "v17", null, null, "dbl",
            ),
        ).withIndex().map { (i, it) ->
            CollectionBoxLabel(subtitle = "Collection ${romanNumerals[i+1]}", *it.toTypedArray())
        }.toTypedArray(),
        *listOf("tdc", "drc", "fdc", "dsc", "blc" , "otc", "mkc", "lcc", "woc", "moc", "onc", "brc", "dmc", "ncc", "nec", "voc", "mic", "afc", "khc", "znc").reversed().chunked(12).withIndex().map { (i, it) ->
            DuplicateBoxLabel(subtitle = "Commander Sets ${romanNumerals[i+2]}", *it.toTypedArray())
        }.takeLast(1).toTypedArray(),
        ReturnToCollectionBoxLabel(),
        DuplicateBoxLabel("tsr", "dmr", ),
        DuplicateBoxLabel(subtitle = "UB Sets", "pip", "who", "40k", "acr", "ltr", "ltc", "fin", "fic"),
    ).takeLast(12))
}
