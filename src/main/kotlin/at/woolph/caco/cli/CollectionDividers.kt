package at.woolph.caco.cli

import at.woolph.caco.binderlabels.*
import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.libs.pdf.*
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.net.URI
import java.nio.file.Path


interface CollectionDivider {
    val title: String
    val subtitle: String? get() = null
}

interface OneSymbolCollectionDivider: CollectionDivider {
    val code: String
    val icon: ByteArray?
}

object PromosCollectionDivider: OneSymbolCollectionDivider {
    override val code: String = "PRM"
    override val title: String = "Promos & Specials"
    override val icon: ByteArray? by lazy { URI("https://c2.scryfall.com/file/scryfall-symbols/sets/star.svg?1624852800").renderSvgAsMythic() }
}

object BlankCollectionDivider: OneSymbolCollectionDivider {
    override val code: String = ""
    override val title: String = ""
    override val icon: ByteArray? = null
}

open class GenericCollectionDivider(override val code: String, override val title: String): OneSymbolCollectionDivider {
    override val icon: ByteArray? by lazy { URI("https://c2.scryfall.com/file/scryfall-symbols/sets/default.svg?1647835200").renderSvgAsMythic() }
}

open class SimpleSetCollectionDivider(override val code: String): OneSymbolCollectionDivider {
    private val set = transaction {
        ScryfallCardSet.findByCode(code) ?: throw IllegalArgumentException("no set with code $code found")
    }
    override val title: String get() = set.name
    override val icon: ByteArray? by lazy { set.icon.renderSvgAsMythic() }
}

interface MultipleSymbolCollectionDivider: CollectionDivider {
    val icons: List<ByteArray>
}

open class BlockCollectionDivider(override val title: String, vararg codes: String): MultipleSymbolCollectionDivider {
    private val sets = fetchCardSets(*codes)
//    override val title: String = "Block"
    override val icons: List<ByteArray> by lazy { sets.mapNotNull { it.icon.renderSvgAsMythic() } }
}

class PileSeparators {
    fun printLabel(file: String) {
        Databases.init()

        transaction {
            createPdfDocument {
                val separators: List<CollectionDivider> = listOf(
//                    PromosCollectionDivider,
//                    SimpleSetCollectionDivider("neo"),
//                    SimpleSetCollectionDivider("mkm"),
//                    SimpleSetCollectionDivider("m21"),
//                    SimpleSetCollectionDivider("dom"),
//                    SimpleSetCollectionDivider("m20"),
//                    SimpleSetCollectionDivider("m19"),
//                    SimpleSetCollectionDivider("rna"),
//                    SimpleSetCollectionDivider("grn"),
//                    SimpleSetCollectionDivider("war"),
//                    SimpleSetCollectionDivider("arn"),
//                    SimpleSetCollectionDivider("atq"),
//                    SimpleSetCollectionDivider("leg"),
//                    SimpleSetCollectionDivider("drk"),
//                    SimpleSetCollectionDivider("fem"),
//                    BlockCollectionDivider("Ice Age Block", "ice", "all", "hml", "csp"),
//                    BlockCollectionDivider("Mirage Block", "mir", "vis", "wth"),
//                    BlockCollectionDivider("Tempest Block", "tmp", "sth", "exo"),
//                    BlockCollectionDivider("Odyssey Block", "ody", "tor", "jud"),
//                    BlockCollectionDivider("Onslaught Block", "ons", "lgn", "scg"),
//                    BlockCollectionDivider("Mirrodin Block", "mrd", "dst", "5dn"),
//                    BlockCollectionDivider("Kamigawa Block", "chk", "bok", "sok"),
//                    BlockCollectionDivider("Ravnica Block", "rav", "gpt", "dis"),
//                    BlockCollectionDivider("Time Spiral Block", "tsp", "plc", "fut"),
//                    BlockCollectionDivider("Lorwyn Block", "lrw", "mor"),
//                    BlockCollectionDivider("Shadowmoor Block", "shm", "eve"),
//                    BlockCollectionDivider("Alara Block", "ala", "con", "arb"),
//                    BlockCollectionDivider("Zendikar Block", "zen", "wwk", "roe"),
//                    BlockCollectionDivider("Scars of Mirrodin Block", "som", "mbs", "nph"),
//                    BlockCollectionDivider("Innistrad Block", "isd", "dka", "avr"),
//                    BlockCollectionDivider("Return to Ravnica Block", "rtr", "gtc", "dgm"),
//                    BlockCollectionDivider("Theros Block", "ths", "bng", "jou"),
//                    BlockCollectionDivider("Khans of Tarkir Block", "ktk", "frf", "dtk"),
//                    BlockCollectionDivider("Battle for Zendikar Tarkir Block", "bfz", "ogw"),
//
//                    // Commander sets
//                    SimpleSetCollectionDivider("cmd"),
//                    SimpleSetCollectionDivider("cm1"),
//                    SimpleSetCollectionDivider("c13"),
//                    SimpleSetCollectionDivider("c14"),
//                    SimpleSetCollectionDivider("c15"),
//                    SimpleSetCollectionDivider("c16"),
//                    SimpleSetCollectionDivider("cma"),
//                    SimpleSetCollectionDivider("c17"),
//
//                    // Core sets
//                    SimpleSetCollectionDivider("lea"),
//                    SimpleSetCollectionDivider("leb"),
//                    SimpleSetCollectionDivider("2ed"),
//                    SimpleSetCollectionDivider("3ed"),
//                    SimpleSetCollectionDivider("4ed"),
//                    SimpleSetCollectionDivider("5ed"),
//                    SimpleSetCollectionDivider("6ed"),
//                    SimpleSetCollectionDivider("8ed"),
//                    SimpleSetCollectionDivider("9ed"),
//                    SimpleSetCollectionDivider("10e"),
//                    SimpleSetCollectionDivider("m10"),
//                    SimpleSetCollectionDivider("m11"),
//                    SimpleSetCollectionDivider("m12"),
//                    SimpleSetCollectionDivider("m13"),
//                    SimpleSetCollectionDivider("m14"),
//                    SimpleSetCollectionDivider("m15"),
//                    SimpleSetCollectionDivider("ori"),
//
//                    // Masters sets
//                    SimpleSetCollectionDivider("mma"),
//                    SimpleSetCollectionDivider("mm2"),
//                    SimpleSetCollectionDivider("ema"),
//                    SimpleSetCollectionDivider("mm3"),
//                    SimpleSetCollectionDivider("ima"),
//                    SimpleSetCollectionDivider("2xm"),
//                    SimpleSetCollectionDivider("2x2"),
//
//

                    // missing
                    SimpleSetCollectionDivider("c18"),
                    SimpleSetCollectionDivider("c19"),
                    SimpleSetCollectionDivider("c20"),
                    SimpleSetCollectionDivider("c21"),
                    SimpleSetCollectionDivider("cmm"),
                    SimpleSetCollectionDivider("scd"),

                    SimpleSetCollectionDivider("uma"),

                    SimpleSetCollectionDivider("7ed"),
                    SimpleSetCollectionDivider("m19"),
                    SimpleSetCollectionDivider("m20"),
                    SimpleSetCollectionDivider("m21"),

                    BlockCollectionDivider("Urza's Block", "usg", "ulg", "uds"),
                    BlockCollectionDivider("Masques Block", "mmq", "nem", "pcy"),
                    BlockCollectionDivider("Invasion Block", "inv", "pls", "apc"),
                    BlockCollectionDivider("Shadows over Innistrad Block", "soi", "emn"),
                    BlockCollectionDivider("Kaladesh Block", "kld", "aer"),
                    BlockCollectionDivider("Amonketh Block", "akh", "hou"),
                    BlockCollectionDivider("Ixalan Block", "xln", "rix"),

                    SimpleSetCollectionDivider("dom"),
                    SimpleSetCollectionDivider("dft"),
                    SimpleSetCollectionDivider("drc"),
                    SimpleSetCollectionDivider("drc"),
                    SimpleSetCollectionDivider("inr"),
                    SimpleSetCollectionDivider("dmr"),
                    SimpleSetCollectionDivider("tsr"),
                    SimpleSetCollectionDivider("tdm"),
                    SimpleSetCollectionDivider("fdn"),
                    SimpleSetCollectionDivider("dsk"),
                    SimpleSetCollectionDivider("dsc"),
                    SimpleSetCollectionDivider("blb"),
                    SimpleSetCollectionDivider("blc"),
                    SimpleSetCollectionDivider("acr"),
                    SimpleSetCollectionDivider("pip"),
                    SimpleSetCollectionDivider("who"),
                    SimpleSetCollectionDivider("ltr"),
                    SimpleSetCollectionDivider("ltc"),
                    SimpleSetCollectionDivider("mh3"),
                    SimpleSetCollectionDivider("otj"),
                    SimpleSetCollectionDivider("otc"),
                    SimpleSetCollectionDivider("mkm"),
                    SimpleSetCollectionDivider("mkc"),
                    SimpleSetCollectionDivider("rvr"),
                    SimpleSetCollectionDivider("lci"),
                    SimpleSetCollectionDivider("lcc"),
                    SimpleSetCollectionDivider("woe"),
                    SimpleSetCollectionDivider("woc"),
                    SimpleSetCollectionDivider("mom"),
                    SimpleSetCollectionDivider("moc"),
                    SimpleSetCollectionDivider("one"),
                    SimpleSetCollectionDivider("onc"),
                    SimpleSetCollectionDivider("bro"),
                    SimpleSetCollectionDivider("brc"),
                    SimpleSetCollectionDivider("unf"),
                    SimpleSetCollectionDivider("dmu"),
                    SimpleSetCollectionDivider("dmc"),
                    SimpleSetCollectionDivider("clb"),
                    SimpleSetCollectionDivider("snc"),
                    SimpleSetCollectionDivider("ncc"),
                    SimpleSetCollectionDivider("neo"),
                    SimpleSetCollectionDivider("nec"),
                )

                val fontColor = Color.BLACK
                val fontFamilyPlanewalker = loadType0Font(javaClass.getResourceAsStream("/fonts/PlanewalkerBold-xZj5.ttf")!!)
                val fontFamily72Black = PDType0Font.load(this.document, javaClass.getResourceAsStream("/fonts/72-Black.ttf"))

                val pageFormat = PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width)

                val columns = 4
                val rows = 2
                val itemsPerPage = columns * rows

                val columnWidth = pageFormat.width/columns
                val rowHeight = pageFormat.height/rows

                val magicCardHeight = 88f* dotsPerMillimeter

                val margin = 12f
                val defaultGapSize = 5f

                val bannerHeight = rowHeight - magicCardHeight - margin

                val fontCode = Font(fontFamily72Black, bannerHeight * 0.9f)
                val fontCode2 = Font(fontFamilyPlanewalker, 8f)

                val maximumWidth = columnWidth - 2*margin
                val desiredHeight = bannerHeight * 0.9f
                val maximumIconWidth = bannerHeight
                val offsetX = (maximumWidth - maximumIconWidth) * 0.5f

                separators.chunked(itemsPerPage).forEachIndexed { pageIndex, separatorItems ->
                    page(pageFormat) {
                        println("collection dividers page #$pageIndex")
                        separatorItems.chunked(columns).forEachIndexed { rowIndex, rowItems ->
                            rowItems.forEachIndexed { columnIndex, columnItem ->
                                val borderWidth = 2f
                                frame(columnWidth*columnIndex, rowHeight*rowIndex, (columnWidth)*(columns-columnIndex-1), (rowHeight)*(rows-rowIndex-1)) {
                                    drawBorder(borderWidth, fontColor)
                                    frame(margin, margin, margin, margin) {
                                        when (columnItem) {
                                            is BlankCollectionDivider -> {}
                                            is OneSymbolCollectionDivider -> {
                                                drawText(
                                                    columnItem.title,
                                                    fontCode2,
                                                    HorizontalAlignment.CENTER,
                                                    0f,
                                                    desiredHeight + fontCode2.height,
                                                    fontColor
                                                )
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
                                            is MultipleSymbolCollectionDivider -> {
                                                drawText(
                                                    columnItem.title,
                                                    fontCode2,
                                                    HorizontalAlignment.CENTER,
                                                    0f,
                                                    desiredHeight + fontCode2.height,
                                                    fontColor
                                                )
                                                val start = box.width * 0.5f - maximumIconWidth * columnItem.icons.count() * 0.5f
                                                columnItem.icons.map { it.toPDImage(this@page) }.forEachIndexed { i, icon ->
                                                    drawAsImageLeft(icon, maximumIconWidth, desiredHeight, start + maximumIconWidth * i, 0f)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                save(Path.of(file))
            }
        }
    }
}

fun main() {
    PileSeparators().printLabel(".\\CollectionDividers.pdf")
}
