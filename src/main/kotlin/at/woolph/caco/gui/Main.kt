package at.woolph.caco.gui

import at.woolph.caco.datamodel.collection.ArenaCardPossessions
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.decks.*
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.datamodel.sets.CardSets
import at.woolph.caco.datamodel.sets.ScryfallCardSets
import at.woolph.caco.view.collection.ArenaCollectionView
import at.woolph.caco.view.collection.PaperCollectionView
import at.woolph.caco.view.decks.DecksView
import at.woolph.libs.ktfx.view
import at.woolph.libs.log.logger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import javafx.geometry.Side
import javafx.scene.control.*
import tornadofx.*
import kotlin.system.measureTimeMillis

private val log by logger("at.woolph.caco.MainGui")

fun main(args: Array<String>) {
	log.trace("connecting DB")
	log.trace("connecting DB took {} ms", measureTimeMillis {
		Database.connect("jdbc:h2:./caco", driver = "org.h2.Driver")
	})

	log.trace("createMissingTablesAndColumns took {} ms", measureTimeMillis {
		transaction {
			SchemaUtils.createMissingTablesAndColumns(CardSets, ScryfallCardSets, Cards, CardPossessions, DeckArchetypes, Builds, DeckCards, ArenaCardPossessions)
		}
	})

	log.trace("launching UI")
	launch<MyApp>(*args)
}

class MyApp : App(MyView::class, Styles::class)

class MyView : View() {
	override val root = TabPane()

	init {
		logger.trace("init MyView")
		title = "CaCo"

		with(root) {
			side = Side.LEFT
			tab("Collection Paper") {
				isClosable = false
				view(PaperCollectionView::class)
			}

			tab("Collection Arena") {
				isClosable = false
				view(ArenaCollectionView::class)
			}

			tab("Decks") {
				isClosable = false
				view(DecksView::class)
			}
		}
	}

	companion object {
		val logger by logger()
	}
}
