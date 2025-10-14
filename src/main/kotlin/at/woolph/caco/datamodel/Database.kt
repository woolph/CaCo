/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel

import at.woolph.caco.HomeDirectory
import at.woolph.caco.datamodel.collection.ArenaCardPossessions
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.decks.Builds
import at.woolph.caco.datamodel.decks.DeckArchetypes
import at.woolph.caco.datamodel.decks.DeckCards
import at.woolph.caco.datamodel.sets.CardVariants
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.datamodel.sets.ScryfallCardSets
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

object Databases {
    fun init(homeDirectory: HomeDirectory = HomeDirectory()) {
        val cacoDatabase = Database.connect("jdbc:h2:${homeDirectory}/caco-database", driver = "org.h2.Driver")
        TransactionManager.defaultDatabase = cacoDatabase
        transaction(cacoDatabase) {
            SchemaUtils.createMissingTablesAndColumns(ScryfallCardSets, Cards, CardVariants, CardPossessions, DeckArchetypes, Builds, DeckCards, ArenaCardPossessions)
        }
    }
}
