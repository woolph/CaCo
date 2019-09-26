package at.woolph.caco.view

import at.woolph.caco.Styles
import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.collection.CardLanguage
import at.woolph.caco.view.collection.CardPossessionModel
import at.woolph.libs.ktfx.mapBinding
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.Label
import tornadofx.Fragment
import tornadofx.*

class CardPossessionView(val cardProperty: SimpleObjectProperty<CardPossessionModel?> = SimpleObjectProperty(null)) : Fragment() {
	var card by cardProperty

	private val conditions = CardCondition.values()
	private val languages = CardLanguage.values()
	private val labelPossessions = Array(conditions.count()) { Array(languages.count()) { Label("0$it") } }

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
		CardCondition.values().forEach { cc ->
			row {
				label(cc.toString())

				languages.forEach { cl ->
					//label("0")
					add(labelPossessions[cc.ordinal][cl.ordinal])
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
