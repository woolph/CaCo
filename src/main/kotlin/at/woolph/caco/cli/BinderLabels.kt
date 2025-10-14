/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli

import at.woolph.caco.binderlabels.*
import at.woolph.caco.datamodel.Databases
import at.woolph.caco.datamodel.sets.MultiSetBlock
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.datamodel.sets.ScryfallCardSets
import at.woolph.caco.datamodel.sets.SingleSetBlock
import at.woolph.libs.pdf.*
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.nio.file.Path

class BinderLabels(
) {
  fun printLabel(file: Path, labels: List<MapLabelItem>, labelsPerPage: Int) {
    createPdfDocument(file) {
      val fontColor = Color.BLACK
      val fontSubColor = Color.GRAY
      val backgroundColor: Color? = null

      val mtgLogo = createFromFile(Path.of("./assets/images/mtg.png"))

      val fontFamilyPlanewalker = loadType0Font(javaClass.getResourceAsStream("/fonts/PlanewalkerBold-xZj5.ttf")!!)
      val fontFamily72Black = loadType0Font(javaClass.getResourceAsStream("/fonts/72-Black.ttf")!!)

      val pageFormat = PDRectangle.A4
      val columnWidth = pageFormat.width / labelsPerPage

      val fontTitle = Font(fontFamilyPlanewalker, columnWidth*0.5f)
      val fontSubTitle = Font(fontFamilyPlanewalker, columnWidth*0.17f)
      val fontCode = Font(fontFamily72Black, columnWidth*0.28f)
      val fontCodeCommanderSubset = Font(fontFamily72Black, columnWidth*0.14f)

      val margin = 12f

      val maximumWidth = columnWidth - 2 * margin
      val maximumHeight = pageFormat.height - 2 * margin
      val desiredHeight = 64f

      val mtgLogoWidth = maximumWidth
      val mtgLogoHeight = mtgLogo.height.toFloat() / mtgLogo.width.toFloat() * mtgLogoWidth
      val magicLogoYPosition = 0f

      val defaultGapSize = 5f
      val mainIconYPos = 0f
      val codeYPos = mainIconYPos + desiredHeight + fontCode.totalHeight + defaultGapSize
      val subCodeYPos = codeYPos + fontCodeCommanderSubset.totalHeight

      val setDivHeight = desiredHeight + fontCode.totalHeight + fontCodeCommanderSubset.totalHeight + defaultGapSize
      val setDivYPos = maximumHeight - setDivHeight

      val maxTitleWidth =
        pageFormat.height - magicLogoYPosition - mtgLogoHeight - defaultGapSize - setDivHeight - 2 * margin - 2 * defaultGapSize

      val titleYPosition = setDivYPos - 3 * defaultGapSize
      val subTitleYPosition = titleYPosition - 30f


      columnedContent(labels, pageFormat, labelsPerPage) { set ->
        drawBorder(2f, fontColor)
        backgroundColor?.let { drawBackground(it) }

        frame(margin, margin, margin, margin) {
          drawAsImageCentered(
            mtgLogo,
            mtgLogoWidth,
            mtgLogoHeight,
            0f,
            magicLogoYPosition,
          )

          adjustTextToFitWidth(
            set.title,
            fontTitle,
            maxTitleWidth,
            columnWidth*0.5f,
          ).let { (title, font) ->
            val titleXPosition = (maximumWidth + font.height) * 0.5f

            set.subTitle?.let { _subTitle ->
              adjustTextToFitWidth(
                _subTitle,
                fontSubTitle,
                maxTitleWidth,
                columnWidth*0.17f,
              ).let { (title, font) ->
                val subTitleXPosition = titleXPosition + font.height + defaultGapSize
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

fun main() {
  Databases.init()

  transaction {
    val thresholdTooMuchPages = 45 // TODE fine tune the max card count per page, so that the labels are not too small
    val thresholdTooFewPages = 7

    val labels = sequence<MapLabelItem> {
      // TODO I would like to habe some reassignments of parent stuff (pltc is child of ltr, but I'd like it to be a child of ltc, H1R should be MH1 child, H2R of MH2, GK1 of
      ScryfallCardSet.rootSetsGroupedByBlocks { (ScryfallCardSets.digitalOnly eq false).and(ScryfallCardSets.cardCount greater 12) }
        .filterNot { it is SingleSetBlock && it.set.code in setOf("sld", "30a", "unk") }
        .forEach { rootBlock ->
          when (rootBlock) {
            is SingleSetBlock -> {
              suspend fun SequenceScope<MapLabelItem>.yieldSet(currentSet: ScryfallCardSet) {
                val (childSetsDefinitelyIncludedInRootSetBinder, childSetsWhichNeedToBeChecked) = currentSet.childSets.filterNot {
                  it.code.endsWith(currentSet.code) && it.code[0] in setOf('p', 't', 'f', 'm', 'w', 'o', 's', 'r')
                      || it.code.matches(Regex("pss\\d"))
                }.partition { it.binderPages <= thresholdTooFewPages }

                val setsInBinder = childSetsDefinitelyIncludedInRootSetBinder.toMutableList()
                childSetsWhichNeedToBeChecked.sortedByDescending(ScryfallCardSet::binderPages).forEach { childSet ->
                  if (currentSet.binderPages + setsInBinder.sumOf(ScryfallCardSet::totalBinderPages) + childSet.binderPages <= thresholdTooMuchPages) {
                    setsInBinder.add(childSet)
                  } else {
                    yieldSet(childSet)
                  }
                }
                setsInBinder.addFirst(currentSet)
                if (setsInBinder.sumOf(ScryfallCardSet::binderPages) > thresholdTooFewPages) {
                  yield(AbstractLabelItem(setsInBinder)) // TODO omit same icons
                }
              }

              yieldSet(rootBlock.set)
            }

            is MultiSetBlock -> {
              if (rootBlock.sets.sumOf(ScryfallCardSet::binderPages) <= thresholdTooMuchPages) {
                yield(MultiSetBlockLabel(rootBlock))
              } else {
                rootBlock.sets.forEach { yield(AbstractLabelItem(listOf(it))) }
              }
            }
          }
        }
      yield(PromosLabel)
      yield(GenericLabel("misc", "Miscellaneous"))
    }

    BinderLabels().printLabel(
      Path.of(".\\BinderLabels.pdf"),
      labels = labels.toList(),
//      labels = labels.filterNot { it.code == "j25" }.take(10).toList(),
      labelsPerPage = 5,
    )
  }
}
