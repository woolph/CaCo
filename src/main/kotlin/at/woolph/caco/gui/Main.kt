package at.woolph.caco.gui

import at.woolph.caco.datamodel.Databases
import at.woolph.caco.view.collection.PaperCollectionView
import at.woolph.caco.view.decks.DecksView
import at.woolph.libs.ktfx.view
import at.woolph.libs.log.logger
import javafx.geometry.Side
import javafx.scene.control.TabPane
import tornadofx.App
import tornadofx.View
import tornadofx.launch
import tornadofx.tab

private val log by logger("at.woolph.caco.MainGui")

fun main(args: Array<String>) {
	Databases.init()
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

//			tab("Collection Arena") {
//				isClosable = false
//				view(ArenaCollectionView::class)
//			}

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
