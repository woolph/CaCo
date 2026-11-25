package at.woolph.caco.cli.command.collection

import at.woolph.caco.collection.labels.box.ArtSeriesLabel
import at.woolph.caco.collection.labels.box.AwaitingCatalogizationBoxLabel
import at.woolph.caco.collection.labels.box.CollectionBoxLabel
import at.woolph.caco.collection.labels.box.DuplicateBoxLabel
import at.woolph.caco.collection.labels.box.GenericBoxLabel
import at.woolph.caco.collection.labels.box.PlanechaseBoxLabel
import at.woolph.caco.collection.labels.box.ReturnToCollectionBoxLabel
import at.woolph.caco.collection.labels.box.SchemeBoxLabel
import at.woolph.caco.collection.labels.box.SnowCoveredBasicsAndWastes
import at.woolph.caco.collection.labels.box.SubtitledDuplicateBoxLabel
import at.woolph.caco.collection.labels.printBoxLabel
import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer
import at.woolph.lib.clikt.SuspendingTransactionCliktCommand
import at.woolph.utils.io.toKotlinxPath
import at.woolph.utils.toRoman
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path

class PrintCollectionBoxLabels : SuspendingTransactionCliktCommand(name = "box-labels") {
  val output by option("--output", "-o").path(canBeDir = false, canBeFile = true).required()

  override suspend fun runTransaction() {
    val labels = // TODO determine the labels automatically (decide by the amount of duplicate cards, how many sets go into one box)
      listOf(
//          ArtSeriesLabel(),
          SchemeBoxLabel, // FIXME symbol
          PlanechaseBoxLabel(1),
          PlanechaseBoxLabel(2),
//          SnowCoveredBasicsAndWastes,
          ReturnToCollectionBoxLabel(),
        //                  AwaitingCollectionBoxLabel(1, "Older sets (w/o binder)"),
        //                  AwaitingCollectionBoxLabel(2, "Older sets (w/o binder)"),
        //                  AwaitingCollectionBoxLabel(3, "Older sets (w/o binder)"),
        //                  AwaitingCollectionBoxLabel(4, "Commander sets (w/o binder)"),
        //                  AwaitingCollectionBoxLabel(5, "Masters & Duel Decks"),
          AwaitingCatalogizationBoxLabel(
              subtitle = "Double-Sided Tokens, Lands, Promos, & Placeholder"
          ),
        object : GenericBoxLabel("Deck Building") {
          override val subtitle: String = "Commander"
          override val icon by lazySetIcon("cmd", mythicBinderLabelIconRenderer)
        },
        object : GenericBoxLabel("Deck Building") {
          override val subtitle: String = "Pioneer & Pauper"
        },
          DuplicateBoxLabel("rix"),
          DuplicateBoxLabel("rix", "kld", "aer", "akh", "hou"),
          DuplicateBoxLabel("soi", "emn"),
        //                  SubtitledDuplicateBoxLabel(subtitle = "Core Set 2021 Pt. I",
        // "m21"),
        //                  SubtitledDuplicateBoxLabel(subtitle = "Core Set 2021 Pt. II",
        // "m21"),
          DuplicateBoxLabel("mid"),
          DuplicateBoxLabel("vow"),
          DuplicateBoxLabel("neo"),
          DuplicateBoxLabel("bbd"),
          DuplicateBoxLabel("afr"),
          SubtitledDuplicateBoxLabel(
              subtitle = "Older Sets",
              "por",
              "p02",
              "ptk",
              "usg",
              "ulg",
              "uds",
              "inv",
              "pls",
              "apc",
              "mmq",
              "nem",
              "pcy",
          ),
          SubtitledDuplicateBoxLabel(
              subtitle = "Weird Sets",
              "ust",
              "unh",
              "unf",
              "hop",
              "pc2",
              "pca",
              "jmp",
              "j22",
              "j25",
              "gnt",
              "gn2",
              "gn3",
          ),
          SubtitledDuplicateBoxLabel(
              subtitle = "White-Bordered Sets",
              "2ed",
              "3ed",
              "4ed",
              "5ed",
              "6ed",
              "7ed",
              "8ed",
              "9ed",
              "itp",
          ),
          SubtitledDuplicateBoxLabel(
              subtitle = "Promos & Special Guests",
              "pspl",
              "spg"
          ),
          SubtitledDuplicateBoxLabel(
              subtitle = "Commander Sets I",
              "c16",
              "c17",
              "c18",
              "c19",
              "c20",
              "c21",
              "cm2",
              "cmr",
              "cmm",
              "clb",
              "m3c",
              "scd",
          ),
          DuplicateBoxLabel("stx", "sta"),
//          SubtitledDuplicateBoxLabel(
//              subtitle = "incl. Innistrad: Double Feature",
//              "mid",
//              "vow",
//          ),
          DuplicateBoxLabel("mh1", "mh2", "mh3"),
          DuplicateBoxLabel("rvr", "inr"),
        *listOf(
          "sos",
          "ecl",
          "eoe",
          "tdm",
          "dft",
          "fdn",
          "dsk",
          "blb",
          "big",
          "otp",
          "otj",
          "clu",
          "mkm",
          "rex",
          "lci",
          "woe",
          "mat",
          "mul",
          "mom",
          "one",
          "bot",
          "bro",
          "dmu",
          "snc",
          "neo",
        )
          .reversed()
          .chunked(SubtitledDuplicateBoxLabel.MAX_SETS_FOR_TWO_ROWS) // TODO take box card limit into account
          .withIndex()
          .map { (i, it) ->
              SubtitledDuplicateBoxLabel(
                  subtitle = "Standard Sets ${(i + 1).toRoman()}",
                  *it.toTypedArray(),
              )
          }
          .toTypedArray(),
        *listOf(
          listOf(
            "fem",
            "arn",
            "leg",
            "ice",
            "hml",
            "all",
            "csp",
            null,
            null,
            "mir",
            "vis",
            "wth",
            "tmp",
            "sth",
            "exo",
            "ody",
            "tor",
            "jud",
            "ons",
            "lgn",
            "scg",
            "mrd",
            "dst",
            "5dn",
          ),
          listOf(
            "chk",
            "bok",
            "sok",
            "rav",
            "gpt",
            "dis",
            "tsp",
            "plc",
            "fut",
            "lrw",
            "mor",
            null,
            "eve",
            "shm",
            null,
            "ala",
            "con",
            "arb",
            "zen",
            "wwk",
            "roe",
            "som",
            "mbs",
            "nph",
            "isd",
            "dka",
            "avr",
          ),
          listOf(
            "rtr",
            "gtc",
            "dgm",
            "ths",
            "bng",
            "jou",
            "ktk",
            "frf",
            "dtk",
            "bfz",
            "ogw",
            null,
          ),
        )
          .withIndex()
          .map { (i, it) ->
              CollectionBoxLabel(
                  subtitle = "Standard Sets ${(i + 1).toRoman()}",
                  *it.toTypedArray(),
              )
          }
          .toTypedArray(),
        *listOf(
          listOf(
            "3ed",
            "7ed",
            "8ed",
            "9ed",
            "10e",
            "m10",
            "m11",
            "m12",
            "m13",
            "m14",
            "m15",
            "ori",
            "a25",
            "ima",
            "ema",
            "mma",
            "mm2",
            "mm3",
          ),
        )
          .withIndex()
          .map { (i, it) ->
              CollectionBoxLabel(
                  subtitle = "Core & Master Sets ${(i + 1).toRoman()}",
                  *it.toTypedArray(),
              )
          }
          .toTypedArray(),
        *listOf(
          listOf(
            "dd1",
            "dd2",
            "ddc",
            "ddd",
            "dde",
            "ddf",
            "ddg",
            "ddh",
            "ddi",
            "ddj",
            "ddk",
            "ddl",
            "ddm",
            "ddn",
            "ddo",
            "ddp",
            "ddq",
            "ddr",
            "dds",
            "ddt",
            "ddu",
            "gs1",
            "pd2",
            "md1",
            "w16",
            "w17",
          ),
        )
          .withIndex()
          .map { (i, it) ->
              CollectionBoxLabel(
                  subtitle = "Duel Deck Sets ${(i + 1).toRoman()}",
                  *it.toTypedArray(),
              )
          }
          .toTypedArray(),
        *listOf(
          listOf(
            "cmd",
            "c13",
            "c14",
            "c15",
            "c16",
            "c17",
            "cma",
            "cmm",
            null,
            "arc",
            "e01",
            "e02",
            "drb",
            "v09",
            "v10",
            "v11",
            "v12",
            "v13",
            "v14",
            "v15",
            "v16",
            "v17",
            null,
            null,
            "dbl",
          ),
        )
          .withIndex()
          .map { (i, it) ->
              CollectionBoxLabel(
                  subtitle = "Commander & Misc Sets ${(i + 1).toRoman()}",
                  *it.toTypedArray(),
              )
          }
          .toTypedArray(),
        *listOf(
          "soc",
          "ecc",
          "eoc",
          "tdc",
          "drc",
          "fdc",
          "dsc",
          "blc",
          "otc",
          "mkc",
          "lcc",
          "woc",
          "moc",
          "onc",
          "brc",
          "dmc",
          "ncc",
          "nec",
          "voc",
          "mic",
          "afc",
          "khc",
          "znc",
        )
          .reversed()
          .chunked(SubtitledDuplicateBoxLabel.MAX_SETS_FOR_TWO_ROWS)
          .withIndex()
          .map { (i, it) ->
              SubtitledDuplicateBoxLabel(
                  subtitle = "Commander Sets ${(i + 1).toRoman()}",
                  *it.toTypedArray(),
              )
          }
          .toTypedArray(),
          DuplicateBoxLabel("tsr", "dmr"),
        *listOf(
          "pip",
          "who",
          "40k",
          "acr",
          "ltr",
          "ltc",
          "fin",
          "fic",
          "spm",
          "tla",
          "tmt",
        )
          .chunked(SubtitledDuplicateBoxLabel.MAX_SETS_FOR_TWO_ROWS) // TODO take box card limit into account
          .withIndex()
          .map { (i, it) ->
              SubtitledDuplicateBoxLabel(
                  subtitle = "UB Sets ${(i + 1).toRoman()}",
                  *it.toTypedArray(),
              )
          }
          .toTypedArray(),
      )

      printBoxLabel(
          file = output.toKotlinxPath(),
          labels = labels.toList(),
      )
  }
}
