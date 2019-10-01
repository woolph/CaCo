package at.woolph.caco.view.collection

import at.woolph.caco.Styles
import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.libs.ktfx.toStringBinding
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.HPos
import javafx.geometry.Pos
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
	//var card by cardProperty

	private val languages = CardLanguage.values().filter { it != CardLanguage.UNKNOWN }
	private val conditions = CardCondition.values().filter { it != CardCondition.UNKNOWN }
	private val possessions = generateMap(languages) { language ->
		generateMap(conditions) { condition ->
			cardProperty.integerBinding { it?.getPaperPossessions(language, condition) ?: 0 }
		}
	}
	private val sums = generateMap(languages) {
		integerBinding(this, *possessions[it]?.values?.toTypedArray()
				?: emptyArray()) { possessions[it]?.values?.sumBy { it.value } ?: 0 }
	}

	private val iconCollection = IconCollection(resources.image("mkm-icons.png"), 16.0, 16.0)

	private fun CardLanguage.getIcon() = when(this) {
		CardLanguage.ENGLISH -> iconCollection[5,1]
		CardLanguage.FRENCH -> iconCollection[5,3]
		CardLanguage.GERMAN -> iconCollection[5,5]
		CardLanguage.SPANISH -> iconCollection[5,7]
		CardLanguage.ITALIAN -> iconCollection[5,9]
		CardLanguage.CHINESE -> iconCollection[5,11]
		CardLanguage.JAPANESE -> iconCollection[5,13]
		CardLanguage.PORTUGUESE -> iconCollection[5,15]
		CardLanguage.RUSSIAN -> iconCollection[5,17]
		CardLanguage.KOREAN -> iconCollection[5,19]
		CardLanguage.CHINESE_TRADITIONL -> iconCollection[5,21]
		else -> Label(this.toString())
	}

	private fun CardCondition.getIcon() = when(this) {
		CardCondition.NEAR_MINT -> iconCollection[4,2]
		CardCondition.EXCELLENT -> iconCollection[4,4]
		CardCondition.GOOD -> iconCollection[4,6]
		CardCondition.PLAYED -> iconCollection[4,8]
		CardCondition.POOR -> iconCollection[4,10]
		else -> Label(this.toString())
	}

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
				add(cl.getIcon())
			}
		}
		row {
			label("\u2211")
			languages.forEach { cl ->
				label {
					textProperty().bind(sums[cl]?.toStringBinding())
				}
			}
		}
		conditions.forEach { cc ->
			row {
				add(cc.getIcon())

				languages.forEach { cl ->
					label {
						textProperty().bind(possessions[cl]?.get(cc)?.toStringBinding())
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

	init {
		//labelNumberInSet.textProperty().bind(cardProperty.mapBinding { it?.numberInSet ?: "" })
		//labelRarity.textProperty().bind(cardProperty.mapBinding { it?.rarity?.toString() ?: "" })
		//labelCardName.textProperty().bind(cardProperty.mapBinding { it?.name ?: "" })
	}
}
