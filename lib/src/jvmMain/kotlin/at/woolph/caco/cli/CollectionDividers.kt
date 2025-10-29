/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli

import at.woolph.caco.binderlabels.*
import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.sets.MultiSetBlock
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.datamodel.sets.SingleSetBlock
import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer
import at.woolph.utils.io.asSink
import at.woolph.utils.pdf.HorizontalAlignment
import at.woolph.utils.pdf.pdfDocument
import at.woolph.utils.pdf.dotsPerMillimeter
import at.woolph.utils.pdf.drawAsImageCentered
import at.woolph.utils.pdf.drawAsImageLeft
import at.woolph.utils.pdf.drawBorder
import at.woolph.utils.pdf.drawText
import at.woolph.utils.pdf.frame
import at.woolph.utils.pdf.loadFont72Black
import at.woolph.utils.pdf.loadFontPlanewalkerBold
import java.awt.Color
import java.nio.file.Path
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.io.path.createParentDirectories

interface CollectionDivider {
  val title: String
  val subtitle: String?
    get() = null
}

interface OneSymbolCollectionDivider : CollectionDivider {
  val code: String
  val icon: ByteArray?
}

object PromosCollectionDivider : OneSymbolCollectionDivider {
  override val code: String = "PRM"
  override val title: String = "Promos & Specials"
  override val icon by lazySetIcon("star", mythicBinderLabelIconRenderer)
}

object BlankCollectionDivider : OneSymbolCollectionDivider {
  override val code: String = ""
  override val title: String = ""
  override val icon: ByteArray? = null
}

open class GenericCollectionDivider(override val code: String, override val title: String) :
    OneSymbolCollectionDivider {
  override val icon by lazySetIcon("default", mythicBinderLabelIconRenderer)
}

open class SimpleSetCollectionDivider(override val code: String) : OneSymbolCollectionDivider {
  private val set = transaction {
    ScryfallCardSet.findByCode(code)
        ?: throw IllegalArgumentException("no set with code $code found")
  }
  override val title: String
    get() = set.name

  override val icon by set.lazySetIcon(mythicBinderLabelIconRenderer)
}

interface MultipleSymbolCollectionDivider : CollectionDivider {
  val icons: List<ByteArray>
}

open class BlockCollectionDivider(override val title: String, codes: Iterable<String>) :
    MultipleSymbolCollectionDivider {
  constructor(title: String, vararg codes: String) : this(title, codes.asIterable())

  private val sets = fetchCardSets(codes)
  //    override val title: String = "Block"
  override val icons: List<ByteArray> by lazy {
    sets.mapNotNull { it.lazySetIcon(mythicBinderLabelIconRenderer).value }
  }
}

class PileSeparators {
  fun printLabel(file: String) {
    Databases.init()

    transaction {
      val blockNameBlacklist =
          listOf(
              "Commander",
              "Core Set",
              "Heroes ofthe Realm",
              "Judge Gift Cards",
              "Friday Night Magic",
              "Magic Player Rewards",
              "Arena League",
          )
      val separators =
          ScryfallCardSet.allGroupedByBlocks().map {
            when (it) {
              is SingleSetBlock -> SimpleSetCollectionDivider(it.set.code)
              is MultiSetBlock -> BlockCollectionDivider(it.blockName, it.sets.map { it.code })
            }
          }
      pdfDocument(Path.of(file).createParentDirectories().asSink()) {
        val fontColor = Color.BLACK
        val fontFamilyPlanewalker = loadFontPlanewalkerBold()
        val fontFamily72Black = loadFont72Black()

        val pageFormat = PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width)

        val columns = 4
        val rows = 2
        val itemsPerPage = columns * rows

        val columnWidth = pageFormat.width / columns
        val rowHeight = pageFormat.height / rows

        val magicCardHeight = 88f * dotsPerMillimeter

        val margin = 12f
        val defaultGapSize = 5f

        val bannerHeight = rowHeight - magicCardHeight - margin

        val sizedFontCode = fontFamily72Black.withSize(bannerHeight * 0.9f)
        val sizedFontCode2 = fontFamilyPlanewalker.withSize(8f)

        val maximumWidth = columnWidth - 2 * margin
        val desiredHeight = bannerHeight * 0.9f
        val maximumIconWidth = bannerHeight
        val offsetX = (maximumWidth - maximumIconWidth) * 0.5f

        separators.chunked(itemsPerPage).forEachIndexed { pageIndex, separatorItems ->
          page(pageFormat) {
            println("collection dividers page #$pageIndex")
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
                  frame(margin, margin, margin, margin) {
                    when (columnItem) {
                      is BlankCollectionDivider -> {}
                      is OneSymbolCollectionDivider -> {
                        drawText(
                          columnItem.title,
                          sizedFontCode2,
                          HorizontalAlignment.CENTER,
                          0f,
                          desiredHeight + sizedFontCode2.height,
                          fontColor,
                        )
                        drawText(
                          columnItem.code.uppercase(),
                          sizedFontCode,
                          HorizontalAlignment.CENTER,
                          0f,
                          sizedFontCode.height,
                          fontColor,
                        )
                        columnItem.icon?.let {
                          createFromByteArray(it)
                        }?.let {
                            drawAsImageCentered(it, maximumIconWidth, desiredHeight, -offsetX, 0f)
                            drawAsImageCentered(it, maximumIconWidth, desiredHeight, +offsetX, 0f)
                          }
                        }

                      is MultipleSymbolCollectionDivider -> {
                        drawText(
                          columnItem.title,
                          sizedFontCode2,
                          HorizontalAlignment.CENTER,
                          0f,
                          desiredHeight + sizedFontCode2.height,
                          fontColor,
                        )
                        val start =
                          box.width * 0.5f - maximumIconWidth * columnItem.icons.count() * 0.5f
                        columnItem.icons
                          .map { createFromByteArray(it) }
                          .forEachIndexed { i, icon ->
                            drawAsImageLeft(
                              icon,
                              maximumIconWidth,
                              desiredHeight,
                              start + maximumIconWidth * i,
                              0f,
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
}

fun main() {
  PileSeparators().printLabel(".\\CollectionDividers.pdf")
}
