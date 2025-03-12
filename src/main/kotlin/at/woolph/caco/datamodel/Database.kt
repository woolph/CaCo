package at.woolph.caco.datamodel

import at.woolph.caco.datamodel.collection.ArenaCardPossessions
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.decks.Builds
import at.woolph.caco.datamodel.decks.DeckArchetypes
import at.woolph.caco.datamodel.decks.DeckCards
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.datamodel.sets.ScryfallCardSets
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

object Databases {
    val cacoDatabase = Database.connect("jdbc:h2:~/.caco/caco-database", driver = "org.h2.Driver")

    init {
        TransactionManager.defaultDatabase = cacoDatabase
    }

    fun init() {
        transaction(cacoDatabase) {
            SchemaUtils.createMissingTablesAndColumns(ScryfallCardSets, Cards, CardPossessions, DeckArchetypes, Builds, DeckCards, ArenaCardPossessions)
        }
    }
}
