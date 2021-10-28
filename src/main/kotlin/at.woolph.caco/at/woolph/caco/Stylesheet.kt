package at.woolph.caco

import tornadofx.*
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight

class Styles : Stylesheet() {
	companion object {
		// Define our styles

		// colors // TODO determine color
		val mythic = c("copper")
		val rare = c("gold")
		val uncommon = c("silver")
		val common = c("black")

		// css classes
		val cardDetailsView by cssclass()
		val cardPossessionView by cssclass()
		val sumRow by cssclass()
	}

	init {
		cardDetailsView {
			label {
				fontSize = 16.px
				fontWeight = FontWeight.BOLD
			}
		}

		cardPossessionView {
			label {
				fontSize = 14.px
			}
		}

		cardPossessionView {
			sumRow {
				fontWeight = FontWeight.BOLD
			}
		}
	}
}
