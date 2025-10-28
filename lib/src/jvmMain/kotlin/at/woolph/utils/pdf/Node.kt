package at.woolph.utils.pdf

import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.util.Matrix
import java.awt.Color
import java.io.IOException


open class Node(
  val document: PDFDocument,
  val contentStream: PDPageContentStream,
  val box: PDRectangle,
  val parentNode: Node? = null
) {
  var currentCursorPosition = box.upperRightY
}

fun Node.drawText(text: String, font: Font, color: Color, x: Float, y: Float, rotation: Double) {
  try {
    contentStream.apply {
      beginText()
      setFont(font.family, font.size)
      // we want to position our text on his baseline

      setTextMatrix(
        Matrix.getTranslateInstance(box.lowerLeftX + x, box.upperRightY - y).apply {
          rotate(Math.toRadians(rotation))
        }
      )

      setNonStrokingColor(color)
      showText(text)
      endText()
    }
  } catch (e: IOException) {
    throw IllegalStateException("Unable to write text", e)
  }
}

fun Node.drawText(text: String, font: Font, x: Float, y: Float, color: Color) {
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

fun Node.drawText90(text: String, font: Font, x: Float, y: Float, color: Color) {
  try {
    contentStream.apply {
      beginText()
      setFont(font.family, font.size)
      // we want to position our text on his baseline

      setTextMatrix(
        Matrix.getRotateInstance(Math.toRadians(90.0), x, y).apply {
          //				translate(0f, -box.width)
        }
      )
      //			newLineAtOffset(x, y - font.totalHeight)
      setNonStrokingColor(color)
      showText(text)
      endText()
    }
  } catch (e: IOException) {
    throw IllegalStateException("Unable to write text", e)
  }
}

fun Node.drawText270(text: String, font: Font, x: Float, y: Float, color: Color) {
  try {
    contentStream.apply {
      beginText()
      setFont(font.family, font.size)
      // we want to position our text on his baseline

      setTextMatrix(
        Matrix.getRotateInstance(Math.toRadians(-90.0), x, y).apply {
          //				translate(0f, -box.width)
        }
      )
      //			newLineAtOffset(x, y - font.totalHeight)
      setNonStrokingColor(color)
      showText(text)
      endText()
    }
  } catch (e: IOException) {
    throw IllegalStateException("Unable to write text", e)
  }
}

fun Node.drawImage(image: PDImageXObject, x: Float, y: Float, width: Float, height: Float) {
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

fun Node.drawText(text: String, font: Font, x: Float, y: Float, color: Color, maxLineWidth: Float) {
  var printedText = text
  var lineWidth = font.getWidth(printedText)
  while (lineWidth > maxLineWidth && printedText.length > 4) {
    val tempPrintedText =
      printedText.removeSuffix("...").trimEnd().dropLastWhile { !it.isWhitespace() }
    if (tempPrintedText.isNotEmpty()) printedText = "$tempPrintedText ..."
    else printedText = printedText.removeSuffix("...").dropLast(1) + "..."
    lineWidth = font.getWidth(printedText)
  }

  drawText(printedText, font, x, y, color)
}
