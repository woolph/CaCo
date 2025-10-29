package at.woolph.utils.pdf

object Fonts {
  const val planewalkerBold = "/fonts/planewalker-bold.ttf"
  const val _72Black = "/fonts/72-Black.ttf"
  const val plantinItalic = "/fonts/mplantin-italic.ttf"
}

fun PDFDocument.loadFontPlanewalkerBold(): Font = loadFont(Fonts.planewalkerBold)

fun PDFDocument.loadFont72Black(): Font = loadFont(Fonts._72Black)

fun PDFDocument.loadPlantinItalic(): Font = loadFont(Fonts.plantinItalic)

expect fun PDFDocument.loadHelveticaRegular(): Font

expect fun PDFDocument.loadHelveticaBold(): Font

expect fun PDFDocument.loadHelveticaOblique(): Font
