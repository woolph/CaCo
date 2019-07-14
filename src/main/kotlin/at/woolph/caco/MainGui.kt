package at.woolph.caco

import at.woolph.caco.datamodel.collection.ArenaCardPossessions
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.decks.Builds
import at.woolph.caco.datamodel.decks.DeckCards
import at.woolph.caco.datamodel.decks.Decks
import at.woolph.caco.datamodel.decks.Variants
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.datamodel.sets.Sets
import at.woolph.caco.view.ArenaCollectionView
import at.woolph.caco.view.PaperCollectionView
import at.woolph.libs.ktfx.view
import javafx.event.EventTarget
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.format.DateTimeFormat
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.control.*
import tornadofx.*
import kotlin.reflect.KClass

/**
 * @see https://github.com/JetBrains/Exposed
 */
fun main(args: Array<String>) {
	println("starting main")
	val dtf = DateTimeFormat.forPattern("YYYY-MM-dd")
	Database.connect("jdbc:h2:~/caco", driver = "org.h2.Driver")

	transaction {
		SchemaUtils.createMissingTablesAndColumns(Sets, Cards, CardPossessions, ArenaCardPossessions, Decks, Variants, Builds, DeckCards)
		//CardPossessions.deleteAll()
	}


	launch<MyApp>(*args)
}

class MyApp : App(MyView::class)

class MyView : View() {
	override val root = TabPane()

	init {
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
			}
		}
	}
}
