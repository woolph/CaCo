package at.woolph.caco.collection

import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.sets.Card
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Path
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.addAll


fun importArchidekt(file: Path) {
  println("importing archidekt collection export file $file")
  transaction {
    CardPossessions.deleteAll()
    CSVWriter(FileWriter(File("not-imported.csv"))).use { writer ->
      CSVReader(FileReader(file.toFile())).use { reader ->
        val header = reader.readNext().also {
          writer.writeNext(buildList {
            addAll(it)
            add("Error")
          }.toTypedArray())
        }.withIndex().associate { it.value to it.index }

        operator fun Array<String>.get(column: String): String? = header[column]?.let { this[it] }

        var countOfUnparsed = 0
        var importedCards = 0
        generateSequence { reader.readNext() }.filter { it.size > 1 }.forEach { nextLine ->
          try {
            val quantity = nextLine["Quantity"]!!.toInt()
            val foil = nextLine["Finish"] == "Foil"
            val dateAdded = nextLine["Last Updated"]?.split(" ")?.get(0)?.let { LocalDate.parse(it) }
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
                this.card = Card.findById(scryfallId) ?: throw NoSuchElementException("card with id $scryfallId not found")
                this.language = language
                this.condition = condition
                this.foil = foil
                this.stampPrereleaseDate = stampPrereleaseDate
                this.stampPlaneswalkerSymbol = stampPlaneswalkerSymbol
                this.dateOfAddition = dateAdded ?: LocalDate.now()
              }
              importedCards ++
            }
          } catch (e: Exception) {
            println("unable to import: ${e.message}")
            countOfUnparsed++
            writer.writeNext(buildList {
              addAll(nextLine)
              add("${e.message}")
            }.toTypedArray())
          }
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
