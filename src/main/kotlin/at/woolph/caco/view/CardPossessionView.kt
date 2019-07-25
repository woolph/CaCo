package at.woolph.caco.view

import at.woolph.caco.Styles
import at.woolph.caco.datamodel.collection.CardCondition
import at.woolph.caco.datamodel.sets.Card
import at.woolph.libs.ktfx.mapBinding
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.image.ImageView
import javafx.scene.paint.Color
import javafx.scene.shape.Shape
import tornadofx.Fragment
import tornadofx.*
import kotlin.math.min

class CardPossessionView(val cardProperty: SimpleObjectProperty<Card?> = SimpleObjectProperty(null)) : Fragment() {
	var card by cardProperty

	private lateinit var labelNumberInSet: Label
	private lateinit var labelRarity: Label
	private lateinit var labelCardName: Label

	private lateinit var imageView: ImageView
	private lateinit var imageLoadingProgressIndicatorBackground: Shape
	private lateinit var imageLoadingProgressIndicator: ProgressIndicator

	private val languages = listOf("en", "de", "ja", "ru", "es", "fr", "it", "pt", "ko", "zhs", "zht")

	override val root =  gridpane {
		addClass(Styles.cardPossessionView)

		paddingAll = 10.0
		hgap = 10.0
		vgap = 10.0

		row {
			label("Condition")
			languages.forEach {
				label(it)
			}
		}
		CardCondition.values().forEach {
			row {
				label(it.toString())

				languages.forEach {
					label("0")
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