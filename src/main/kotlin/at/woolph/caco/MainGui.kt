package at.woolph.caco

import at.woolph.caco.datamodel.collection.ArenaCardPossessions
import at.woolph.caco.datamodel.collection.CardPossessions
import at.woolph.caco.datamodel.decks.*
import at.woolph.caco.datamodel.sets.Cards
import at.woolph.caco.datamodel.sets.CardSets
import at.woolph.caco.view.collection.ArenaCollectionView
import at.woolph.caco.view.collection.PaperCollectionView
import at.woolph.caco.view.decks.DecksView
import at.woolph.libs.ktfx.view
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.format.DateTimeFormat
import javafx.geometry.Side
import javafx.scene.control.*
import tornadofx.*

/**
 * @see https://github.com/JetBrains/Exposed
 */
fun main(args: Array<String>) {
	println("starting main")
	val dtf = DateTimeFormat.forPattern("YYYY-MM-dd")
	Database.connect("jdbc:h2:~/caco", driver = "org.h2.Driver")

	transaction {
		SchemaUtils.createMissingTablesAndColumns(CardSets, Cards, CardPossessions, ArenaCardPossessions, Decks, Variants, Builds, DeckCards)
		//CardPossessions.deleteAll()

		Deck.new {
			name = "Sultai Midrange"
			format = Format.Standard
		}
	}


	launch<MyApp>(*args)
}

class MyApp : App(MyView::class, Styles::class)

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
				view(DecksView::class)
			}
		}
	}
}
