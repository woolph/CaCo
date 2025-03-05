package at.woolph.caco.collection

import at.woolph.caco.datamodel.collection.ArenaCardPossession
import at.woolph.caco.datamodel.collection.ArenaCardPossessions
import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Cards
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

val MTGA_COLLECTION_KEYWORD = "<== PlayerInventory.GetPlayerCardsV3"
val MTGA_LOG_FILE = File(System.getenv("APPDATA") + "\\..\\LocalLow\\Wizards Of The Coast\\MTGA\\output_log.txt")

fun importArenaCollection() {
    val regex = Regex("\"(\\d{5,})\": ([1,2,3,4]),?")
    val keyword = MTGA_COLLECTION_KEYWORD
    val result = mutableMapOf<Int, Int>()
    var copy = false
    var levels = 0

    MTGA_LOG_FILE.forEachLine { line ->
        if (copy) {
            regex.find(line)?.let {
                result.put(it.groupValues[1].toInt(), it.groupValues[2].toInt())
            }
        }

        if(line.contains(keyword)) {
            result.clear()
            copy = true
        }

        val addLevel = line.count { it == '{' }
        val removeLevel = line.count { it == '}' }
        levels += addLevel - removeLevel

        if(levels == 0 && removeLevel > 0) {
            copy = false
        }
    }

    transaction {
        result.forEach { arenaId, importedCount ->
            val importedCard = Card.find { Cards.arenaId.eq(arenaId) }.firstOrNull()
            if(importedCard != null)  {
                ArenaCardPossession.find { ArenaCardPossessions.card.eq(importedCard.id) }.singleOrNull()?.let {
                    it.count = importedCount
                } ?: ArenaCardPossession.new {
                    card = importedCard
                    count = importedCount
                }
            } else {
                //throw IllegalStateException("database does not know arenaId = $arenaId or there are multiples => import from scryfall? https://api.scryfall.com/cards/arena/$arenaId")
                println("unable to import $arenaId because it's not known https://api.scryfall.com/cards/arena/$arenaId")
            }
        }
    }
}
