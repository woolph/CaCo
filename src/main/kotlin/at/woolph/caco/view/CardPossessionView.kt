package at.woolph.caco.view

import at.woolph.caco.Styles
import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.view.collection.CardPossessionModel
import at.woolph.libs.ktfx.mapBinding
import at.woolph.libs.ktfx.toStringBinding
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableIntegerArray
import javafx.scene.control.Label
import org.jetbrains.exposed.sql.transactions.transaction
import tornadofx.Fragment
import tornadofx.*

fun <K,V> generateMap(keys: Collection<K>, valueGenerator: (K)->V) = keys.map { it to valueGenerator(it) }.toMap()
fun <K,V> generateMap(keys: Array<K>, valueGenerator: (K)->V) = keys.map { it to valueGenerator(it) }.toMap()

class CardPossessionView(val cardProperty: SimpleObjectProperty<CardPossessionModel?> = SimpleObjectProperty(null)) : Fragment() {
	//var card by cardProperty

	private val languages = CardLanguage.values().filter { it != CardLanguage.UNKNOWN }
	private val conditions = CardCondition.values().filter { it != CardCondition.UNKNOWN }
	private val possessions = generateMap(languages) { language -> generateMap(conditions) { condition ->
				cardProperty.integerBinding { it?.getPaperPossessions(language, condition) ?: 0 }
			}
		}
	private val sums = generateMap(languages) { integerBinding(this, *possessions[it]?.values?.toTypedArray() ?: emptyArray()) { possessions[it]?.values?.sumBy { it.value } ?: 0 } }

	override val root = gridpane {
		addClass(Styles.cardPossessionView)

		paddingAll = 10.0
		hgap = 10.0
		vgap = 10.0

		row {
			label("")
			languages.forEach { cl ->
				label(cl.toString())
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
				label(cc.toString())

				languages.forEach { cl ->
					label {
						textProperty().bind(possessions[cl]?.get(cc)?.toStringBinding())
					}
				}
			}
		}
	}

	init {
		//labelNumberInSet.textProperty().bind(cardProperty.mapBinding { it?.numberInSet ?: "" })
		//labelRarity.textProperty().bind(cardProperty.mapBinding { it?.rarity?.toString() ?: "" })
		//labelCardName.textProperty().bind(cardProperty.mapBinding { it?.name ?: "" })
	}
}
