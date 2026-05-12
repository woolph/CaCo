/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.datamodel

import at.woolph.caco.HomeDirectory
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.decks.Builds
import at.woolph.caco.datamodel.decks.DeckArchetypes
import at.woolph.caco.datamodel.decks.DeckCards
import at.woolph.caco.datamodel.sets.CardPrintVariants
import at.woolph.caco.datamodel.sets.CardPrints
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.datamodel.sets.ScryfallCardSets
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

actual fun initDatabase(homeDirectory: HomeDirectory) {
  val cacoDatabase =
      Database.connect("jdbc:h2:${homeDirectory}/caco-database", driver = "org.h2.Driver")
  TransactionManager.defaultDatabase = cacoDatabase
  transaction {
    SchemaUtils.createMissingTablesAndColumns(
        ScryfallCardSets,
        Cards,
        CardPrints,
        CardPrintVariants,
        CardPossessions,
        DeckArchetypes,
        Builds,
        DeckCards,
    )
  }
}
