package at.woolph.caco.cli

import at.woolph.caco.binderlabels.*
import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.CardSets
import at.woolph.caco.drawAsImage
import at.woolph.caco.imagecache.ImageCache
import at.woolph.caco.toPDImage
import at.woolph.caco.view.collection.CardModel
import at.woolph.libs.pdf.*
import be.quodlibet.boxable.HorizontalAlignment
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.util.*

class CollectionPagePreview {
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun printLabel(file: String) {
        Databases.init()

        val setCode = "dmr"

        val cardList = transaction {
            CardSet.findById(setCode)?.cards ?: emptyList()
        }.sortedBy { it.numberInSet }.asSequence().map{ Optional.of(it) }

        val collectionPages = sequenceOf(
            generateSequence { Optional.empty<Card>() }.take(9),
            cardList,
        ).flatten().chunked(18)

        createPdfDocument {
            val titleAdjustment = TitleAdjustment.FONT_SIZE
            val pageFormat = PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width)

            val fontColor = if (darkLabels) Color.WHITE else Color.BLACK
            val fontSubColor = if (darkLabels) Color.LIGHT_GRAY else Color.GRAY
            val backgroundColor: Color? = if (darkLabels) Color.BLACK else null

            val mtgCardBack = PDImageXObject.createFromFile("./card-back.jpg", this)

            val fontFamilyPlanewalker = PDType0Font.load(this, javaClass.getResourceAsStream("/fonts/PlanewalkerBold-xZj5.ttf"))
            val fontFamily72Black = PDType0Font.load(this, javaClass.getResourceAsStream("/fonts/72-Black.ttf"))

            val fontTitle = Font(fontFamilyPlanewalker, 48f)
            val fontSubTitle = Font(fontFamilyPlanewalker, 16f)
            val fontCode = Font(fontFamily72Black, 28f)
            val fontCodeCommanderSubset = Font(fontFamily72Black, 14f)

            data class Position(
                val x: Float,
                val y: Float,
            ) {
                operator fun plus(p: Position) = Position(x + p.x, y + p.y)
                operator fun times(d: Float) = Position(x * d, y * d)
                operator fun times(p: Position) = Position(x * p.x, y * p.y)
                operator fun div(d: Float) = Position(x / d, y / d)
                operator fun div(p: Position) = Position(x / p.x, y / p.y)
                operator fun minus(p: Position) = Position(x - p.x, y -p.y)
                operator fun unaryPlus() = this
                operator fun unaryMinus() = Position(-x, -y)
            }
            fun PDRectangle.toPosition() = Position(width, height)

            val margin = Position(10.0f, 10.0f)
            val pageGap = 10.0f

            val pageSize = (pageFormat.toPosition() - (margin * 2.0f) - Position(pageGap, 0f)) / Position(2.0f, 1.0f)
            val rows = 3
            val columns = 3
            val cardGap = Position(5.0f, 5.0f)
            val cardSize = Position((pageSize.x - cardGap.x * (columns - 1))/columns, (pageSize.y - cardGap.y * (rows - 1))/rows)

            val pageOffset = Position(pageSize.x + pageGap, 0f)
            val cardOffset = Position(cardSize.x + cardGap.x, cardSize.y + cardGap.y)


            fun position(index: Int): Position {
                val page = when(index) {
                    in 0 ..< 9 -> 0
                    in 9 ..< 18 -> 1
                    else -> throw IllegalArgumentException("index $index is not between 0 ..< 18")
                }

                val row = when(index - 9 * page) {
                    in 0 ..< 3 -> 0
                    in 3 ..< 6 -> 1
                    in 6 ..< 9 -> 2
                    else -> throw IllegalStateException()
                }

                val column = index - 9 * page - 3 * row
                require(column in 0 ..< 3)

                return margin +
                        pageOffset * page.toFloat() +
                        cardOffset * Position(column.toFloat(), rows - row.toFloat() - 1)
            }
            collectionPages.forEach { pageContent ->
                page(pageFormat) {
                    pageContent.forEachIndexed { index, cardOptional ->
                        cardOptional.ifPresent { card ->
                            val cardPosition = position(index)
                            try {
                                println("card #$${card.numberInSet} ${card.name} image is being downloaded")
                                val byteArray = runBlocking {
                                    ImageCache.getImageByteArray(card.image.toString()) {
                                        try {
                                            card.image?.toURL()?.readBytes()
                                        } catch (t: Throwable) {
                                            println("unable to load")
                                            null
                                        }
                                    }
                                }!!
                                val cardImage = PDImageXObject.createFromByteArray(this@createPdfDocument, byteArray, card.name)
                                drawImage(cardImage, cardPosition.x, cardPosition.y, cardSize.x, cardSize.y)
                                println("card #\$${card.numberInSet} ${card.name} image is rendered")
                            } catch (t: Throwable) {
                                drawImage(mtgCardBack, cardPosition.x, cardPosition.y, cardSize.x, cardSize.y)
                                println("card #\$${card.numberInSet} ${card.name} cardback rendered")
                            }
                        }
                    }
                }
            }


//            mapLabels.forEach { (columns, labels) ->
//                val columnWidth = pageFormat.width/columns
//
//                val magicLogoHPadding = 10f
//                val titleXPosition = (columnWidth + fontTitle.size)*0.5f - 10f //64f
//                val titleYPosition = 164f
//                val subTitleXPosition = titleXPosition+22f
//                val subTitleYPosition = titleYPosition+30f
//
//                val maximumWidth = columnWidth-20f
//                val desiredHeight = 64f
//
//                val mtgLogoWidth = columnWidth-2*magicLogoHPadding
//                val mtgLogoHeight = mtgLogo.height.toFloat()/mtgLogo.width.toFloat()*mtgLogoWidth
//
//                val magicLogoYPosition = 842f-14f-mtgLogoHeight
//                val maxTitleWidth = magicLogoYPosition-titleYPosition
//
//                for(pageIndex in 0.. (labels.size-1)/columns) {
//                    page(pageFormat) {
//                        println("column label page #$pageIndex")
//                        val columnWidth = box.width/columns
//
//                        for (i in 0 until columns) {
//                            labels.getOrNull(columns*pageIndex + i)?.let { set ->
//                                if (set != BlankLabel) {
//                                    frame(columnWidth*i+1f, 0f, (columnWidth)*(columns-i-1)+1f, 0f) {
//                                        backgroundColor?.let { drawBackground(it) }
//                                        drawBorder(1f, fontColor)
//
//                                        drawImage(mtgLogo, magicLogoHPadding + columnWidth*i, magicLogoYPosition, mtgLogoWidth, mtgLogoHeight)
//
//                                        set.subTitle?.let { _subTitle ->
//                                            when(titleAdjustment) {
//                                                TitleAdjustment.TEXT_CUT -> {
//                                                    var title = _subTitle
//                                                    while (fontTitle.getWidth(title) > maxTitleWidth) {
//                                                        title = title.substring(0, title.length-4) + "..."
//                                                    }
//                                                    drawText90(title, fontSubTitle, subTitleXPosition + columnWidth*i, subTitleYPosition, fontSubColor)
//                                                }
//                                                TitleAdjustment.FONT_SIZE -> {
//                                                    var fontSubTitleAdjusted = fontSubTitle
//                                                    while (fontSubTitleAdjusted.getWidth(_subTitle) > maxTitleWidth) {
//                                                        fontSubTitleAdjusted = fontSubTitleAdjusted.relative(0.95f)
//                                                    }
//                                                    drawText90(_subTitle, fontSubTitleAdjusted, subTitleXPosition + columnWidth*i, subTitleYPosition, fontSubColor)
//                                                }
//                                            }
//                                        }
//
//                                        when(titleAdjustment) {
//                                            TitleAdjustment.TEXT_CUT -> {
//                                                var title = set.title
//                                                while (fontTitle.getWidth(title) > maxTitleWidth) {
//                                                    title = title.substring(0, title.length-4) + "..."
//                                                }
//                                                drawText90(title, fontTitle, titleXPosition + columnWidth*i, titleYPosition, fontColor)
//                                            }
//                                            TitleAdjustment.FONT_SIZE -> {
//                                                var fontTitleAdjusted = fontTitle
//                                                while (fontTitleAdjusted.getWidth(set.title) > maxTitleWidth) {
//                                                    fontTitleAdjusted = fontTitleAdjusted.relative(0.95f)
//                                                }
//                                                drawText90(set.title, fontTitleAdjusted, titleXPosition + columnWidth*i, titleYPosition, fontColor)
//                                            }
//                                        }
//
//                                        set.mainIcon?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidth, desiredHeight, i, columnWidth, 0f, 68f) }
//
////										set.mainIcon?.let { PDImageXObject.createFromByteArray(this@createPdfDocument, it ,null) }?.let { image ->
////											val heightScale = desiredHeight/image.height
////											val desiredWidth = image.width*heightScale
////											val (actualWidth, actualHeight) = if (desiredWidth > maximumWidth) {
////												maximumWidth to image.height*maximumWidth/image.width
////											} else {
////												desiredWidth to desiredHeight
////											}
////											drawImage(image, 10f + columnWidth*i+(maximumWidth-actualWidth)*0.5f, 68f+(desiredHeight-actualHeight)*0.5f, actualWidth, actualHeight)
////										}
//
//                                        frame(5f, 842f-120f+15f, 5f, 5f) {
//                                            //								drawBackground(Color.WHITE)
//                                            //								drawBorder(1f, Color.BLACK)
//                                            drawText(set.code.uppercase(), fontCode, HorizontalAlignment.CENTER, 55f, fontColor)
//                                            set.subCode?.let { subCode ->
//                                                //											drawText("&", fontCodeCommanderSubset.relative(0.75f), HorizontalAlignment.CENTER, 32f, fontSubColor)
//                                                drawText(subCode.uppercase(), fontCodeCommanderSubset, HorizontalAlignment.CENTER, 25f, fontSubColor)
//                                            }
//
//                                            val maximumWidthSub = 20f
//                                            val desiredHeightSub = 20f
//                                            val xOffsetIcons = 32f
//                                            val yOffsetIcons = 58f
//
//                                            set.subIconRight?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidthSub, desiredHeightSub, i, columnWidth, xOffsetIcons, yOffsetIcons) }
//                                            set.subIconLeft?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidthSub, desiredHeightSub, i, columnWidth, -xOffsetIcons, yOffsetIcons) }
//
//                                            val xOffsetIcons2 = 32f+15f
//                                            val yOffsetIcons2 = 58f+25f
//
//                                            set.subIconRight2?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidthSub, desiredHeightSub, i, columnWidth, xOffsetIcons2, yOffsetIcons2) }
//                                            set.subIconLeft2?.toPDImage(this@page)?.let { drawAsImage(it, maximumWidthSub, desiredHeightSub, i, columnWidth, -xOffsetIcons2, yOffsetIcons2) }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
            save(file)
        }
    }
}

//fun ByteArray.toPDImage(page: Page) = PDImageXObject.createFromByteArray(page.document, this ,null)
//
//fun Node.drawAsImage(subIcon: PDImageXObject, maximumWidth: Float, desiredHeight: Float, i: Int, columnWidth: Float, xOffsetIcons: Float, yOffsetIcons: Float) {
//    val heightScale = desiredHeight/subIcon.height
//    val desiredWidth = subIcon.width*heightScale
//    val (actualWidth, actualHeight) = if (desiredWidth > maximumWidth) {
//        maximumWidth to subIcon.height*maximumWidth/subIcon.width
//    } else {
//        desiredWidth to desiredHeight
//    }
//    drawImage(subIcon, columnWidth*0.5f + xOffsetIcons + columnWidth*i - actualWidth*0.5f, yOffsetIcons+(desiredHeight-actualHeight)*0.5f, actualWidth, actualHeight)
//}

suspend fun main() {
    CollectionPagePreview().printLabel("C:\\Users\\001121673\\POR.pdf")
}
