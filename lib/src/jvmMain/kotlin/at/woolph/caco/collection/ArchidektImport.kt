/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.collection

import arrow.core.Either
import arrow.core.raise.Raise
import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.CardRepresentation
import at.woolph.caco.datamodel.sets.Finish
import at.woolph.utils.csv.CsvRecord
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.io.files.Path
import kotlin.time.Instant
import java.util.function.Predicate
import kotlin.text.toDoubleOrNull
import kotlin.text.toInt
import kotlin.time.Clock
import kotlin.uuid.Uuid

fun importArchidekt(
  file: Path,
  notImportedOutputFile: Path = Path("not-imported.csv"),
  datePredicate: Predicate<Instant> = Predicate { true },
  clearBeforeImport: Boolean = false,
) =
    import(
        file = file,
        notImportedOutputFile = notImportedOutputFile,
        datePredicate = datePredicate,
        clearBeforeImport = clearBeforeImport,
        mapper = Raise<Throwable>::mapArchitect,
    )

fun importSequenceArchidekt(
    file: Path,
    notImportedOutputFile: Path = Path("not-imported.csv"),
): Sequence<Either<Throwable, CardCollectionItem>> =
    importSequence(
        file,
        notImportedOutputFile,
        mapper = Raise<Throwable>::mapArchitect,
    )

fun Raise<Throwable>.mapArchitect(csvRecord: CsvRecord): CardCollectionItem {
  val dateAdded =
      csvRecord["Date Added"]?.let { kotlinx.datetime.LocalDate.parse(it).atTime(LocalTime(0,0,0)).toInstant(TimeZone.UTC) }
          ?: Clock.System.now()

  val quantity = csvRecord["Quantity"]!!.toInt()
  val finish = Finish.parse(csvRecord["Finish"]!!)
  val language = CardLanguage.parse(csvRecord["Language"]!!)
  val condition =
      when (csvRecord["Condition"]) {
        "NM" -> CardCondition.NEAR_MINT
        "LP" -> CardCondition.EXCELLENT
        "MP" -> CardCondition.GOOD
        "HP" -> CardCondition.PLAYED
        "D" -> CardCondition.POOR
        else -> CardCondition.UNKNOWN
      }
  val purchasePrice = csvRecord["Purchase Price"]?.toDoubleOrNull()
  val scryfallId = Either.catch { Uuid.parse(csvRecord["Scryfall ID"]!!) }.bind()
  val (cardPrint, cardPrintVariantType) =
      CardRepresentation.findByScryfallId(scryfallId)
          ?: raise(Exception("card with id $scryfallId not found"))
  return CardCollectionItem(
      quantity = quantity.toUInt(),
      cardCollectionItemId =
          CardCollectionItemId(
              cardPrint = cardPrint,
              finish = finish,
              language = language,
              condition = condition,
              variantType = cardPrintVariantType,
          ),
      dateAdded = dateAdded,
      purchasePrice = purchasePrice,
  )
}
