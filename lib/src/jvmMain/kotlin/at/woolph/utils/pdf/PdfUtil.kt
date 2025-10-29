/* Copyright 2025 Wolfgang Mayer */
package at.woolph.utils.pdf

import java.awt.Color
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState

fun PDRectangle.toPosition() = Position(width, height)

class ColumnSpace(val columnManager: ColumnManager, val column: Int, val line: Int) {
  fun drawText(text: String, color: Color, lineIndent: Float = 0f) {
    val x =
        columnManager.node.box.lowerLeftX +
            lineIndent +
            column * (columnManager.maxLineWidth + columnManager.columnGap)
    val y = columnManager.node.box.upperRightY - line * columnManager.lineHeight

    columnManager.node.drawText(
        text,
        columnManager.sizedFont,
        x,
        y,
        color,
        columnManager.maxLineWidth - lineIndent,
    )
  }

  fun drawTextWithRects(text: String, countEN: Int, countDE: Int) {
    columnManager.apply {
      val max = 1
      val rectLineWidth = 1f
      val rectSize = sizedFont.size - rectLineWidth * 2
      val rectGap = 2f
      val textGap = 2f
      val symbolMore = "+"
      val symbolWidth = sizedFont.getWidth(symbolMore)
      val lineIndent = (rectSize + rectGap) * max + textGap + symbolWidth

      val more = (countEN + countDE) > max
      val enoughEN = countEN >= max
      val enoughTotal = (countEN + countDE) >= max
      val color =
          if (enoughEN) Color.LIGHT_GRAY else if (enoughTotal) Color.MAGENTA else Color.BLACK

      columnManager.node.apply {
        contentStream.apply {
          val y = box.upperRightY - line * lineHeight

          setLineWidth(rectLineWidth)
          setStrokingColor(Color.BLACK)

          for (i in 0 until max) {
            val x = box.lowerLeftX + column * (maxLineWidth + columnGap) + (rectSize + rectGap) * i

            addRect(x, y - rectSize, rectSize, rectSize)
            setNonStrokingColor(
                when {
                  i < countEN -> Color.BLACK
                  i < countEN + countDE -> Color.MAGENTA
                  else -> Color.WHITE
                }
            )
            fillAndStroke()
          }

          val x = box.lowerLeftX + lineIndent + column * (maxLineWidth + columnGap)

          if (more) {
            drawText(symbolMore, sizedFont, x - textGap - symbolWidth, y - sizedFont.height, Color.BLACK)
          }
          drawText(text, sizedFont, x, y - sizedFont.height, color, maxLineWidth - lineIndent)
        }
      }
    }
  }

  fun invoke(block: ColumnSpace.() -> Unit): ColumnSpace {
    return this.apply(block)
  }
}

class ColumnManager(
  val node: Node,
  val columns: Int,
  val linesPerColumn: Int,
  val columnGap: Float,
  val lineSpacing: Float,
  val sizedFont: SizedFont,
) {
  val maxLineWidth = (node.box.width - (columns - 1) * columnGap) / columns
  val lineHeight = sizedFont.totalHeight + lineSpacing

  fun get(index: Int, block: ColumnSpace.() -> Unit): ColumnSpace {
    return ColumnSpace(this, index / linesPerColumn, index % linesPerColumn).apply(block)
  }

  fun get(column: Int, line: Int, block: ColumnSpace.() -> Unit): ColumnSpace {
    return ColumnSpace(this, column, line).apply(block)
  }

  fun write(column: Int, line: Int, text: String, color: Color, lineIndent: Float = 0f) {
    val x = node.box.lowerLeftX + lineIndent + column * (maxLineWidth + columnGap)
    val y = node.box.upperRightY - line * lineHeight

    node.drawText(text, sizedFont, x, y, color, maxLineWidth - lineIndent)
  }
}

fun Node.columns(
  columns: Int,
  linesPerColumn: Int,
  columnGap: Float,
  lineSpacing: Float,
  sizedFont: SizedFont,
  block: ColumnManager.() -> Unit,
): ColumnManager {
  return ColumnManager(this, columns, linesPerColumn, columnGap, lineSpacing, sizedFont).apply(block)
}

fun Node.columns(
    columnGap: Float,
    lineSpacing: Float,
    font: PDFont,
    columns: Int,
    items: Int,
    block: ColumnManager.() -> Unit,
): ColumnManager {
  val linesPerColumn = ceil(items.toFloat() / columns.toFloat()).roundToInt()
  val fontSize =
      floor(
          (box.height / (linesPerColumn) - lineSpacing) * 1000 /
              font.fontDescriptor.fontBoundingBox.height
      )

  return ColumnManager(this, columns, linesPerColumn, columnGap, lineSpacing, SizedFont(font, fontSize))
      .apply(block)
}

fun Node.columns(
    columnGap: Float,
    lineSpacing: Float,
    font: PDFont,
    minFontSize: Float,
    maxFontSize: Float,
    items: Int,
    block: ColumnManager.() -> Unit,
): ColumnManager {
  var columns = 1
  var linesPerColumn = ceil(items.toFloat() / columns.toFloat()).roundToInt()
  var fontSize =
      min(
          maxFontSize,
          floor(
              (box.height / (linesPerColumn) - lineSpacing) * 1000 /
                  font.fontDescriptor.fontBoundingBox.height
          ),
      )
  while (fontSize < minFontSize) {
    columns++
    linesPerColumn = ceil(items.toFloat() / columns.toFloat()).roundToInt()
    fontSize =
        min(
            maxFontSize,
            floor(
                (box.height / (linesPerColumn) - lineSpacing) * 1000 /
                    font.fontDescriptor.fontBoundingBox.height
            ),
        )
  }

  return ColumnManager(this, columns, linesPerColumn, columnGap, lineSpacing, SizedFont(font, fontSize))
      .apply(block)
}

fun PDRectangle.withRelativeSize(
    horizontalAlignment: HorizontalAlignment,
    offSetX: Float = 0f,
    verticalAlignment: VerticalAlignment,
    offSetY: Float = 0f,
    width: Float = 0f,
    height: Float = 0f,
): PDRectangle {
  val x =
      offSetX +
          when (horizontalAlignment) {
            HorizontalAlignment.LEFT -> this.lowerLeftX
            HorizontalAlignment.CENTER -> (this.lowerLeftX + this.upperRightX - width) / 2
            HorizontalAlignment.RIGHT -> this.upperRightX - width
          }

  val y =
      -offSetY +
          when (verticalAlignment) {
            VerticalAlignment.TOP -> this.upperRightY - height
            VerticalAlignment.MIDDLE -> (this.lowerLeftY + this.upperRightY - height) / 2
            VerticalAlignment.BOTTOM -> lowerLeftY
          }

  return PDRectangle(x, y, width, height)
}

fun PDRectangle.inset(
    marginLeft: Float = 0f,
    marginTop: Float = 0f,
    marginRight: Float = 0f,
    marginBottom: Float = 0f,
) =
    PDRectangle(
        this.lowerLeftX + marginLeft,
        this.lowerLeftY + marginBottom,
        this.width - marginLeft - marginRight,
        this.height - marginTop - marginBottom,
    )

fun PDRectangle.inset(
    pagePosition: PagePosition,
    marginInner: Float = 0f,
    marginTop: Float = 0f,
    marginOuter: Float = 0f,
    marginBottom: Float = 0f,
) =
    this.inset(
        if (pagePosition == PagePosition.LEFT) marginOuter else marginInner,
        marginTop,
        if (pagePosition == PagePosition.RIGHT) marginOuter else marginInner,
        marginBottom,
    )

fun Node.drawText(
  text: String,
  sizedFont: SizedFont,
  horizontalAlignment: HorizontalAlignment,
  shiftX: Float,
  y: Float,
  color: Color,
) {
  val startX =
      when (horizontalAlignment) {
        HorizontalAlignment.LEFT -> box.lowerLeftX
        HorizontalAlignment.CENTER -> (box.width - sizedFont.getWidth(text)) / 2 + box.lowerLeftX
        HorizontalAlignment.RIGHT -> box.upperRightX - sizedFont.getWidth(text)
      }
  drawText(text, sizedFont, startX + shiftX, box.upperRightY - y, color)
}

fun Node.drawText(
  text: String,
  sizedFont: SizedFont,
  verticalAlignment: VerticalAlignment,
  x: Float,
  color: Color,
) {
  val objectHeight = sizedFont.height
  val startY =
      when (verticalAlignment) {
        VerticalAlignment.TOP -> box.upperRightY - objectHeight
        VerticalAlignment.MIDDLE -> (box.height - objectHeight) / 2 + box.lowerLeftY
        VerticalAlignment.BOTTOM -> box.lowerLeftY
      }
  drawText(text, sizedFont, x, startY, color)
}

fun Node.drawText(
  text: String,
  sizedFont: SizedFont,
  horizontalAlignment: HorizontalAlignment,
  color: Color,
) {
  val startX =
      when (horizontalAlignment) {
        HorizontalAlignment.LEFT -> box.lowerLeftX
        HorizontalAlignment.CENTER -> (box.width - sizedFont.getWidth(text)) / 2 + box.lowerLeftX
        HorizontalAlignment.RIGHT -> box.upperRightX - sizedFont.getWidth(text)
      }
  drawText(text, sizedFont, startX, color)
}

fun Node.frame(box: PDRectangle = this.box, block: Node.() -> Unit) = Node(document, contentStream, box, this).apply(block)

fun Node.frame(
    marginLeft: Float = 0f,
    marginTop: Float = 0f,
    marginRight: Float = 0f,
    marginBottom: Float = 0f,
    block: Node.() -> Unit,
) = frame(box.inset(marginLeft, marginTop, marginRight, marginBottom), block)

fun Node.frameRelative(
    horizontalAlignment: HorizontalAlignment,
    offsetX: Float = 0f,
    verticalAlignment: VerticalAlignment,
    offsetY: Float = 0f,
    width: Float = 0f,
    height: Float = 0f,
    block: Node.() -> Unit,
) =
  frame(box.withRelativeSize(horizontalAlignment, offsetX, verticalAlignment, offsetY, width, height), block)

fun Page.framePagePosition(
    marginInner: Float,
    marginOuter: Float,
    marginTop: Float = 0f,
    marginBottom: Float = 0f,
    block: Node.() -> Unit,
) = frame(box.inset(pagePosition, marginInner, marginTop, marginOuter, marginBottom), block)

fun Node.drawBorder(lineWidth: Float, lineColor: Color) {
  contentStream.apply {
    setLineWidth(lineWidth)
    setStrokingColor(lineColor)

    addRect(box.lowerLeftX, box.lowerLeftY, box.width, box.height)
    stroke()
  }
}

fun Node.drawBackground(backgroundColor: Color) {
  contentStream.apply {
    setGraphicsStateParameters(
        PDExtendedGraphicsState().apply { nonStrokingAlphaConstant = backgroundColor.alpha / 255f }
    )
    setNonStrokingColor(backgroundColor)

    addRect(box.lowerLeftX, box.lowerLeftY, box.width, box.height)
    fill()

    setGraphicsStateParameters(PDExtendedGraphicsState().apply { nonStrokingAlphaConstant = 1f })
  }
}

fun Node.drawAsImage(
    subIcon: PDImageXObject,
    maximumWidth: Float,
    desiredHeight: Float,
    i: Int,
    columnWidth: Float,
    xOffsetIcons: Float,
    yOffsetIcons: Float,
) {
  val heightScale = desiredHeight / subIcon.height
  val desiredWidth = subIcon.width * heightScale
  val (actualWidth, actualHeight) =
      if (desiredWidth > maximumWidth) {
        maximumWidth to subIcon.height * maximumWidth / subIcon.width
      } else {
        desiredWidth to desiredHeight
      }
  drawImage(
      subIcon,
      columnWidth * 0.5f + xOffsetIcons + columnWidth * i - actualWidth * 0.5f,
      yOffsetIcons + (desiredHeight - actualHeight) * 0.5f,
      actualWidth,
      actualHeight,
  )
}

fun Node.drawAsImageCentered(
    subIcon: PDImageXObject,
    maximumWidth: Float,
    desiredHeight: Float,
    xOffsetIcons: Float,
    yOffsetIcons: Float,
) {
  val heightScale = desiredHeight / subIcon.height
  val desiredWidth = subIcon.width * heightScale
  val (actualWidth, actualHeight) =
      if (desiredWidth > maximumWidth) {
        maximumWidth to subIcon.height * maximumWidth / subIcon.width
      } else {
        desiredWidth to desiredHeight
      }
  drawImage(
      subIcon,
      box.width * 0.5f + xOffsetIcons - actualWidth * 0.5f,
      yOffsetIcons + (desiredHeight - actualHeight) * 0.5f,
      actualWidth,
      actualHeight,
  )
}

fun Node.drawAsImageLeft(
    subIcon: PDImageXObject,
    maximumWidth: Float,
    desiredHeight: Float,
    xOffsetIcons: Float,
    yOffsetIcons: Float,
) {
  val heightScale = desiredHeight / subIcon.height
  val desiredWidth = subIcon.width * heightScale
  val (actualWidth, actualHeight) =
      if (desiredWidth > maximumWidth) {
        maximumWidth to subIcon.height * maximumWidth / subIcon.width
      } else {
        desiredWidth to desiredHeight
      }
  drawImage(
      subIcon,
      xOffsetIcons,
      yOffsetIcons + (desiredHeight - actualHeight) * 0.5f,
      actualWidth,
      actualHeight,
  )
}

fun Node.drawAsImageRight(
    subIcon: PDImageXObject,
    maximumWidth: Float,
    desiredHeight: Float,
    xOffsetIcons: Float,
    yOffsetIcons: Float,
) {
  val heightScale = desiredHeight / subIcon.height
  val desiredWidth = subIcon.width * heightScale
  val (actualWidth, actualHeight) =
      if (desiredWidth > maximumWidth) {
        maximumWidth to subIcon.height * maximumWidth / subIcon.width
      } else {
        desiredWidth to desiredHeight
      }
  drawImage(
      subIcon,
      box.width + xOffsetIcons - actualWidth,
      yOffsetIcons + (desiredHeight - actualHeight) * 0.5f,
      actualWidth,
      actualHeight,
  )
}

val dotsPerMillimeter = PDRectangle.A4.width / 210f

fun <Item> PDFDocument.paginatedColumnedContent(
    items: Iterable<Item>,
    pageFormat: PDRectangle,
    columnCount: Int,
    columnBlock: Node.(Item) -> Unit,
) {
  val columnWidth = pageFormat.width / columnCount
  items.chunked(columnCount).forEachIndexed { pageIndex, columns ->
    page(pageFormat) {
      println("column page #$pageIndex")
      columns.forEachIndexed { i, columnItem ->
        frame(columnWidth * i, 0f, (columnWidth) * (columnCount - i - 1), 0f) { columnBlock(columnItem) }
      }
    }
  }
}
