package at.woolph.caco.cli

import at.woolph.caco.binderlabels.*
import at.woolph.caco.datamodel.Databases
import at.woolph.libs.pdf.*
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.net.URI
import java.nio.file.Path

class BinderLabels(
) {
  fun printLabel(file: Path, labels: List<MapLabelItem>, labelsPerPage: Int) {
    createPdfDocument(file) {
      val fontColor = if (darkLabels) Color.WHITE else Color.BLACK
      val fontSubColor = if (darkLabels) Color.LIGHT_GRAY else Color.GRAY
      val backgroundColor: Color? = if (darkLabels) Color.BLACK else null

      val mtgLogo = createFromFile(Path.of("./assets/images/mtg.png"))

      val fontFamilyPlanewalker = loadType0Font(javaClass.getResourceAsStream("/fonts/PlanewalkerBold-xZj5.ttf")!!)
      val fontFamily72Black = loadType0Font(javaClass.getResourceAsStream("/fonts/72-Black.ttf")!!)

      val fontTitle = Font(fontFamilyPlanewalker, 48f)
      val fontSubTitle = Font(fontFamilyPlanewalker, 16f)
      val fontCode = Font(fontFamily72Black, 28f)
      val fontCodeCommanderSubset = Font(fontFamily72Black, 14f)

      val pageFormat = PDRectangle.A4

      val columnWidth = pageFormat.width / labelsPerPage
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

      val titleXPosition = (maximumWidth + fontTitle.height) * 0.5f
      val titleYPosition = setDivYPos - 3 * defaultGapSize
      val subTitleXPosition = titleXPosition + fontSubTitle.height + defaultGapSize
      val subTitleYPosition = titleYPosition - 30f


      columnedContent(labels, pageFormat, labelsPerPage) { set ->
        drawBorder(2f, fontColor)
        if (set != BlankLabel) {
          backgroundColor?.let { drawBackground(it) }

          frame(margin, margin, margin, margin) {
            drawAsImageCentered(
              mtgLogo,
              mtgLogoWidth,
              mtgLogoHeight,
              0f,
              magicLogoYPosition,
            )

            set.subTitle?.let { _subTitle ->
              adjustTextToFitWidth(
                _subTitle,
                fontSubTitle,
                maxTitleWidth,
                10f
              ).let { (title, font) ->
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

            adjustTextToFitWidth(
              set.title,
              fontTitle,
              maxTitleWidth,
              36f
            ).let { (title, font) ->
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
}

fun main() {
  Databases.init()

  transaction {
    BinderLabels().printLabel(
      Path.of(".\\BinderLabels.pdf"),
      labels = listOf(
        PromosLabel,
        GenericLabel("misc", "Miscellaneous"),
        SetWithCommander("neo", "nec"),
        SetWithCommander("khm", "khc"),
        SetWithCommander("znr", "znc"),
        SetWithCommander("mid", "mic"),
        SetWithCommander("vow", "voc"),
        SimpleSet("afc"),
        SimpleSet("c18"),
        SimpleSet("c19"),
        SimpleSet("c20"),
        SimpleSet("c21"),
        SimpleSet("cmr"),
        SimpleSet("cm2"),
        SimpleSet("m19"),
        SimpleSet("m20"),
        SimpleSet("m21"),
        SimpleSet("afr"),
        SimpleSet("stx"),
        SimpleSet("iko"),
        SimpleSet("dom"),
        SimpleSet("thb"),
        SimpleSet("eld"),
        SimpleSet("war"),
        object : SimpleSet("rna") {
          override val subTitle = "incl. Guild Kit"
        },
        object : SimpleSet("grn") {
          override val subTitle = "incl. Guild Kit"
        },
        Block("Conspiracy Sets", "cns", "cn2"),
        SimpleSet("tsr"),
        SimpleSet("uma"),
        SimpleSet("2xm"),
        SimpleSet("mh1"),
        SimpleSet("mh2"),
        SimpleSet("a25"),
        SimpleSet("ima"),
        SimpleSet("mm3"),
        SimpleSet("ema"),
        SimpleSet("mm2"),
        SimpleSet("mma"),
        SimpleSet("4ed"),
        SimpleSet("5ed"),
        SimpleSet("6ed"),
        SimpleSet("jmp"),
        SimpleSet("ust"),
        SimpleSet("unf"),
        Block("Portal Sets", "por", "p02", "ptk"),
        Block("Urza Block", "usg", "ulg", "uds"),
        Block("Tempest Block", "tmp", "sth", "exo"),
        Block("Mirage Block", "mir", "vis", "wth"),
        Block("Mercadian Block", "mmq", "nem", "pcy"),
        Block("Invasion Block", "inv", "pls", "apc"),
        Block("Ixalan Block", "xln", "rix"),
        Block("Amonkhet Block", "akh", "hou"),
        Block("Kaladesh Block", "kld", "aer"),
        Block("Shadow over Innistrad Block", "soi", "emn"),
        SimpleSet("bbd"),
        Block("Planechase Sets", "hop", "pc2", "pca"),
        Block("Odyssey Block", "ody", "tor", "jud"),
        Block("Battle for Zendikar Block", "bfz", "ogw"),
        SimpleSet("ori"),
        SimpleSet("c17"),
        SimpleSet("snc"),
        SimpleSet("c16"),
        SimpleSet("c15"),
        SimpleSet("c14"),
        SimpleSet("c13"),
        SimpleSet("cmd"),
        SimpleSet("cma"),
        Block("Khans of Tarkir Block", "ktk", "frf", "dtk"),
        SimpleSet("m15"),
        Block("Theros Block", "ths", "bng", "jou"),
        SimpleSet("m14"),
        Block("Return to Ravnica Block", "rtr", "gtc", "dgm"),
        SimpleSet("m13"),
        Block("Innistrad Block", "isd", "dka", "avr"),
        SimpleSet("m12"),
        Block("Scars of Mirrodin Block", "som", "mbs", "nph"),
        SimpleSet("m11"),
        Block("Zendikar Block", "zen", "wwk", "roe"),
        SimpleSet("m10"),
        Block("Alara Block", "ala", "con", "arb"),
        Block("Shadowmoor Block", "shm", "eve"),
        Block("Lorwyn Block", "lrw", "mor"),
        SimpleSet("10e"),
        Block("Time Spiral Block", "tsp", "plc", "fut"),
        Block("Ravnica Block", "rav", "gpt", "dis"),
        SimpleSet("9ed"),
        Block("Kamigawa Block", "chk", "bok", "sok"),
        Block("Mirrodin Block", "mrd", "dst", "5dn"),
        SimpleSet("8ed"),
        Block("Onslaught Block", "ons", "lgn", "scg"),
        SimpleSet("7ed"),
        Block("Ice Age Block", "ice", "all", "csp"),
        SetWithCommanderAndAncillary("znr", "znc", "zne"),
        SetWithCommander("stx", "sta"),
        SimpleSet("dmr"),
        SimpleSet("j22"),
        SimpleSet("ugl"),
        SimpleSet("unh"),
        SimpleSet("und"),
        SimpleSet("ust"),
        SimpleSet("unf"),
        SimpleSet("gnt"),
        SimpleSet("gn2"),
        SimpleSet("gn3"),
        SetWithCommanderAndAncillary("bro", "brc", "brr"),
        SetWithCommander("one", "onc"),
        SimpleSet("who"),
        SimpleSet("ltc"),
        SimpleSet("scd"),
        SimpleSet("ltr"),
        SimpleSet("clb"),
        SetWithCommanderAndAncillary("mom", "mat", "mul"),
        SimpleSet("moc"),
        SetWithCommander("lci", "rex"),
        SimpleSet("lcc"),
        SimpleSet("rvr"),
        SetWithCommanderAndAncillary("woe", "woc", "wot"),
        SimpleSet("pip"),
        SetWithCommander("mkm", "clu"),
        SimpleSet("mkc"),
        SimpleSet("mh3"),
        SetWithCommanderAndAncillary("otj", "otp", "big"),
        SimpleSet("spg"),
        SimpleSet("2x2"),
        SeparatePreconPartSet("cmr"),
        BlankLabel,
        SimpleSet("cmm"),
        SeparatePreconPartSet("cmm"),
        SimpleSet("otc"),
        SimpleSet("dbl"),
        SimpleSet("m3c"),
        SimpleSet("acr"),
        SimpleSet("blb"),
        SimpleSet("blc"),
        SimpleSet("dsk"),
        SimpleSet("dsc"),
        SeparatePreconPartSet("clb"),

        SetWithCommander("fdn", "fdc"),
        SetWithCommander("dft", "drc"),
        SimpleSet("inr"),
        object : MapLabelItem {
          override val code: String = "PRM"
          override val subCode: String = "SPG"
          override val title: String = "Promos et al"
          override val subTitle: String = "incl. Special Guests"
          override val mainIcon: ByteArray? by lazy { URI("https://c2.scryfall.com/file/scryfall-symbols/sets/star.svg?1624852800").renderSvgAsMythic() }
          override val subIconRight: ByteArray? by lazy { URI("https://c2.scryfall.com/file/scryfall-symbols/sets/spg.svg?1624852800").renderSvgAsMythic() }
        },
        Block("Portal Sets", "por", "por", "p02", "ptk", "itp"),
        Block("Game Night", "gnt", "gnt", "gn2", "gn3"),
        Block("Mystery Booster", "mb2", "mb2", "cmb1", "cmb2"),
        SimpleSet("tdm"),
        SimpleSet("tdc"),
        SimpleSet("fin"),
        SimpleSet("fic"),
        SimpleSet("j25"),
      ).takeLast(12),
      labelsPerPage = 6,
    )
  }
}
