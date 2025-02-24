package at.woolph.caco.cli

import at.woolph.caco.binderlabels.*
import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.libs.pdf.*
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.net.URI
import java.nio.file.Path


interface PileSeparator {
    val title: String
    val subtitle: String? get() = null
}

interface OneSymbolPileSeparator: PileSeparator {
    val code: String
    val icon: ByteArray?
}

object PromosPileSeparator: OneSymbolPileSeparator {
    override val code: String = "PRM"
    override val title: String = "Promos & Specials"
    override val icon: ByteArray? by lazy { URI("https://c2.scryfall.com/file/scryfall-symbols/sets/star.svg?1624852800").renderSvgAsMythic() }
}

object BlankPileSeparator: OneSymbolPileSeparator {
    override val code: String = ""
    override val title: String = ""
    override val icon: ByteArray? = null
}

open class GenericPileSeparator(override val code: String, override val title: String): OneSymbolPileSeparator {
    override val icon: ByteArray? by lazy { URI("https://c2.scryfall.com/file/scryfall-symbols/sets/default.svg?1647835200").renderSvgAsMythic() }
}

open class SimpleSetPileSeparator(override val code: String): OneSymbolPileSeparator {
    private val set = transaction {
        CardSet.findById(code) ?: throw IllegalArgumentException("no set with code $code found")
    }
    override val title: String get() = set.name
    override val icon: ByteArray? by lazy { set.icon.renderSvgAsMythic() }
}

interface MultipleSymbolPileSeparator: PileSeparator {
    val icons: List<ByteArray>
}

open class BlockPileSeparator(override val title: String, vararg codes: String): MultipleSymbolPileSeparator {
    private val sets = transaction {
        codes.map {
            CardSet.findById(it) ?: throw IllegalArgumentException("no set with code $it found")
        }
    }
//    override val title: String = "Block"
    override val icons: List<ByteArray> by lazy { sets.mapNotNull { it.icon.renderSvgAsMythic() } }
}

class PileSeparators {
    fun printLabel(file: String) {
        Databases.init()

        transaction {
            createPdfDocument {
                val separators: List<PileSeparator> = listOf(
//                    PromosPileSeparator,
//                    SimpleSetPileSeparator("neo"),
//                    SimpleSetPileSeparator("mkm"),
//                    SimpleSetPileSeparator("m21"),
//                    SimpleSetPileSeparator("dom"),
//                    SimpleSetPileSeparator("m20"),
//                    SimpleSetPileSeparator("m19"),
//                    SimpleSetPileSeparator("rna"),
//                    SimpleSetPileSeparator("grn"),
//                    SimpleSetPileSeparator("war"),
                    SimpleSetPileSeparator("arn"),
                    SimpleSetPileSeparator("atq"),
                    SimpleSetPileSeparator("leg"),
                    SimpleSetPileSeparator("drk"),
                    SimpleSetPileSeparator("fem"),
                    BlockPileSeparator("Ice Age Block", "ice", "all", "hml", "csp"),
                    BlockPileSeparator("Mirage Block", "mir", "vis", "wth"),
                    BlockPileSeparator("Tempest Block", "tmp", "sth", "exo"),
                    BlockPileSeparator("Odyssey Block", "ody", "tor", "jud"),
                    BlockPileSeparator("Onslaught Block", "ons", "lgn", "scg"),
                    BlockPileSeparator("Mirrodin Block", "mrd", "dst", "5dn"),
                    BlockPileSeparator("Kamigawa Block", "chk", "bok", "sok"),
                    BlockPileSeparator("Ravnica Block", "rav", "gpt", "dis"),
                    BlockPileSeparator("Time Spiral Block", "tsp", "plc", "fut"),
                    BlockPileSeparator("Lorwyn Block", "lrw", "mor"),
                    BlockPileSeparator("Shadowmoor Block", "shm", "eve"),
                    BlockPileSeparator("Alara Block", "ala", "con", "arb"),
                    BlockPileSeparator("Zendikar Block", "zen", "wwk", "roe"),
                    BlockPileSeparator("Scars of Mirrodin Block", "som", "mbs", "nph"),
                    BlockPileSeparator("Innistrad Block", "isd", "dka", "avr"),
                    BlockPileSeparator("Return to Ravnica Block", "rtr", "gtc", "dgm"),
                    BlockPileSeparator("Theros Block", "ths", "bng", "jou"),
                    BlockPileSeparator("Khans of Tarkir Block", "ktk", "frf", "dtk"),
                    BlockPileSeparator("Battle for Zendikar Tarkir Block", "bfz", "ogw"),

                    // Commander sets
                    SimpleSetPileSeparator("cmd"),
                    SimpleSetPileSeparator("cm1"),
                    SimpleSetPileSeparator("c13"),
                    SimpleSetPileSeparator("c14"),
                    SimpleSetPileSeparator("c15"),
                    SimpleSetPileSeparator("c16"),
                    SimpleSetPileSeparator("cma"),
                    SimpleSetPileSeparator("c17"),

                    // Core sets
                    SimpleSetPileSeparator("lea"),
                    SimpleSetPileSeparator("leb"),
                    SimpleSetPileSeparator("2ed"),
                    SimpleSetPileSeparator("3ed"),
                    SimpleSetPileSeparator("4ed"),
                    SimpleSetPileSeparator("5ed"),
                    SimpleSetPileSeparator("6ed"),
                    SimpleSetPileSeparator("8ed"),
                    SimpleSetPileSeparator("9ed"),
                    SimpleSetPileSeparator("10e"),
                    SimpleSetPileSeparator("m10"),
                    SimpleSetPileSeparator("m11"),
                    SimpleSetPileSeparator("m12"),
                    SimpleSetPileSeparator("m13"),
                    SimpleSetPileSeparator("m14"),
                    SimpleSetPileSeparator("m15"),
                    SimpleSetPileSeparator("ori"),

                    // Masters sets
                    SimpleSetPileSeparator("mma"),
                    SimpleSetPileSeparator("mm2"),
                    SimpleSetPileSeparator("ema"),
                    SimpleSetPileSeparator("mm3"),
                    SimpleSetPileSeparator("ima"),
                    SimpleSetPileSeparator("2xm"),
                    SimpleSetPileSeparator("2x2"),



                    // missing
                    SimpleSetPileSeparator("c18"),
                    SimpleSetPileSeparator("c19"),
                    SimpleSetPileSeparator("c20"),
                    SimpleSetPileSeparator("c21"),
                    SimpleSetPileSeparator("cmm"),
                    SimpleSetPileSeparator("scd"),

                    SimpleSetPileSeparator("uma"),

                    SimpleSetPileSeparator("7ed"),
                    SimpleSetPileSeparator("m19"),
                    SimpleSetPileSeparator("m20"),
                    SimpleSetPileSeparator("m21"),

                    BlockPileSeparator("Urza's Block", "usg", "ulg", "uds"),
                    BlockPileSeparator("Masques Block", "mmq", "nem", "pcy"),
                    BlockPileSeparator("Invasion Block", "inv", "pls", "apc"),
                    BlockPileSeparator("Shadows over Innistrad Block", "soi", "emn"),
                    BlockPileSeparator("Kaladesh Block", "kld", "aer"),
                    BlockPileSeparator("Amonketh Block", "akh", "hou"),
                    BlockPileSeparator("Ixalan Block", "xln", "rix"),

                    SimpleSetPileSeparator("dom"),
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
                        println("column separator page #$pageIndex")
                        separatorItems.chunked(columns).forEachIndexed { rowIndex, rowItems ->
                            rowItems.forEachIndexed { columnIndex, columnItem ->
                                val borderWidth = 2f
                                frame(columnWidth*columnIndex, rowHeight*rowIndex, (columnWidth)*(columns-columnIndex-1), (rowHeight)*(rows-rowIndex-1)) {
                                    drawBorder(borderWidth, fontColor)
                                    frame(margin, margin, margin, margin) {
                                        when (columnItem) {
                                            is BlankPileSeparator -> {}
                                            is OneSymbolPileSeparator -> {
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
                                            is MultipleSymbolPileSeparator -> {
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
    PileSeparators().printLabel("C:\\Users\\001121673\\private\\magic\\CardSeparators.pdf")
}
