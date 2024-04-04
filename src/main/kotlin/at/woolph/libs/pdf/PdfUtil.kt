package at.woolph.libs.pdf

import be.quodlibet.boxable.HorizontalAlignment
import be.quodlibet.boxable.VerticalAlignment
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import org.apache.pdfbox.util.Matrix
import java.awt.Color
import java.io.Closeable
import java.io.IOException
import java.nio.file.Path
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt


class ColumnSpace(val columnManager: ColumnManager, val column: Int, val line: Int) {
	fun drawText(text: String, color: Color, lineIndent: Float = 0f) {
		val x = columnManager.node.box.lowerLeftX+lineIndent + column*(columnManager.maxLineWidth+columnManager.columnGap)
		val y = columnManager.node.box.upperRightY - line*columnManager.lineHeight

		columnManager.node.drawText(text, columnManager.font, x, y, color, columnManager.maxLineWidth-lineIndent)
	}

	fun drawTextWithRects(text: String, countEN: Int, countDE: Int) {
		columnManager.apply {
			val max = 4
			val rectLineWidth = 1f
			val rectSize = font.size-rectLineWidth*2
			val rectGap = 2f
			val textGap = 2f
			val symbolMore = "+"
			val symbolWidth = font.getWidth(symbolMore)
			val lineIndent = (rectSize+rectGap)*max+textGap+symbolWidth

			val more = (countEN+countDE)>max
			val enoughEN = countEN>=max
			val enoughTotal = (countEN+countDE)>=max
			val color = if(enoughEN) Color.LIGHT_GRAY else if(enoughTotal) Color.MAGENTA else Color.BLACK

			columnManager.node.apply {
				contentStream.apply {
					val y = box.upperRightY - line * lineHeight

					setLineWidth(rectLineWidth)
					setStrokingColor(Color.BLACK)

					for (i in 0 until max) {
						val x = box.lowerLeftX + column * (maxLineWidth + columnGap) + (rectSize + rectGap) * i

						addRect(x, y - rectSize, rectSize, rectSize)
						setNonStrokingColor(when {
							i < countEN -> Color.BLACK
							i < countEN + countDE -> Color.MAGENTA
							else -> Color.WHITE
						})
						fillAndStroke()
					}

					val x = box.lowerLeftX + lineIndent + column * (maxLineWidth + columnGap)

					if (more) {
						drawText(symbolMore, font, x - textGap - symbolWidth, y, Color.BLACK)
					}
					drawText(text, font, x, y, color, maxLineWidth - lineIndent)
				}
			}
		}

	}

	fun invoke(block: ColumnSpace.()->Unit): ColumnSpace {
		return this.apply(block)
	}
}

class ColumnManager(val node: Node, val columns: Int, val linesPerColumn: Int, val columnGap: Float, val lineSpacing: Float, val font: Font) {
	val maxLineWidth = (node.box.width-(columns-1)*columnGap)/columns
	val lineHeight = font.totalHeight + lineSpacing


	fun get(index: Int, block: ColumnSpace.()->Unit): ColumnSpace {
		return ColumnSpace(this, index/linesPerColumn, index%linesPerColumn).apply(block)
	}

	fun get(column: Int, line: Int, block: ColumnSpace.()->Unit): ColumnSpace {
		return ColumnSpace(this, column, line).apply(block)
	}

	fun write(column: Int, line: Int, text: String, color: Color, lineIndent: Float = 0f) {
		val x = node.box.lowerLeftX+lineIndent + column*(maxLineWidth+columnGap)
		val y = node.box.upperRightY - line*lineHeight

		node.drawText(text, font, x, y, color, maxLineWidth-lineIndent)
	}
}

fun Node.columns(columns: Int, linesPerColumn: Int, columnGap: Float, lineSpacing: Float, font: Font, block: ColumnManager.() -> Unit): ColumnManager {
	return ColumnManager(this, columns, linesPerColumn, columnGap, lineSpacing, font).apply(block)
}

fun Node.columns(columnGap: Float, lineSpacing: Float, font: PDFont, columns: Int, items: Int, block: ColumnManager.() -> Unit): ColumnManager {
	val linesPerColumn = ceil(items.toFloat()/columns.toFloat()).roundToInt()
	val fontSize = floor((box.height/(linesPerColumn) - lineSpacing) * 1000 / font.fontDescriptor.fontBoundingBox.height)

	return ColumnManager(this, columns, linesPerColumn, columnGap, lineSpacing, Font(font, fontSize)).apply(block)
}

fun Node.columns(columnGap: Float, lineSpacing: Float, font: PDFont, minFontSize: Float, maxFontSize: Float, items: Int, block: ColumnManager.() -> Unit): ColumnManager {
	var columns = 1
	var linesPerColumn = ceil(items.toFloat()/columns.toFloat()).roundToInt()
	var fontSize = min(maxFontSize, floor((box.height/(linesPerColumn) - lineSpacing) * 1000 / font.fontDescriptor.fontBoundingBox.height))
	while(fontSize<minFontSize) {
		columns++
		linesPerColumn = ceil(items.toFloat()/columns.toFloat()).roundToInt()
		fontSize = min(maxFontSize, floor((box.height/(linesPerColumn) - lineSpacing) * 1000 / font.fontDescriptor.fontBoundingBox.height))
	}

	return ColumnManager(this, columns, linesPerColumn, columnGap, lineSpacing, Font(font, fontSize)).apply(block)
}

/*
fun PDPageContentStream.write(text: String, font: PDFont, fontSize: Float, x: Float, y:Float, color: Color) {
	try {
		beginText()
		setFont(font, fontSize)
		// we want to position our text on his baseline
		newLineAtOffset(x, y - FontUtils.getDescent(font, fontSize) - FontUtils.getHeight(font, fontSize))
		setNonStrokingColor(color)
		showText(text)
		endText()
	} catch (e: IOException) {
		throw IllegalStateException("Unable to write text", e)
	}
}

fun PDPageContentStream.write(text: String, font: PDFont, fontSize: Float, x: Float, y:Float, color: Color, maxLineWidth: Float) {
	//val text = "This answer points to something one needs to keep in mind, no matter which PDFBox versi"

	var printedText = text
	var lineWidth = font.getStringWidth(printedText) / 1000 * fontSize
	while(lineWidth > maxLineWidth) {
		val tempPrintedText = printedText.removeSuffix("...").trimEnd().dropLastWhile { !it.isWhitespace() }
		if(!tempPrintedText.isEmpty())
			printedText = "$tempPrintedText ..."
		else
			printedText = printedText.removeSuffix("...").dropLast(1)+"..."
		lineWidth = font.getStringWidth(printedText) / 1000 * fontSize
	}

	write(printedText, font, fontSize, x, y, color)
}
*/
fun PDRectangle.relative(x: Float = 0f, y: Float = 0f, width: Float = 0f, height: Float = 0f) // FIXME cap size if it does not fit?
		= PDRectangle(this.lowerLeftX + x, upperRightY - y - height, width, height)

fun PDRectangle.inset(marginLeft: Float = 0f, marginTop: Float = 0f, marginRight: Float = 0f, marginBottom: Float = 0f)
		= PDRectangle(this.lowerLeftX+marginLeft, this.lowerLeftY + marginBottom, this.width-marginLeft-marginRight, this.height-marginTop-marginBottom)

fun PDRectangle.inset(pagePosition: PagePosition, marginInner: Float = 0f, marginTop: Float = 0f, marginOuter: Float = 0f, marginBottom: Float = 0f)
		= this.inset(if(pagePosition == PagePosition.LEFT) marginOuter else marginInner, marginTop, if(pagePosition == PagePosition.RIGHT) marginOuter else marginInner, marginBottom )

enum class PagePosition {
	SINGLE, LEFT, RIGHT
}



fun createPdfDocument(file: Path, block: PDDocument.()->Unit) = PDDocument().use {
	it.apply(block)
	it.save(file.toFile())
}

fun createPdfDocument( block: PDDocument.()->Unit) = PDDocument().use {it.apply(block) }

open class Node(val contentStream: PDPageContentStream, val box: PDRectangle) {
	var currentCursorPosition = box.upperRightY
}

class Page(val document: PDDocument, val pdPage: PDPage): Node(PDPageContentStream(document, pdPage), pdPage.mediaBox), Closeable {
	override fun close() {
		contentStream.close()
	}
}

fun PDDocument.page(format: PDRectangle, block: Page.()->Unit): Page {
	val page = Page(this, PDPage(format)).use { it.apply(block) }
	this.addPage(page.pdPage)
	return page
}

fun Node.drawText(text: String, font: Font, color: Color, x: Float, y:Float, rotation: Double) {
	try {
		contentStream.apply {
			beginText()
			setFont(font.family, font.size)
			// we want to position our text on his baseline

			setTextMatrix(Matrix.getTranslateInstance(box.lowerLeftX + x, box.upperRightY - y).apply {
				rotate(Math.toRadians(rotation))
			})

			setNonStrokingColor(color)
			showText(text)
			endText()
		}
	} catch (e: IOException) {
		throw IllegalStateException("Unable to write text", e)
	}
}

fun Node.drawText(text: String, font: Font, x: Float, y:Float, color: Color) {
	try {
		contentStream.apply {
			beginText()
			setFont(font.family, font.size)
			// we want to position our text on his baseline


			newLineAtOffset(x, y)
			setNonStrokingColor(color)
			showText(text)
			endText()
		}
	} catch (e: IOException) {
		throw IllegalStateException("Unable to write text", e)
	}
}

fun Node.drawText90(text: String, font: Font, x: Float, y:Float, color: Color) {
	try {
		contentStream.apply {
			beginText()
			setFont(font.family, font.size)
			// we want to position our text on his baseline


			setTextMatrix(Matrix.getRotateInstance(Math.toRadians(90.0), x, y).apply {
//				translate(0f, -box.width)
			})
//			newLineAtOffset(x, y - font.totalHeight)
			setNonStrokingColor(color)
			showText(text)
			endText()
		}
	} catch (e: IOException) {
		throw IllegalStateException("Unable to write text", e)
	}
}

fun Node.drawText270(text: String, font: Font, x: Float, y:Float, color: Color) {
	try {
		contentStream.apply {
			beginText()
			setFont(font.family, font.size)
			// we want to position our text on his baseline


			setTextMatrix(Matrix.getRotateInstance(Math.toRadians(-90.0), x, y).apply {
//				translate(0f, -box.width)
			})
//			newLineAtOffset(x, y - font.totalHeight)
			setNonStrokingColor(color)
			showText(text)
			endText()
		}
	} catch (e: IOException) {
		throw IllegalStateException("Unable to write text", e)
	}
}

fun Node.drawImage(image: PDImageXObject, x: Float, y:Float, width: Float, height: Float) {
	try {
		contentStream.apply {
			drawImage(image, box.lowerLeftX + x, box.upperRightY - y - height, width, height)
		}
	} catch (e: IOException) {
		throw IllegalStateException("Unable to write text", e)
	}
}

fun Node.drawText(text: String, font: Font, x: Float, color: Color) {
	drawText(text, font, x, currentCursorPosition, color)
	currentCursorPosition -= font.totalHeight
}

fun Node.drawText(text: String, font: Font, x: Float, y:Float, color: Color, maxLineWidth: Float) {
	//val text = "This answer points to something one needs to keep in mind, no matter which PDFBox versi"

	var printedText = text
	var lineWidth = font.getWidth(printedText)
	while(lineWidth > maxLineWidth) {
		val tempPrintedText = printedText.removeSuffix("...").trimEnd().dropLastWhile { !it.isWhitespace() }
		if(!tempPrintedText.isEmpty())
			printedText = "$tempPrintedText ..."
		else
			printedText = printedText.removeSuffix("...").dropLast(1)+"..."
		lineWidth = font.getWidth(printedText)
	}

	drawText(printedText, font, x, y, color)
}

fun Node.drawText(text: String, font: Font, horizontalAlignment: HorizontalAlignment, shiftX: Float, y: Float, color: Color) {
	val startX = when(horizontalAlignment) {
		HorizontalAlignment.LEFT -> box.lowerLeftX
		HorizontalAlignment.CENTER -> (box.width - font.getWidth(text)) / 2 + box.lowerLeftX
		HorizontalAlignment.RIGHT -> box.upperRightX - font.getWidth(text)
	}
	drawText(text, font, startX + shiftX, box.upperRightY - y, color)
}

fun Node.drawText(text: String, font: Font, verticalAlignment: VerticalAlignment, x: Float, color: Color) {
	val objectHeight = 0f // TODO font height?!
	val startY = when(verticalAlignment) {
		VerticalAlignment.TOP -> box.upperRightY - objectHeight // TODO subtract font height?!
		VerticalAlignment.MIDDLE -> (box.height - objectHeight) / 2 + box.lowerLeftY
		VerticalAlignment.BOTTOM -> box.lowerLeftY // TODO subtract font height?!
	}
	drawText(text, font, x, startY, color)
}

fun Node.drawText(text: String, font: Font, horizontalAlignment: HorizontalAlignment, color: Color) {
	val startX = when(horizontalAlignment) {
		HorizontalAlignment.LEFT -> box.lowerLeftX
		HorizontalAlignment.CENTER -> (box.width - font.getWidth(text)) / 2 + box.lowerLeftX
		HorizontalAlignment.RIGHT -> box.upperRightX - font.getWidth(text)
	}
	drawText(text, font, startX, color)
}
//
//class Columns(val parentNode: Node, val columnCount: UInt): Node(parentNode.contentStream, parentNode.box) {
//	var currentColumn = 0u
//}
//
//class Column(val parent: Columns, val columnIndex: UInt): Node(parent.contentStream, box = PDRectangle(parent.box.))

class Frame(val parentNode: Node, box: PDRectangle): Node(parentNode.contentStream, box) {
}

fun Node.frame(box: PDRectangle = this.box, block: Frame.()->Unit)
		= Frame(this, box).apply(block)
fun Node.frame(marginLeft: Float = 0f, marginTop: Float = 0f, marginRight: Float = 0f, marginBottom: Float = 0f, block: Frame.()->Unit)
		= Frame(this, box.inset(marginLeft, marginTop, marginRight, marginBottom)).apply(block)
fun Node.frameRelative(x: Float = 0f, y: Float = 0f, width: Float = 0f, height: Float = 0f, block: Frame.()->Unit)
		= Frame(this, box.relative(x, y, width, height)).apply(block)

fun Node.frame(pagePosition: PagePosition, marginInner: Float = 0f, marginTop: Float = 0f, marginOuter: Float = 0f, marginBottom: Float = 0f, block: Frame.()->Unit)
		= Frame(this, box.inset(pagePosition, marginInner, marginTop, marginOuter, marginBottom)).apply(block)

fun Node.drawBorder(lineWidth: Float, lineColor: Color) {
	contentStream.apply {
		setLineWidth(lineWidth)
		setStrokingColor(lineColor)

//		addRect(box.lowerLeftX-lineWidth, box.lowerLeftY-lineWidth, box.width+lineWidth*2, box.height+lineWidth*2)
		addRect(box.lowerLeftX, box.lowerLeftY, box.width, box.height)
		stroke()
	}
}

fun Node.drawBackground(backgroundColor: Color) {
	contentStream.apply {
		setGraphicsStateParameters(PDExtendedGraphicsState().apply {
			nonStrokingAlphaConstant = backgroundColor.alpha/255f
		})
		setNonStrokingColor(backgroundColor)

		addRect(box.lowerLeftX, box.lowerLeftY, box.width, box.height)
		fill()

		setGraphicsStateParameters(PDExtendedGraphicsState().apply {
			nonStrokingAlphaConstant = 1f
		})
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