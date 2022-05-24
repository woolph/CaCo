package at.woolph.caco

import at.woolph.caco.datamodel.collection.ArenaCardPossessions
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.decks.Builds
import at.woolph.caco.datamodel.decks.DeckArchetypes
import at.woolph.caco.datamodel.decks.DeckCards
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.CardSets
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.datamodel.sets.renderSvg
import at.woolph.libs.pdf.*
import be.quodlibet.boxable.HorizontalAlignment
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.net.URI

interface MapLabelItem {
	val code: String
	val title: String
	val mainIcon: ByteArray?
	val subCode: String?
		get() = null
	val subTitle: String?
		get() = null
	val subIconLeft: ByteArray?
		get() = null
	val subIconRight: ByteArray?
		get() = null
	val subIconLeft2: ByteArray?
		get() = null
	val subIconRight2: ByteArray?
		get() = null
}

val darkLabels = false
val iconResolution = 256f

fun URI?.renderSvgAsMythic(): ByteArray? = try { this.renderSvg(iconResolution, if (darkLabels) "gray" else "black","#c54326", "#f7971c") } catch (e: Exception) { null }
fun URI?.renderSvgAsRare(): ByteArray? = try { this.renderSvg(iconResolution, if (darkLabels) "gray" else "black", "#8d7431", "#f6db94") } catch (e: Exception) { null }
fun URI?.renderSvgAsUncommon(): ByteArray? = try { this.renderSvg(iconResolution, if (darkLabels) "gray" else "black", "#626e77", "#c8e2f2") } catch (e: Exception) { null }
fun URI?.renderSvgAsCommon(): ByteArray? = try { this.renderSvg(iconResolution, if (darkLabels) "gray" else "black", "black", "black") } catch (e: Exception) { null }

object PromosLabel: MapLabelItem {
	override val code: String = "PRM"
	override val title: String = "Promos & Specials"
	override val mainIcon: ByteArray? = URI("https://c2.scryfall.com/file/scryfall-symbols/sets/star.svg?1624852800").renderSvgAsMythic()
}

object BlankLabel: MapLabelItem {
	override val code: String = ""
	override val title: String = ""
	override val mainIcon: ByteArray? = null
}

open class GenericLabel(override val code: String, override val title: String): MapLabelItem {
	override val mainIcon: ByteArray? = URI("https://c2.scryfall.com/file/scryfall-symbols/sets/default.svg?1647835200").renderSvgAsMythic()
}

open class SimpleSet(override val code: String): MapLabelItem {
	override val title: String
	override val mainIcon: ByteArray?

	init {
		val set = transaction {
			CardSet.find(CardSets.shortName.eq(code)).singleOrNull() ?: throw IllegalArgumentException("no set with code $code found")
		}

		title = set.name
		mainIcon = set.icon.renderSvgAsMythic()
	}
}

class SetWithCommander(override val code: String, override val subCode: String): MapLabelItem {
	override val title: String
	override val mainIcon: ByteArray?
	override val subTitle: String
	override val subIconRight: ByteArray?

	init {
		val (mainSet, commanderSet) = transaction {
			arrayOf(
				CardSet.find(CardSets.shortName.eq(code)).singleOrNull() ?: throw IllegalArgumentException("no set with code $subCode found"),
				CardSet.find(CardSets.shortName.eq(subCode)).singleOrNull() ?: throw IllegalArgumentException("no set with code $subCode found"),
			)
		}

		title = mainSet.name
		mainIcon = mainSet.icon.renderSvgAsMythic()

		subTitle = "incl. ${commanderSet.name}"
		subIconRight = commanderSet.icon.renderSvgAsUncommon()
	}
}

class SetWithCommanderAndMasterPieces(override val code: String, subCode1: String, subCode2: String): MapLabelItem {
	override val title: String
	override val mainIcon: ByteArray?
	override val subTitle: String
	override val subIconRight: ByteArray?
	override val subIconLeft: ByteArray?
	override val subCode: String = "$subCode1 & $subCode2"

	init {
		val (mainSet, commanderSet, masterpieceSet) = transaction {
			arrayOf(
				CardSet.find(CardSets.shortName.eq(code)).singleOrNull() ?: throw IllegalArgumentException("no set with code $code found"),
				CardSet.find(CardSets.shortName.eq(subCode1)).singleOrNull() ?: throw IllegalArgumentException("no set with code $subCode1 found"),
				CardSet.find(CardSets.shortName.eq(subCode2)).singleOrNull() ?: throw IllegalArgumentException("no set with code $subCode2 found"),
			)
		}

		title = mainSet.name
		mainIcon = mainSet.icon.renderSvgAsMythic()

		subTitle = "incl. ${commanderSet.name} & ${masterpieceSet.name}"
		subIconLeft = commanderSet.icon.renderSvgAsUncommon()
		subIconRight = masterpieceSet.icon.renderSvgAsUncommon()
	}
}

class TwoSetBlock(blockTitle: String, code0: String, code1: String): MapLabelItem {
	override val title: String = blockTitle
	override val code: String = code0
	override val mainIcon: ByteArray?
	override val subCode: String = code1
	override val subTitle: String
	override val subIconRight: ByteArray?

	init {
		val cardSets = transaction {
			listOf(code0, code1).map { _code ->
				CardSet.find(CardSets.shortName.eq(_code)).singleOrNull() ?: throw IllegalArgumentException("no set with code $code found")
			}
		}

		subTitle = "incl. ${cardSets.joinToString(" & ") { it.name }}"
		mainIcon = cardSets.component1().icon.renderSvgAsMythic()
		subIconRight = cardSets.component2().icon.renderSvgAsUncommon()
	}
}

class ThreeSetBlock(blockTitle: String, code0: String, code1: String, code2: String): MapLabelItem {
	override val title: String = blockTitle
	override val code: String = code0
	override val mainIcon: ByteArray?
	override val subCode: String = "$code1 / $code2"
	override val subTitle: String
	override val subIconLeft: ByteArray?
	override val subIconRight: ByteArray?

	init {
		val cardSets = transaction {
			listOf(code0, code1, code2).map { _code ->
				CardSet.find(CardSets.shortName.eq(_code)).singleOrNull() ?: throw IllegalArgumentException("no set with code $code found")
			}
		}

		subTitle =  "incl. ${cardSets.dropLast(1).joinToString(", ") { it.name }}, & ${cardSets.last().name}"
		mainIcon = cardSets.component1().icon.renderSvgAsMythic()
		subIconLeft = cardSets.component2().icon.renderSvgAsUncommon()
		subIconRight = cardSets.component3().icon.renderSvgAsUncommon()
	}
}

class FiveSetBlock(blockTitle: String, code0: String, code1: String, code2: String, code3: String, code4: String): MapLabelItem {
	override val title: String = blockTitle
	override val code: String
	override val mainIcon: ByteArray?
	override val subCode: String
	override val subTitle: String
	override val subIconLeft: ByteArray?
	override val subIconRight: ByteArray?
	override val subIconLeft2: ByteArray?
	override val subIconRight2: ByteArray?

	init {
		val codes = listOf(code0, code1, code2, code3, code4)
		val cardSets = transaction {
			codes.map { _code ->
				CardSet.find(CardSets.shortName.eq(_code)).singleOrNull() ?: throw IllegalArgumentException("no set with code $_code found")
			}
		}

		code = "UN*"
		subCode = ""
//		code = codes.first()
//		subCode = codes.drop(1).joinToString("/")

		subTitle =  "incl. ${cardSets.dropLast(1).joinToString(", ") { it.name }}, & ${cardSets.last().name}"
		mainIcon = cardSets.component1().icon.renderSvgAsMythic()
		subIconLeft = cardSets.component2().icon.renderSvgAsUncommon()
		subIconRight = cardSets.component3().icon.renderSvgAsUncommon()
		subIconLeft2 = cardSets.component4().icon.renderSvgAsUncommon()
		subIconRight2 = cardSets.component5().icon.renderSvgAsUncommon()
	}
}

enum class TitleAdjustment {
	FONT_SIZE, TEXT_CUT
}

val Number.cm get(): Float = this.toFloat()/21f*595f

// a4 842 pt x 595 pt
fun main(args: Array<String>) {
	Database.connect("jdbc:h2:~/caco", driver = "org.h2.Driver")

	transaction {
		SchemaUtils.createMissingTablesAndColumns(CardSets, Cards, CardPossessions, ArenaCardPossessions, DeckArchetypes, Builds, DeckCards)
		createPdfDocument {
			val titleAdjustment = TitleAdjustment.FONT_SIZE
			val labelsWide : List<MapLabelItem> = listOf(
//				FiveSetBlock("Un-Sets", "ugl", "unh", "und", "ust", "unf"),
//				PromosLabel,
//				GenericLabel("misc1", "Miscellaneous Pt. 1"),
//				GenericLabel("misc2", "Miscellaneous Pt. 2"),
			)
			val labelsNarrow: List<MapLabelItem> = listOf(
//				PromosLabel,
//				SetWithCommander("neo", "nec"),
//				SetWithCommander("khm", "khc"),
//				SetWithCommander("znr", "znc"),
//				SetWithCommander("mid", "mic"),
//				SetWithCommander("vow", "voc"),
//				SimpleSet("afc"),
//				SimpleSet("c18"),
//				SimpleSet("c19"),
//				SimpleSet("c20"),
//				SimpleSet("c21"),
//				SimpleSet("cmr"),
//				SimpleSet("cm2"),
//				SimpleSet("m19"),
//				SimpleSet("m20"),
//				SimpleSet("m21"),
//				SimpleSet("afr"),
//				SimpleSet("stx"),
//				SimpleSet("iko"),
//				SimpleSet("dom"),
//				SimpleSet("thb"),
//				SimpleSet("eld"),
//				SimpleSet("war"),
//				object: SimpleSet("rna") {
//					override val subTitle = "incl. Guild Kit"
//				},
//				object: SimpleSet("grn") {
//					override val subTitle = "incl. Guild Kit"
//				},
//				TwoSetBlock("Conspiracy Sets", "cns", "cn2"),
//				SimpleSet("tsr"),
//				SimpleSet("uma"),
//				SimpleSet("2xm"),
//				SimpleSet("mh1"),
//				SimpleSet("mh2"),
//				SimpleSet("a25"),
//				SimpleSet("ima"),
//				SimpleSet("mm3"),
//				SimpleSet("ema"),
//				SimpleSet("mm2"),
//				SimpleSet("mma"),
//				SimpleSet("4ed"),
//				SimpleSet("5ed"),
//				SimpleSet("6ed"),
//				SimpleSet("jmp"),
//				SimpleSet("ust"),
//				SimpleSet("unf"),
//				ThreeSetBlock("Portal Sets", "por", "p02", "ptk"),
//				ThreeSetBlock("Urza Block", "usg", "ulg", "uds"),
//				ThreeSetBlock("Tempest Block", "tmp", "sth", "exo"),
//				ThreeSetBlock("Mirage Block", "mir", "vis", "wth"),
//				ThreeSetBlock("Mercadian Block", "mmq", "nem", "pcy"),
//				ThreeSetBlock("Invasion Block", "inv", "pls", "apc"),
//				TwoSetBlock("Ixalan Block", "xln", "rix"),
//				TwoSetBlock("Amonkhet Block", "akh", "hou"),
//				TwoSetBlock("Kaladesh Block", "kld", "aer"),
//				TwoSetBlock("Shadow over Innistrad Block", "soi", "emn"),
//				GenericLabel("misc", "Miscellaneous"),
//				SimpleSet("bbd"),
//				ThreeSetBlock("Planechase Sets", "hop", "pc2","pca"),
//				ThreeSetBlock("Odyssey Block", "ody", "tor", "jud"),
//				TwoSetBlock("Battle for Zendikar Block", "bfz", "ogw"),
//				SimpleSet("ori"),
//				SimpleSet("c17"),
//				SetWithCommander("stx", "sta"),
//				SetWithCommanderAndMasterPieces("znr", "znc", "zne"),
//				SimpleSet("snc"),
//				SimpleSet("ncc"),
//				SimpleSet("clb"),
//				SimpleSet("c16"),
//				SimpleSet("c15"),
//				SimpleSet("c14"),
//				SimpleSet("c13"),
//				SimpleSet("cmd"),
//				SimpleSet("cma"),
//				ThreeSetBlock("Khans of Tarkir Block", "ktk", "frf", "dtk"),
//				SimpleSet("m15"),
//				ThreeSetBlock("Theros Block", "ths", "bng", "jou"),
//				SimpleSet("m14"),
//				ThreeSetBlock("Return to Ravnica Block", "rtr", "gtc", "dgm"),
//				SimpleSet("m13"),
//				ThreeSetBlock("Innistrad Block", "isd", "dka", "avr"),
//				SimpleSet("m12"),
//				ThreeSetBlock("Scars of Mirrodin Block", "som", "mbs", "nph"),
//				SimpleSet("m11"),
//				ThreeSetBlock("Zendikar Block", "zen", "wwk", "roe"),
//				SimpleSet("m10"),
//				ThreeSetBlock("Alara Block", "ala", "con", "arb"),
//				TwoSetBlock("Shadowmoor Block", "shm", "eve"),
//				TwoSetBlock("Lorwyn Block", "lrw", "mor"),
//				SimpleSet("10e"),
//				ThreeSetBlock("Time Spiral Block", "tsp", "plc", "fut"),
//				ThreeSetBlock("Ravnica Block", "rav", "gpt", "dis"),
//				SimpleSet("9ed"),
				ThreeSetBlock("Kamigawa Block", "chk", "bok", "sok"),
				ThreeSetBlock("Mirrodin Block", "mrd", "dst", "5dn"),
				SimpleSet("8ed"),
				ThreeSetBlock("Onslaught Block", "ons", "lgn", "scg"),
				SimpleSet("7ed"),
				ThreeSetBlock("Ice Age Block", "ice", "all", "csp"),
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

			val mapLabels = mapOf(4 to labelsWide, 6 to labelsNarrow)

			val pageFormat = PDRectangle.A4

			mapLabels.forEach { (columns, labels) ->
				val columnWidth = pageFormat.width/columns

				val magicLogoHPadding = 10f
				val titleXPosition = (columnWidth + fontTitle.size)*0.5f - 10f //64f
				val titleYPosition = 164f
				val subTitleXPosition = titleXPosition+22f
				val subTitleYPosition = titleYPosition+30f

				val maximumWidth = columnWidth-20f
				val desiredHeight = 64f

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

										set.mainIcon?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidth, desiredHeight, i, columnWidth, 0f, 68f) }

//										set.mainIcon?.let { PDImageXObject.createFromByteArray(this@createPdfDocument, it ,null) }?.let { image ->
//											val heightScale = desiredHeight/image.height
//											val desiredWidth = image.width*heightScale
//											val (actualWidth, actualHeight) = if (desiredWidth > maximumWidth) {
//												maximumWidth to image.height*maximumWidth/image.width
//											} else {
//												desiredWidth to desiredHeight
//											}
//											drawImage(image, 10f + columnWidth*i+(maximumWidth-actualWidth)*0.5f, 68f+(desiredHeight-actualHeight)*0.5f, actualWidth, actualHeight)
//										}

										frame(5f, 842f-120f+15f, 5f, 5f) {
											//								drawBackground(Color.WHITE)
											//								drawBorder(1f, Color.BLACK)
											drawText(set.code.uppercase(), fontCode, HorizontalAlignment.CENTER, 55f, fontColor)
											set.subCode?.let { subCode ->
												//											drawText("&", fontCodeCommanderSubset.relative(0.75f), HorizontalAlignment.CENTER, 32f, fontSubColor)
												drawText(subCode.uppercase(), fontCodeCommanderSubset, HorizontalAlignment.CENTER, 25f, fontSubColor)
											}

											val maximumWidthSub = 20f
											val desiredHeightSub = 20f
											val xOffsetIcons = 32f
											val yOffsetIcons = 58f

											set.subIconRight?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidthSub, desiredHeightSub, i, columnWidth, xOffsetIcons, yOffsetIcons) }
											set.subIconLeft?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidthSub, desiredHeightSub, i, columnWidth, -xOffsetIcons, yOffsetIcons) }

											val xOffsetIcons2 = 32f+15f
											val yOffsetIcons2 = 58f+25f

											set.subIconRight2?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidthSub, desiredHeightSub, i, columnWidth, xOffsetIcons2, yOffsetIcons2) }
											set.subIconLeft2?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidthSub, desiredHeightSub, i, columnWidth, -xOffsetIcons2, yOffsetIcons2) }
										}
									}
								}
							}
						}
					}
				}
			}
			save("BinderLabels.pdf")
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
