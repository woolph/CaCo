/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.binderlabels

import arrow.core.getOrElse
import at.woolph.caco.datamodel.sets.IScryfallCardSet
import at.woolph.caco.datamodel.sets.MultiSetBlock
import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.datamodel.sets.ScryfallCardSets
import at.woolph.caco.datamodel.sets.SingleSetBlock
import at.woolph.utils.pdf.Font
import at.woolph.utils.pdf.HorizontalAlignment
import at.woolph.utils.pdf.pdfDocument
import at.woolph.utils.pdf.paginatedColumnedContent
import at.woolph.utils.pdf.drawAsImageCentered
import at.woolph.utils.pdf.drawBackground
import at.woolph.utils.pdf.drawBorder
import at.woolph.utils.pdf.drawText
import at.woolph.utils.pdf.frame
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.outputStream

fun fetchCardSets(codes: Iterable<String>): List<ScryfallCardSet> = transaction {
  codes.map { code ->
    ScryfallCardSet.findByCode(code)
        ?: throw IllegalArgumentException("no set with code $code found")
  }
}

fun fetchCardSetsNullable(codes: Iterable<String?>): List<ScryfallCardSet?> = transaction {
  codes.map {
    it?.let { code ->
      ScryfallCardSet.findByCode(code)
          ?: throw IllegalArgumentException("no set with code $code found")
    }
  }
}

fun fetchCardSetsNullable(vararg codes: String?) = fetchCardSetsNullable(codes.asIterable())

fun determineBinderLabels(thresholdTooMuchPages: Int, thresholdTooFewPages: Int): Sequence<MapLabelItem> =
    sequence {
      // TODO I would like to habe some reassignments of parent stuff (pltc is child of ltr, but
      // I'd like it to be a child of ltc, H1R should be MH1 child, H2R of MH2, GK1 of
      ScryfallCardSet.rootSetsGroupedByBlocks {
        (ScryfallCardSets.digitalOnly eq false).and(ScryfallCardSets.cardCount greater 12)
      }
        .filterNot { it is SingleSetBlock && it.set.code in setOf("sld", "30a", "unk") }
        .forEach { rootBlock ->
          when (rootBlock) {
            is SingleSetBlock -> {
              suspend fun SequenceScope<MapLabelItem>.yieldSet(currentSet: IScryfallCardSet) {
                val (
                  childSetsDefinitelyIncludedInRootSetBinder,
                  childSetsWhichNeedToBeChecked) =
                  currentSet.childSets
                    .filterNot {
                      it.code.endsWith(currentSet.code) &&
                        it.code[0] in setOf('p', 't', 'f', 'm', 'w', 'o', 's', 'r') ||
                        it.code.matches(Regex("pss\\d"))
                    }
                    .partition { it.binderPages <= thresholdTooFewPages }

                val setsInBinder = childSetsDefinitelyIncludedInRootSetBinder.toMutableList()
                childSetsWhichNeedToBeChecked
                  .sortedByDescending(IScryfallCardSet::binderPages)
                  .forEach { childSet ->
                    if (
                      currentSet.binderPages +
                      setsInBinder.sumOf(IScryfallCardSet::totalBinderPages) +
                      childSet.binderPages <= thresholdTooMuchPages
                    ) {
                      setsInBinder.add(childSet)
                    } else {
                      yieldSet(childSet)
                    }
                  }
                setsInBinder.addFirst(currentSet)
                if (setsInBinder.sumOf(IScryfallCardSet::binderPages) > thresholdTooFewPages) {
                  yield(AbstractLabelItem(setsInBinder)) // TODO omit same icons
                }
              }

              yieldSet(rootBlock.set)
            }

            is MultiSetBlock -> {
              if (
                rootBlock.sets.sumOf(IScryfallCardSet::binderPages) <= thresholdTooMuchPages
              ) {
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

fun printBinderLabel(file: Path, labels: List<MapLabelItem>, labelsPerPage: Int) {
  pdfDocument(file.createParentDirectories().outputStream()) {
    val fontColor = Color.BLACK
    val subTitleFontColor = Color.GRAY
    val backgroundColor: Color? = null

    val mtgLogo = createFromFile(Path.of("./assets/images/mtg.png"))

    val fontFamilyPlanewalker = loadType0Font("/fonts/planewalker-bold.ttf").getOrElse { throw it }
    val fontPlantin = loadType0Font("/fonts/mplantin-italic.ttf").getOrElse { throw it }
    val fontFamily72Black = loadType0Font("/fonts/72-Black.ttf").getOrElse { throw it }

    val pageFormat = PDRectangle.A4
    val columnWidth = pageFormat.width / labelsPerPage

    val fontTitle = Font(fontFamilyPlanewalker, columnWidth * 0.5f)
    val fontSubTitle = Font(fontPlantin, columnWidth * 0.14f)
    val fontCode = Font(fontFamily72Black, columnWidth * 0.28f)
    val fontCodeCommanderSubset = Font(fontFamily72Black, columnWidth * 0.14f)

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

    val setDivHeight =
      desiredHeight +
        fontCode.totalHeight +
        fontCodeCommanderSubset.totalHeight +
        defaultGapSize
    val setDivYPos = maximumHeight - setDivHeight

    val maxTitleWidth =
      pageFormat.height -
        magicLogoYPosition -
        mtgLogoHeight -
        defaultGapSize -
        setDivHeight -
        2 * margin -
        2 * defaultGapSize

    val titleYPosition = setDivYPos - 3 * defaultGapSize
    val subTitleYPosition = titleYPosition - 30f

    paginatedColumnedContent(labels, pageFormat, labelsPerPage) { set ->
      try {
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

          fontTitle.adjustedTextToFitWidth(
            set.title,
            maxTitleWidth,
          ) { title, font ->
              val titleXPosition = (maximumWidth + font.height) * 0.5f

              fontSubTitle.adjustedTextToFitWidth(
                set.subTitle,
                maxTitleWidth,
              ) { subTitle, subTitleFont ->
                  val subTitleXPosition = titleXPosition + subTitleFont.height + defaultGapSize
                  drawText(
                    subTitle,
                    subTitleFont,
                    subTitleFontColor,
                    subTitleXPosition,
                    subTitleYPosition,
                    90.0,
                  )
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
              fontColor,
            )

            set.subCode?.let { subCode ->
              drawText(
                subCode.uppercase(),
                fontCodeCommanderSubset,
                HorizontalAlignment.CENTER,
                0f,
                subCodeYPos,
                subTitleFontColor,
              )
            }

            val maximumWidthSub = 20f
            val desiredHeightSub = 20f
            val xOffsetIcons = 27f
            val yOffsetIcons = mainIconYPos + desiredHeight - 15f

            set.subIconRight?.let {
              drawAsImageCentered(
                createFromByteArray(it, "subIconRight(${set.title})"),
                maximumWidthSub,
                desiredHeightSub,
                xOffsetIcons,
                yOffsetIcons,
              )
            }
            set.subIconLeft?.let {
              drawAsImageCentered(
                createFromByteArray(it, "subIconLeft(${set.title})"),
                maximumWidthSub,
                desiredHeightSub,
                -xOffsetIcons,
                yOffsetIcons,
              )
            }

            val xOffsetIcons2 = 32f + 15f
            val yOffsetIcons2 = mainIconYPos + 10f

            set.subIconRight2?.let {
              drawAsImageCentered(
                createFromByteArray(it, "subIconRight2(${set.title})"),
                maximumWidthSub,
                desiredHeightSub,
                xOffsetIcons2,
                yOffsetIcons2,
              )
            }
            set.subIconLeft2?.let {
              drawAsImageCentered(
                createFromByteArray(it, "subIconLeft2(${set.title})"),
                maximumWidthSub,
                desiredHeightSub,
                -xOffsetIcons2,
                yOffsetIcons2,
              )
            }

            set.mainIcon?.let {
              drawAsImageCentered(
                createFromByteArray(it, "mainIcon(${set.title})"),
                maximumWidth,
                desiredHeight,
                0f,
                mainIconYPos,
              )
            }
          }
        }
      } catch (ex: Exception) {
        println("Error with set ${set.title}: ${ex.message}")
      }
    }
  }
}
