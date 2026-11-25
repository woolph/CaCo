/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.collection.labels

import at.woolph.caco.collection.labels.box.BlankBoxLabel
import at.woolph.caco.collection.labels.box.BoxLabel
import at.woolph.caco.collection.labels.box.DualSymbolBoxLabel
import at.woolph.caco.collection.labels.box.MultipleSymbolBoxLabel
import at.woolph.caco.collection.labels.box.OneSymbolBoxLabel
import at.woolph.utils.io.asSink
import at.woolph.utils.io.createParentDirectories
import at.woolph.utils.pdf.HorizontalAlignment
import at.woolph.utils.pdf.dotsPerMillimeter
import at.woolph.utils.pdf.drawAsImageLeft
import at.woolph.utils.pdf.drawAsImageRight
import at.woolph.utils.pdf.drawBorder
import at.woolph.utils.pdf.drawText
import at.woolph.utils.pdf.frame
import at.woolph.utils.pdf.loadFont72Black
import at.woolph.utils.pdf.loadFontPlanewalkerBold
import at.woolph.utils.pdf.pdfDocument
import kotlinx.io.files.Path
import java.awt.Color
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun printBoxLabel(file: Path, labels: List<BoxLabel>) {
    transaction {
      pdfDocument(file.createParentDirectories().asSink()) {
        val fontColor = Color.BLACK

        val fontFamilyPlanewalker = loadFontPlanewalkerBold()
        val fontFamily72Black = loadFont72Black()

        val pageFormat = PDRectangle.A4

        val columns = 3
        val rows = 2
        val itemsPerPage = columns * rows

        val columnWidth = pageFormat.width / columns
        val rowHeight = pageFormat.height / rows

        val margin = 12f
        val defaultGapSize = 5f

        val foldingLine = 80.8f * 90f / 89.8f * dotsPerMillimeter

        val bannerHeight = 15f * dotsPerMillimeter - margin

        val sizedFontCode = fontFamilyPlanewalker.withSize(bannerHeight * 0.7f)
        val sizedFontCode2 = fontFamily72Black.withSize(bannerHeight * 0.7f)
        val sizedFontSubtitle = fontFamilyPlanewalker.withSize(6f)

        val desiredHeight = bannerHeight * 0.8f
        val maximumIconWidth = desiredHeight

        labels.chunked(itemsPerPage).forEachIndexed { pageIndex, separatorItems ->
          page(pageFormat) {
            println("box label page #$pageIndex")
            separatorItems.chunked(columns).forEachIndexed { rowIndex, rowItems ->
              rowItems.forEachIndexed { columnIndex, columnItem ->
                val borderWidth = 2f
                frame(
                    columnWidth * columnIndex,
                    rowHeight * rowIndex,
                    (columnWidth) * (columns - columnIndex - 1),
                    (rowHeight) * (rows - rowIndex - 1),
                ) {
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
                            sizedFontCode,
                            HorizontalAlignment.LEFT,
                            maximumIconWidth + defaultGapSize,
                            box.height + sizedFontCode.descent,
                            fontColor,
                        )
                        columnItem.icon?.let {
                          drawAsImageLeft(
                              createFromByteArray(it, "OneSymbolBoxLabel(${columnItem.title})"),
                              maximumIconWidth,
                              desiredHeight,
                              0f,
                              box.height - desiredHeight,
                          )
                        }
                        columnItem.subtitle?.let {
                          drawText(
                              it,
                              sizedFontSubtitle,
                              HorizontalAlignment.LEFT,
                              maximumIconWidth + defaultGapSize + 8f,
                              box.height + sizedFontSubtitle.descent + 2f,
                              fontColor,
                          )
                        }
                      }

                      is DualSymbolBoxLabel -> {
                        drawText(
                            columnItem.title,
                            sizedFontCode,
                            HorizontalAlignment.LEFT,
                            maximumIconWidth + defaultGapSize,
                            box.height + sizedFontCode.descent,
                            fontColor,
                        )
                        columnItem.icon?.let {
                          drawAsImageLeft(
                              createFromByteArray(it, "DualSymbolBoxLabel(${columnItem.title})"),
                              maximumIconWidth,
                              desiredHeight,
                              0f,
                              box.height - desiredHeight,
                          )
                        }
                        columnItem.icon2?.let {
                          drawAsImageRight(
                              createFromByteArray(it, "boxLabel2(${columnItem.title})"),
                              maximumIconWidth,
                              desiredHeight,
                              0f,
                              box.height - desiredHeight,
                          )
                        }
                        columnItem.subtitle?.let {
                          drawText(
                              it,
                              sizedFontSubtitle,
                              HorizontalAlignment.LEFT,
                              maximumIconWidth + defaultGapSize + 8f,
                              box.height + sizedFontSubtitle.descent + 2f,
                              fontColor,
                          )
                        }
                      }

                      is MultipleSymbolBoxLabel -> {
                        drawText(
                            columnItem.title,
                            sizedFontCode2,
                            HorizontalAlignment.LEFT,
                            0f,
                            box.height + sizedFontCode.descent + 2f,
                            fontColor,
                        )
                        val width = sizedFontCode2.getWidth(columnItem.title) + defaultGapSize

                        val iconHeight = desiredHeight / columnItem.rows
                        val iconWidth = maximumIconWidth / columnItem.rows
                        val gapX = iconWidth + 2f * columnItem.rows
                        val gapXWithinColummn = iconWidth * 0.75f
                        val gapY = iconHeight * 0.75f

                        columnItem.icons.forEachIndexed { i, nullableIcon ->
                          nullableIcon?.let { icon ->
                            val row = i % columnItem.rows
                            val column = i / columnItem.rows

                            val displacingX = gapX * column + gapXWithinColummn * row
                            val displacingY = gapY * (columnItem.rows - row - 1)

                            drawAsImageLeft(
                                createFromByteArray(
                                    icon,
                                    "MultipleSymbolBoxLabel(${columnItem.title}#$i)",
                                ),
                                iconWidth,
                                iconHeight,
                                width + displacingX,
                                box.height - iconHeight - displacingY,
                            )
                          }
                        }
                        columnItem.subtitle?.let {
                          drawText(
                              it,
                              sizedFontSubtitle,
                              HorizontalAlignment.LEFT,
                              8f,
                              box.height + sizedFontSubtitle.descent + 2f,
                              fontColor,
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
