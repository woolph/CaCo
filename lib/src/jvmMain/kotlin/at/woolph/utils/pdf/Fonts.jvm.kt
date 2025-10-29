package at.woolph.utils.pdf

import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts

actual fun PDFDocument.loadHelveticaRegular() =
  Font(PDType1Font(Standard14Fonts.FontName.HELVETICA))

actual fun PDFDocument.loadHelveticaBold() =
  Font(PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD))

actual fun PDFDocument.loadHelveticaOblique() =
  Font(PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE))