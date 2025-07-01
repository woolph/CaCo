package at.woolph.caco.collection

import arrow.core.Either
import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.CardRepresentation
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*
import java.util.function.Predicate

fun importArchidekt(
  file: Path,
  notImportedOutputFile: Path = Path.of("not-imported.csv"),
  datePredicate: Predicate<Instant> =  Predicate { true },
  clearBeforeImport: Boolean = false) = import(
  file = file,
  notImportedOutputFile = notImportedOutputFile,
  datePredicate = datePredicate,
  clearBeforeImport = clearBeforeImport,
) { csvRecord ->
  val dateAdded = csvRecord["Date Added"]
    ?.let { LocalDate.parse(it).atStartOfDay().toInstant(ZoneOffset.UTC) }
    ?: Instant.now()

  val quantity = csvRecord["Quantity"]!!.toInt()
  val finish = Finish.parse(csvRecord["Finish"]!!)
  val language = CardLanguage.parse(csvRecord["Language"]!!)
  val condition = when (csvRecord["Condition"]) {
    "NM" -> CardCondition.NEAR_MINT
    "LP" -> CardCondition.EXCELLENT
    "MP" -> CardCondition.GOOD
    "HP" -> CardCondition.PLAYED
    "D" -> CardCondition.POOR
    else -> CardCondition.UNKNOWN
  }
  val purchasePrice = csvRecord["Purchase Price"]?.toDoubleOrNull()
  val scryfallId = Either.catch { UUID.fromString(csvRecord["Scryfall ID"]!!) }.bind()
  val (card, cardVariantType) = CardRepresentation.findByScryfallId(scryfallId)
    ?: raise(Exception("card with id $scryfallId not found"))
  return@import CardCollectionItem(
    quantity = quantity.toUInt(),
    cardCollectionItemId = CardCollectionItemId(
      card = card,
      finish = finish,
      language = language,
      condition = condition,
      variantType = cardVariantType,
    ),
    dateAdded = dateAdded,
    purchasePrice = purchasePrice,
  )
}
