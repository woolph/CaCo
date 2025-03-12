package at.woolph.caco.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardVersion
import at.woolph.caco.utils.Either
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
  mapper = ::mapArchidekt,
)

private fun mapArchidekt(csvRecord: CsvRecord): Either<CardCollectionItem, String> {
  val dateAdded = csvRecord["Date Added"]
    ?.let { LocalDate.parse(it).atStartOfDay().toInstant(ZoneOffset.UTC) }
    ?: Instant.now()

  val quantity = csvRecord["Quantity"]!!.toInt()
  val foil = csvRecord["Finish"] == "Foil"
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
  val scryfallId = UUID.fromString(csvRecord["Scryfall ID"]!!)
  val (card, cardVersion) =
    Card.findById(scryfallId)?.let {
      Pair(it, CardVersion.OG)
    } ?: Card.findByPrereleaseStampedVersionId(scryfallId)?.let {
      Pair(it, CardVersion.PrereleaseStamped)
    } ?: Card.findByPromopackStampedVersionVersionId(scryfallId)?.let {
      Pair(it, CardVersion.PromopackStamped)
    }  ?: Card.findByTheListVersionId(scryfallId)?.let {
      Pair(it, CardVersion.TheList)
    } ?: return Either.Right("card with id $scryfallId not found")

  return Either.Left(CardCollectionItem(
    quantity = quantity.toUInt(),
    cardCollectionItemId = CardCollectionItemId(
      card = card,
      foil = foil,
      language = language,
      condition = condition,
      cardVersion = cardVersion,
    ),
    dateAdded = dateAdded,
    purchasePrice = purchasePrice,
  ))
}