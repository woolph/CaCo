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

class CardPossessionView(val cardProperty: SimpleObjectProperty<CardPossessionModel?> = SimpleObjectProperty(null)) : Fragment() {
	//var card by cardProperty

	private val languages = CardLanguage.values()
	private val conditions = CardCondition.values()
	private val possessions = Array(languages.count()) { languageIndex ->
			Array(conditions.count()) { conditionIndex ->
				//SimpleIntegerProperty(0) // TODO
				cardProperty.integerBinding {
					it?.getPaperPossessions(languages[languageIndex], conditions[conditionIndex]) ?: 0
				}
			}
		}
	private val sums = Array(languages.count()) { integerBinding(this, *possessions[it]) { possessions[it].sumBy { it.value } } }

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
					textProperty().bind(sums[cl.ordinal].toStringBinding())
				}
			}
		}
		CardCondition.values().forEach { cc ->
			row {
				label(cc.toString())

				languages.forEach { cl ->
					label {
						textProperty().bind(possessions[cl.ordinal][cc.ordinal].toStringBinding())
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
