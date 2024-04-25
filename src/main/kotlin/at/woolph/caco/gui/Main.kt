package at.woolph.caco.gui

import at.woolph.caco.datamodel.Databases
import at.woolph.caco.importer.sets.ProgressIndicator
import at.woolph.caco.view.collection.PaperCollectionView
import at.woolph.caco.view.decks.DecksView
import at.woolph.libs.ktfx.view
import at.woolph.libs.log.logger
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Side
import javafx.scene.Parent
import tornadofx.App
import tornadofx.View
import tornadofx.borderpane
import tornadofx.doubleBinding
import tornadofx.launch
import tornadofx.progressbar
import tornadofx.tab
import tornadofx.tabpane


fun main(args: Array<String>) {
	Databases.init()
	launch<MyApp>(*args)
}

class MyApp : App(MyView::class, Styles::class)

class MyView : View() {
	override val root: Parent

	init {
		logger.trace("init MyView")
		title = "CaCo"

		root = borderpane {
			center = tabpane {
				side = Side.LEFT
				tab("Collection Paper") {
					isClosable = false
					view<PaperCollectionView>()
				}

//			tab("Collection Arena") {
//				isClosable = false
//				view(ArenaCollectionView::class)
//			}

				tab("Decks") {
					isClosable = false
					view<DecksView>()
				}
			}
			bottom = progressbar {
				doubleBinding(progressIndicators) {
					progressIndicators.firstOrNull()?.progress?.value?.toDouble() ?: 0.0
				}
			}
		}
	}

	companion object {
		val logger by logger()
		val progressIndicators: ObservableList<ProgressIndicator> = FXCollections.observableList(mutableListOf())
	}
}
