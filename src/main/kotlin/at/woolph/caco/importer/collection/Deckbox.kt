package at.woolph.caco.importer.collection

import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.collection.Condition
import at.woolph.caco.datamodel.sets.*
import at.woolph.caco.datamodel.sets.Set
import com.opencsv.CSVReader
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.FileReader

fun getLatestDeckboxCollectionExport(dir: File) = dir.let {
    if(it.isDirectory) {
        it.listFiles().asList().onEach { println("content $it") }
                .filter { it.name.toString().let { it.startsWith("Inventory") && it.endsWith(".csv")} }
                .onEach {println("passed filter $it") }
                .maxBy { it.lastModified() }
    } else {
        it
    }
}

fun importDeckbox(file: File) {
    println("importing deckbox collection export file $file")
    transaction {
        CardPossessions.deleteAll()

        val reader = CSVReader(FileReader(file))
        if (reader.readNext() != null) { // skip header
            var nextLine: Array<String>? = reader.readNext()
            while (nextLine != null) {
                try {
                    val count = nextLine[0].toInt()
                    val cardName = nextLine[2]
                    val setName = nextLine[3].removePrefix("Prerelease Events: ").removePrefix("Extras: ")
                    val set = Set.find { Sets.name eq setName }.firstOrNull()
                            ?: throw Exception("set $setName not found")
                    val token = nextLine[3].startsWith("Extras: ")
                    val language = when (nextLine[6]) {
                        "English" -> "en"
                        "German" -> "de"
                        else -> "??"
                    }
                    val condition = when (nextLine[5]) {
                        "Mint", "Near Mint" -> Condition.NEAR_MINT
                        "Good (Lightly Played)" -> Condition.EXCELLENT
                        "Played" -> Condition.GOOD
                        "Heavily Played" -> Condition.PLAYED
                        "Poor" -> Condition.POOR
                        else -> Condition.UNKNOWN
                    }
                    val foil = when {
                        nextLine[3].startsWith("Prerelease Events: ") -> Foil.PRERELASE_STAMPED_FOIL
                        nextLine[7] == "foil" -> Foil.FOIL
                        else -> Foil.NONFOIL
                    }
                    val cardNumber = when{
                        foil == Foil.PRERELASE_STAMPED_FOIL -> "P"
                        token -> "T"
                        else -> ""
                    } + nextLine[4].padStart(3, '0') + when {
                        foil == Foil.PRERELASE_STAMPED_FOIL -> "s"
                        else -> ""
                    }
                    val card = Card.find { (Cards.numberInSet eq cardNumber) and (Cards.set eq set.id) and (Cards.token eq token)}.firstOrNull()
                            ?: throw Exception("card #$cardNumber (\"$cardName\") not found in set $setName")

                    repeat(count) {
                        CardPossession.new {
                            this.card = card
                            this.language = language
                            this.condition = condition
                            this.foil = foil
                        }
                    }
                } catch (e: Exception) {
                    println("unable to import: ${e.message}")
                }
                nextLine = reader.readNext()
            }
        }
    }
}