package at.woolph.caco.cli

import at.woolph.caco.binderlabels.*
import at.woolph.caco.datamodel.Databases
import at.woolph.libs.pdf.*
import be.quodlibet.boxable.HorizontalAlignment
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.nio.file.Path

class BinderLabels(
//    val inputReader: LineReader,
//    val output: OutputStream,
) {
    fun printLabel(file: Path) {
        Databases.init()

        transaction {
            createPdfDocument(file) {
                val labelsWide : List<MapLabelItem> = listOf(
                )
                val labelsNarrow: List<MapLabelItem> = listOf(
                    PromosLabel,
                    SetWithCommander("neo", "nec"),
                    SetWithCommander("khm", "khc"),
                    SetWithCommander("znr", "znc"),
                    SetWithCommander("mid", "mic"),
                    SetWithCommander("vow", "voc"),
                    SimpleSet("afc"),
                    SimpleSet("c18"),
                    SimpleSet("c19"),
                    SimpleSet("c20"),
                    SimpleSet("c21"),
                    SimpleSet("cmr"),
                    SimpleSet("cm2"),
                    SimpleSet("m19"),
                    SimpleSet("m20"),
                    SimpleSet("m21"),
                    SimpleSet("afr"),
                    SimpleSet("stx"),
                    SimpleSet("iko"),
                    SimpleSet("dom"),
                    SimpleSet("thb"),
                    SimpleSet("eld"),
                    SimpleSet("war"),
                    object: SimpleSet("rna") {
                        override val subTitle = "incl. Guild Kit"
                    },
                    object: SimpleSet("grn") {
                        override val subTitle = "incl. Guild Kit"
                    },
                    TwoSetBlock("Conspiracy Sets", "cns", "cn2"),
                    SimpleSet("tsr"),
                    SimpleSet("uma"),
                    SimpleSet("2xm"),
                    SimpleSet("mh1"),
                    SimpleSet("mh2"),
                    SimpleSet("a25"),
                    SimpleSet("ima"),
                    SimpleSet("mm3"),
                    SimpleSet("ema"),
                    SimpleSet("mm2"),
                    SimpleSet("mma"),
                    SimpleSet("4ed"),
                    SimpleSet("5ed"),
                    SimpleSet("6ed"),
                    SimpleSet("jmp"),
                    SimpleSet("ust"),
                    SimpleSet("unf"),
                    ThreeSetBlock("Portal Sets", "por", "p02", "ptk"),
                    ThreeSetBlock("Urza Block", "usg", "ulg", "uds"),
                    ThreeSetBlock("Tempest Block", "tmp", "sth", "exo"),
                    ThreeSetBlock("Mirage Block", "mir", "vis", "wth"),
                    ThreeSetBlock("Mercadian Block", "mmq", "nem", "pcy"),
                    ThreeSetBlock("Invasion Block", "inv", "pls", "apc"),
                    TwoSetBlock("Ixalan Block", "xln", "rix"),
                    TwoSetBlock("Amonkhet Block", "akh", "hou"),
                    TwoSetBlock("Kaladesh Block", "kld", "aer"),
                    TwoSetBlock("Shadow over Innistrad Block", "soi", "emn"),
                    GenericLabel("misc", "Miscellaneous"),
                    SimpleSet("bbd"),
                    ThreeSetBlock("Planechase Sets", "hop", "pc2","pca"),
                    ThreeSetBlock("Odyssey Block", "ody", "tor", "jud"),
                    TwoSetBlock("Battle for Zendikar Block", "bfz", "ogw"),
                    SimpleSet("ori"),
                    SimpleSet("c17"),
                    SimpleSet("snc"),
                    SimpleSet("c16"),
                    SimpleSet("c15"),
                    SimpleSet("c14"),
                    SimpleSet("c13"),
                    SimpleSet("cmd"),
                    SimpleSet("cma"),
                    ThreeSetBlock("Khans of Tarkir Block", "ktk", "frf", "dtk"),
                    SimpleSet("m15"),
                    ThreeSetBlock("Theros Block", "ths", "bng", "jou"),
                    SimpleSet("m14"),
                    ThreeSetBlock("Return to Ravnica Block", "rtr", "gtc", "dgm"),
                    SimpleSet("m13"),
                    ThreeSetBlock("Innistrad Block", "isd", "dka", "avr"),
                    SimpleSet("m12"),
                    ThreeSetBlock("Scars of Mirrodin Block", "som", "mbs", "nph"),
                    SimpleSet("m11"),
                    ThreeSetBlock("Zendikar Block", "zen", "wwk", "roe"),
                    SimpleSet("m10"),
                    ThreeSetBlock("Alara Block", "ala", "con", "arb"),
                    TwoSetBlock("Shadowmoor Block", "shm", "eve"),
                    TwoSetBlock("Lorwyn Block", "lrw", "mor"),
                    SimpleSet("10e"),
                    ThreeSetBlock("Time Spiral Block", "tsp", "plc", "fut"),
                    ThreeSetBlock("Ravnica Block", "rav", "gpt", "dis"),
                    SimpleSet("9ed"),
                    ThreeSetBlock("Kamigawa Block", "chk", "bok", "sok"),
                    ThreeSetBlock("Mirrodin Block", "mrd", "dst", "5dn"),
                    SimpleSet("8ed"),
                    ThreeSetBlock("Onslaught Block", "ons", "lgn", "scg"),
                    SimpleSet("7ed"),
                    ThreeSetBlock("Ice Age Block", "ice", "all", "csp"),
                    SetWithCommanderAndAncillary("znr", "znc", "zne"),
                    SetWithCommander("stx", "sta"),
                    SimpleSet("dmr"),
                    SimpleSet("j22"),
                    SimpleSet("ugl"),
                    SimpleSet("unh"),
                    SimpleSet("und"),
                    SimpleSet("ust"),
                    SimpleSet("unf"),
                    SimpleSet("gnt"),
                    SimpleSet("gn2"),
                    SimpleSet("gn3"),
                    SetWithCommanderAndAncillary("bro", "brc", "brr"),
                    SetWithCommander("one", "onc"),
                    SimpleSet("who"),
                    SimpleSet("ltc"),
                    SimpleSet("cmm"),
                    SimpleSet("scd"),
                    SimpleSet("otc"),
                    SimpleSet("ltr"),
                    SimpleSet("clb"),
                    BlankLabel,
                    SetWithCommanderAndAncillary("mom", "mat", "mul"),
                    SimpleSet("moc"),
                    SetWithCommander("lci",  "rex"),
                    SimpleSet("lcc"),
                    SimpleSet("rvr"),
                    SetWithCommanderAndAncillary("woe", "woc", "wot"),
                    SimpleSet("pip"),
                    SetWithCommander("mkm", "clu"),
                    SimpleSet("mkc"),
                    SimpleSet("mh3"),
                    SetWithCommanderAndAncillary("otj", "otp", "big"),
                    SimpleSet("spg"),
                )

                val mapLabels = mapOf(
//                    4 to labelsWide,
                    6 to labelsNarrow,
                )

                val fontColor = if (darkLabels) Color.WHITE else Color.BLACK
                val fontSubColor = if (darkLabels) Color.LIGHT_GRAY else Color.GRAY
                val backgroundColor: Color? = if (darkLabels) Color.BLACK else null

                val mtgLogo = createFromFile(Path.of("./mtg.png"))

                val fontFamilyPlanewalker = loadType0Font(javaClass.getResourceAsStream("/fonts/PlanewalkerBold-xZj5.ttf")!!)
                val fontFamily72Black = loadType0Font(javaClass.getResourceAsStream("/fonts/72-Black.ttf")!!)

                val fontTitle = Font(fontFamilyPlanewalker, 48f)
                val fontSubTitle = Font(fontFamilyPlanewalker, 16f)
                val fontCode = Font(fontFamily72Black, 28f)
                val fontCodeCommanderSubset = Font(fontFamily72Black, 14f)

                val pageFormat = PDRectangle.A4

                mapLabels.forEach { (columns, labels) ->
                    val columnWidth = pageFormat.width/columns
                    val margin = 12f

                    val maximumWidth = columnWidth-2*margin
                    val maximumHeight = pageFormat.height-2*margin
                    val desiredHeight = 64f

                    val mtgLogoWidth = maximumWidth
                    val mtgLogoHeight = mtgLogo.height.toFloat()/mtgLogo.width.toFloat()*mtgLogoWidth
                    val magicLogoYPosition = 0f

                    val defaultGapSize = 5f
                    val mainIconYPos = 0f
                    val codeYPos = mainIconYPos + desiredHeight + fontCode.totalHeight + defaultGapSize
                    val subCodeYPos = codeYPos + fontCodeCommanderSubset.totalHeight

                    val setDivHeight = desiredHeight + fontCode.totalHeight + fontCodeCommanderSubset.totalHeight + defaultGapSize
                    val setDivYPos = maximumHeight - setDivHeight

                    val maxTitleWidth = pageFormat.height - magicLogoYPosition - mtgLogoHeight - defaultGapSize - setDivHeight - 2 * margin - 2 * defaultGapSize

                    val titleXPosition = (maximumWidth + fontTitle.height) * 0.5f
                    val titleYPosition = setDivYPos - 3 * defaultGapSize
                    val subTitleXPosition = titleXPosition + fontSubTitle.height + defaultGapSize
                    val subTitleYPosition = titleYPosition - 30f


                    columnedContent(labels, pageFormat, columns) { set ->
                        drawBorder(2f, fontColor)
                        if (set != BlankLabel) {
                            backgroundColor?.let { drawBackground(it) }

                            frame(margin, margin, margin, margin) {
                                drawAsImageCentered(
                                    mtgLogo,
                                    mtgLogoWidth,
                                    mtgLogoHeight,
                                    0f,
                                    magicLogoYPosition,
                                )

                                set.subTitle?.let { _subTitle ->
                                    adjustTextToFitWidth(
                                        _subTitle,
                                        fontSubTitle,
                                        maxTitleWidth,
                                        10f
                                    ).let { (title, font) ->
                                        drawText(
                                            title,
                                            font,
                                            fontSubColor,
                                            subTitleXPosition,
                                            subTitleYPosition,
                                            90.0
                                        )
                                    }
                                }

                                adjustTextToFitWidth(
                                    set.title,
                                    fontTitle,
                                    maxTitleWidth,
                                    36f
                                ).let { (title, font) ->
                                    drawText(title, font, fontColor, titleXPosition, titleYPosition, 90.0)
                                }

                                frame(0f, setDivYPos, 0f, 0f) {
                                    drawText(
                                        set.code.uppercase(),
                                        fontCode,
                                        HorizontalAlignment.CENTER,
                                        0f,
                                        codeYPos,
                                        fontColor
                                    )

                                    set.subCode?.let { subCode ->
                                        drawText(
                                            subCode.uppercase(),
                                            fontCodeCommanderSubset,
                                            HorizontalAlignment.CENTER,
                                            0f,
                                            subCodeYPos,
                                            fontSubColor
                                        )
                                    }

                                    val maximumWidthSub = 20f
                                    val desiredHeightSub = 20f
                                    val xOffsetIcons = 27f
                                    val yOffsetIcons = mainIconYPos + desiredHeight - 15f

                                    set.subIconRight?.toPDImage(this)?.let {
                                        drawAsImageCentered(
                                            it,
                                            maximumWidthSub,
                                            desiredHeightSub,
                                            xOffsetIcons,
                                            yOffsetIcons
                                        )
                                    }
                                    set.subIconLeft?.toPDImage(this)?.let {
                                        drawAsImageCentered(
                                            it,
                                            maximumWidthSub,
                                            desiredHeightSub,
                                            -xOffsetIcons,
                                            yOffsetIcons
                                        )
                                    }

                                    val xOffsetIcons2 = 32f + 15f
                                    val yOffsetIcons2 = mainIconYPos + 10f

                                    set.subIconRight2?.toPDImage(this)?.let {
                                        drawAsImageCentered(
                                            it,
                                            maximumWidthSub,
                                            desiredHeightSub,
                                            xOffsetIcons2,
                                            yOffsetIcons2
                                        )
                                    }
                                    set.subIconLeft2?.toPDImage(this)?.let {
                                        drawAsImageCentered(
                                            it,
                                            maximumWidthSub,
                                            desiredHeightSub,
                                            -xOffsetIcons2,
                                            yOffsetIcons2
                                        )
                                    }

                                    set.mainIcon?.toPDImage(this)?.let {
                                        drawAsImageCentered(
                                            it,
                                            maximumWidth,
                                            desiredHeight,
                                            0f,
                                            mainIconYPos
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


fun main() {
    BinderLabels().printLabel(Path.of("C:\\Users\\001121673\\private\\magic\\BinderLabels.pdf"))
}
