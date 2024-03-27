package at.woolph.caco.cli

import at.woolph.caco.binderlabels.*
import at.woolph.caco.datamodel.Databases
import at.woolph.libs.pdf.Font
import at.woolph.libs.pdf.Node
import at.woolph.libs.pdf.Page
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
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color

enum class TitleAdjustment {
    FONT_SIZE, TEXT_CUT
}

class BinderLabels(
//    val inputReader: LineReader,
//    val output: OutputStream,
) {
    fun printLabel(file: String) {
        Databases.init()

        transaction {
            createPdfDocument {
                val titleAdjustment = TitleAdjustment.FONT_SIZE
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
                    SimpleSet("clb"),
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
                    SimpleSet("scd"),
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
                    SimpleSet("ltr"),
                    SimpleSet("ltc"),
                    SimpleSet("cmm"),
                    SimpleSet("scd"),
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

                val fontColor = if (darkLabels) Color.WHITE else Color.BLACK
                val fontSubColor = if (darkLabels) Color.LIGHT_GRAY else Color.GRAY
                val backgroundColor: Color? = if (darkLabels) Color.BLACK else null

                val mtgLogo = PDImageXObject.createFromFile("./mtg.png", this)

                val fontFamilyPlanewalker = PDType0Font.load(this, javaClass.getResourceAsStream("/fonts/PlanewalkerBold-xZj5.ttf"))
                val fontFamily72Black = PDType0Font.load(this, javaClass.getResourceAsStream("/fonts/72-Black.ttf"))

                val fontTitle = Font(fontFamilyPlanewalker, 48f)
                val fontSubTitle = Font(fontFamilyPlanewalker, 16f)
                val fontCode = Font(fontFamily72Black, 28f)
                val fontCodeCommanderSubset = Font(fontFamily72Black, 14f)

                val mapLabels = mapOf(
//                    4 to labelsWide,
                    6 to labelsNarrow,
                )

                val pageFormat = PDRectangle.A4

                mapLabels.forEach { (columns, labels) ->
                    val columnWidth = pageFormat.width/columns

                    val titleXPosition = (columnWidth + fontTitle.size)*0.5f - 10f //64f
                    val titleYPosition = 164f
                    val subTitleXPosition = titleXPosition+22f
                    val subTitleYPosition = titleYPosition+30f

                    val maximumWidth = columnWidth-20f
                    val desiredHeight = 64f
                    val marginTop = 15f
                    val marginBottom = marginTop

                    val magicLogoHPadding = 10f
                    val magicLogoVPadding = marginTop
                    val mtgLogoWidth = columnWidth-2*magicLogoHPadding
                    val mtgLogoHeight = mtgLogo.height.toFloat()/mtgLogo.width.toFloat()*mtgLogoWidth
                    val magicLogoYPosition = 842f-magicLogoVPadding-mtgLogoHeight

                    val maxTitleWidth = magicLogoYPosition-titleYPosition

                    val subCodeYPos = marginBottom + fontCodeCommanderSubset.height
                    val codeYPos = subCodeYPos + fontCode.height
                    val mainIconYPos = codeYPos + 17f

                    labels.chunked(columns).forEachIndexed { pageIndex, mapLabelItems ->
                        page(pageFormat) {
                            println("column label page #$pageIndex")
                            mapLabelItems.forEachIndexed { i, set ->
                                if (set != BlankLabel) {
                                    frame(columnWidth*i+1f, 0f, (columnWidth)*(columns-i-1)+1f, 0f) {
                                        backgroundColor?.let { drawBackground(it) }
                                        drawBorder(2f, fontColor)

                                        drawImage(mtgLogo, magicLogoHPadding + columnWidth*i, magicLogoYPosition, mtgLogoWidth, mtgLogoHeight)

                                        set.subTitle?.let { _subTitle ->
                                            when(titleAdjustment) {
                                                TitleAdjustment.TEXT_CUT -> {
                                                    var title = _subTitle
                                                    while (fontTitle.getWidth(title) > maxTitleWidth) {
                                                        title = title.substring(0, title.length-4) + "..."
                                                    }
                                                    drawText90(title, fontSubTitle, subTitleXPosition + columnWidth*i, subTitleYPosition, fontSubColor)
                                                }
                                                TitleAdjustment.FONT_SIZE -> {
                                                    var fontSubTitleAdjusted = fontSubTitle
                                                    while (fontSubTitleAdjusted.getWidth(_subTitle) > maxTitleWidth) {
                                                        fontSubTitleAdjusted = fontSubTitleAdjusted.relative(0.95f)
                                                    }
                                                    drawText90(_subTitle, fontSubTitleAdjusted, subTitleXPosition + columnWidth*i, subTitleYPosition, fontSubColor)
                                                }
                                            }
                                        }

                                        when(titleAdjustment) {
                                            TitleAdjustment.TEXT_CUT -> {
                                                var title = set.title
                                                while (fontTitle.getWidth(title) > maxTitleWidth) {
                                                    title = title.substring(0, title.length-4) + "..."
                                                }
                                                drawText90(title, fontTitle, titleXPosition + columnWidth*i, titleYPosition, fontColor)
                                            }
                                            TitleAdjustment.FONT_SIZE -> {
                                                var fontTitleAdjusted = fontTitle
                                                while (fontTitleAdjusted.getWidth(set.title) > maxTitleWidth) {
                                                    fontTitleAdjusted = fontTitleAdjusted.relative(0.95f)
                                                }
                                                drawText90(set.title, fontTitleAdjusted, titleXPosition + columnWidth*i, titleYPosition, fontColor)
                                            }
                                        }

                                        frame(5f, 842f-120f+15f, 5f, 5f) {
                                            drawText(set.code.uppercase(), fontCode, HorizontalAlignment.CENTER, codeYPos, fontColor)
                                            set.subCode?.let { subCode ->
                                                drawText(subCode.uppercase(), fontCodeCommanderSubset, HorizontalAlignment.CENTER, subCodeYPos, fontSubColor)
                                            }

                                            val maximumWidthSub = 20f
                                            val desiredHeightSub = 20f
                                            val xOffsetIcons = 27f
                                            val yOffsetIcons = mainIconYPos - 10f

                                            set.subIconRight?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidthSub, desiredHeightSub, i, columnWidth, xOffsetIcons, yOffsetIcons) }
                                            set.subIconLeft?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidthSub, desiredHeightSub, i, columnWidth, -xOffsetIcons, yOffsetIcons) }

                                            val xOffsetIcons2 = 32f+15f
                                            val yOffsetIcons2 = mainIconYPos + 15f

                                            set.subIconRight2?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidthSub, desiredHeightSub, i, columnWidth, xOffsetIcons2, yOffsetIcons2) }
                                            set.subIconLeft2?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidthSub, desiredHeightSub, i, columnWidth, -xOffsetIcons2, yOffsetIcons2) }
                                        }

                                        set.mainIcon?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidth, desiredHeight, i, columnWidth, 0f, mainIconYPos) }

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

fun ByteArray.toPDImage(page: Page) = PDImageXObject.createFromByteArray(page.document, this ,null)

fun Node.drawAsImage(subIcon: PDImageXObject, maximumWidth: Float, desiredHeight: Float, i: Int, columnWidth: Float, xOffsetIcons: Float, yOffsetIcons: Float) {
    val heightScale = desiredHeight/subIcon.height
    val desiredWidth = subIcon.width*heightScale
    val (actualWidth, actualHeight) = if (desiredWidth > maximumWidth) {
        maximumWidth to subIcon.height*maximumWidth/subIcon.width
    } else {
        desiredWidth to desiredHeight
    }
    drawImage(subIcon, columnWidth*0.5f + xOffsetIcons + columnWidth*i - actualWidth*0.5f, yOffsetIcons+(desiredHeight-actualHeight)*0.5f, actualWidth, actualHeight)
}

fun main() {
    BinderLabels().printLabel("C:\\Users\\001121673\\BinderLabels.pdf")
}
