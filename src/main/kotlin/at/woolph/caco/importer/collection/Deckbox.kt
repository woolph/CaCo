package at.woolph.caco.importer.collection

import at.woolph.caco.datamodel.collection.CardPossession
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.datamodel.sets.*
import at.woolph.caco.datamodel.sets.CardSet
import com.opencsv.CSVReader
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.FileReader

fun String.parseLanguageDeckbox(): CardLanguage = when (this) {
	"English" -> CardLanguage.ENGLISH
	"German" -> CardLanguage.GERMAN
	"Japanese" -> CardLanguage.JAPANESE
	"Russian" -> CardLanguage.RUSSIAN
	"Spanish" -> CardLanguage.SPANISH
	"Korean" -> CardLanguage.KOREAN
	"Italian" -> CardLanguage.ITALIAN
	"Portuguese" -> CardLanguage.PORTUGUESE
	"French" -> CardLanguage.FRENCH
	"Chinese" -> CardLanguage.CHINESE
	"Traditional Chinese" -> CardLanguage.CHINESE_TRADITIONAL
	else -> CardLanguage.UNKNOWN
}
fun CardLanguage.toLanguageDeckbox(): String = when (this) {
    CardLanguage.ENGLISH -> "English"
    CardLanguage.GERMAN -> "German"
    CardLanguage.JAPANESE -> "Japanese"
    CardLanguage.RUSSIAN -> "Russian"
    CardLanguage.SPANISH -> "Spanish"
    CardLanguage.KOREAN -> "Korean"
    CardLanguage.ITALIAN -> "Italian"
    CardLanguage.PORTUGUESE -> "Portuguese"
    CardLanguage.FRENCH -> "French"
    CardLanguage.CHINESE -> "Chinese"
    CardLanguage.CHINESE_TRADITIONAL -> "Traditional Chinese"
    else -> throw Exception("unknown language")
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
                    val set = CardSet.find { CardSets.name eq setName }.firstOrNull()
                            ?: throw Exception("set $setName not found")
                    val token = nextLine[3].startsWith("Extras: ")
                    val language = nextLine[6].parseLanguageDeckbox()
                    val condition = when (nextLine[5]) {
                        "Mint", "Near Mint" -> CardCondition.NEAR_MINT
                        "Good (Lightly Played)" -> CardCondition.EXCELLENT
                        "Played" -> CardCondition.GOOD
                        "Heavily Played" -> CardCondition.PLAYED
                        "Poor" -> CardCondition.POOR
                        else -> CardCondition.UNKNOWN
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
