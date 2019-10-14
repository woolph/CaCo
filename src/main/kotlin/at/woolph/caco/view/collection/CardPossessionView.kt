package at.woolph.caco.view.collection

import at.woolph.caco.Styles
import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.libs.ktfx.toStringBinding
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.HPos
import javafx.geometry.Rectangle2D
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import tornadofx.Fragment
import tornadofx.*

fun <K,V> generateMap(keys: Collection<K>, valueGenerator: (K)->V) = keys.map { it to valueGenerator(it) }.toMap()
fun <K,V> generateMap(keys: Array<K>, valueGenerator: (K)->V) = keys.map { it to valueGenerator(it) }.toMap()

class IconCollection(val image: Image, val iconWidth: Double, val iconHeight: Double) {
	operator fun get(row: Int, column: Int) = ImageView(image).apply {
		viewport = Rectangle2D(iconWidth*column,iconHeight*row,iconWidth, iconHeight)
	}
}

class CardPossessionView(val cardProperty: SimpleObjectProperty<CardPossessionModel?> = SimpleObjectProperty(null)) : Fragment() {
	private val languages = CardLanguage.values().filter { it != CardLanguage.UNKNOWN }
	private val conditions = CardCondition.values().filter { it != CardCondition.UNKNOWN }
	private val possessions = generateMap(languages) { language ->
		generateMap(conditions) { condition ->
			cardProperty.stringBinding { it?.getPaperPossessionsString(language, condition) }
		}
	}
	private val sums = generateMap(languages) { language ->
		cardProperty.stringBinding { it?.getPaperPossessionsString(language) }
	}

	private val iconCollection = IconCollection(resources.image("mkm-icons.png"), 16.0, 16.0)

	private fun CardLanguage.toIcon() = when(this) {
		CardLanguage.ENGLISH -> 1
		CardLanguage.FRENCH -> 3
		CardLanguage.GERMAN -> 5
		CardLanguage.SPANISH -> 7
		CardLanguage.ITALIAN -> 9
		CardLanguage.CHINESE -> 11
		CardLanguage.JAPANESE -> 13
		CardLanguage.PORTUGUESE -> 15
		CardLanguage.RUSSIAN -> 17
		CardLanguage.KOREAN -> 19
		CardLanguage.CHINESE_TRADITIONAL -> 21
		else -> null
	}?.let { iconCollection[5, it] } ?: Label(this.toString())

	private fun CardCondition.toIcon() = when(this) {
		CardCondition.NEAR_MINT -> 2
		CardCondition.EXCELLENT -> 4
		CardCondition.GOOD -> 6
		CardCondition.PLAYED -> 8
		CardCondition.POOR -> 10
		else -> null
	}?.let { iconCollection[4, it] } ?: Label(this.toString())

	override val root = gridpane {
		minWidth = 300.0
		prefWidth = 300.0
		addClass(Styles.cardPossessionView)

		paddingAll = 10.0
		hgap = 10.0
		vgap = 10.0

		row {
			label("")
			languages.forEach { cl ->
				add(cl.toIcon())
			}
		}
		row {
			label("\u2211") {
				addClass(Styles.sumRow)
			}
			languages.forEach { cl ->
				label {
					textProperty().bind(sums[cl])
					addClass(Styles.sumRow)
				}
			}
		}
		conditions.forEach { cc ->
			row {
				add(cc.toIcon())

				languages.forEach { cl ->
					label {
						textProperty().bind(possessions[cl]?.get(cc))
					}
				}
			}
		}
		constraintsForColumn(0).apply {
			percentWidth = 150.0 / (languages.count() + 1.5)
			halignment = HPos.CENTER
		}
		for(columnIndex in 1..languages.count()) {
			constraintsForColumn(columnIndex).apply {
				percentWidth = 100.0 / (languages.count() + 1.5)
				halignment = HPos.CENTER
			}
		}
	}
}
