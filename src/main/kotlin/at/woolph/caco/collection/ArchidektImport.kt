package at.woolph.caco.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.CardVersion
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import java.util.function.Predicate
import kotlin.collections.addAll
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter

private val log = LoggerFactory.getLogger("at.woolph.caco.collection.ArchidektImport")
fun importArchidekt(file: Path, notImportedOutputFile: Path = Path.of("not-imported.csv"), datePredicate: Predicate<Instant> =  Predicate { true }, clearBeforeImport: Boolean = false) {
  println("importing archidekt collection export file $file")
  transaction {
    if (clearBeforeImport) {
      println("current collection possessions are cleared!")
      CardPossessions.deleteAll()
    }
    CSVWriter(notImportedOutputFile.bufferedWriter()).use { writer ->
      CSVReader(file.bufferedReader()).use { reader ->
        val header = reader.readNext().also {
          writer.writeNext(buildList {
            addAll(it)
            add("Error")
          }.toTypedArray(), false)
        }.withIndex().associate { it.value to it.index }

        operator fun Array<String>.get(column: String): String? = header[column]?.let { this[it] }

        var countOfUnparsed = 0
        var countOfSkipped = 0
        var importedCards = 0
        generateSequence { reader.readNext() }.filter { it.size > 1 }.forEach { nextLine ->
          try {
            val dateAdded = nextLine["Date Added"]
              ?.let { LocalDate.parse(it).atStartOfDay().toInstant(ZoneOffset.UTC) }
              ?: Instant.now()

            if (datePredicate.test(dateAdded)) {
              val quantity = nextLine["Quantity"]!!.toInt()
              val foil = nextLine["Finish"] == "Foil"
              val language = CardLanguage.parse(nextLine["Language"]!!)
              val condition = when (nextLine["Condition"]) {
                "NM" -> CardCondition.NEAR_MINT
                "LP" -> CardCondition.EXCELLENT
                "MP" -> CardCondition.GOOD
                "HP" -> CardCondition.PLAYED
                "D" -> CardCondition.POOR
                else -> CardCondition.UNKNOWN
              }
              val scryfallId = UUID.fromString(nextLine["Scryfall ID"]!!)
              // TODO map to card collection item first then add to database
              repeat(quantity) {
                CardPossession.new {
                  val (card, cardVersion) =
                    Card.findById(scryfallId)?.let {
                      Pair(it, CardVersion.OG)
                    } ?: Card.findByPrereleaseStampedVersionId(scryfallId)?.let {
                      Pair(it, CardVersion.PrereleaseStamped)
                    } ?: Card.findByPromopackStampedVersionVersionId(scryfallId)?.let {
                      Pair(it, CardVersion.PromopackStamped)
                    }  ?: Card.findByTheListVersionId(scryfallId)?.let {
                      Pair(it, CardVersion.TheList)
                    } ?: throw NoSuchElementException("card with id $scryfallId not found")

                  this.card = card
                  this.language = language
                  this.condition = condition
                  this.foil = foil
                  this.cardVersion = cardVersion
                  this.dateOfAddition = dateAdded
                }
                importedCards++
              }
            } else {
              log.warn("not imported due to date restriction")
              countOfSkipped++
              writer.writeNext(buildList {
                addAll(nextLine)
                add("not imported due to date restriction")
              }.toTypedArray(), false)
            }
          } catch (e: Exception) {
            log.error("unable to import: ${e.message}", if (log.isDebugEnabled) e else null)
            countOfUnparsed++
            writer.writeNext(buildList {
              addAll(nextLine)
              add("${e.message}")
            }.toTypedArray(), false)
          }
        }
        if (countOfSkipped > 0) {
          println("$countOfSkipped were skipped due to date predicate!!")
        }
        if (countOfUnparsed > 0) {
          println("unable to import $countOfUnparsed lines")
        } else {
          println("all cards imported successfully ($importedCards in total)!!")
        }
      }
    }
  }
}
