package at.woolph.caco.view

import at.woolph.caco.datamodel.sets.Card
import at.woolph.libs.ktfx.ImageCache
import at.woolph.libs.ktfx.mapBinding
import at.woolph.libs.ktfx.mapStringBinding
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.effect.Bloom
import javafx.scene.effect.Effect
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.paint.Color
import javafx.scene.shape.Shape
import tornadofx.Fragment
import tornadofx.*
import javafx.scene.effect.Glow
import kotlin.math.min


class CardDetailsView(val cardProperty: SimpleObjectProperty<Card?> = SimpleObjectProperty(null)) : Fragment() {
	var card by cardProperty

	val imageLoadingProperty = SimpleBooleanProperty(true)
	var imageLoading by imageLoadingProperty

	private lateinit var labelNumberInSet: Label
	private lateinit var labelRarity: Label
	private lateinit var labelCardName: Label

	private lateinit var imageView: ImageView
	private lateinit var imageLoadingProgressIndicatorBackground: Shape
	private lateinit var imageLoadingProgressIndicator: ProgressIndicator

	override val root =  gridpane {
		paddingAll = 10.0
		hgap = 10.0
		vgap = 10.0

		row {
			labelNumberInSet = label()
			labelRarity = label()
			labelCardName = label()
		}

		row {
			stackpane {
				gridpaneConstraints {
					columnSpan = 3
				}

				imageView = imageview {
					fitHeight = 312.0
					fitWidth = 224.0

					/*
					val glow = Bloom(0.8)
					effectProperty().bind(
							Bindings.`when`(hoverProperty()).then<Effect?>(glow).otherwise(null as Effect?)
					)*/
				}

				imageLoadingProgressIndicatorBackground = rectangle {
					fill = Color.rgb(1, 1, 1, 0.3)
					height = imageView.fitHeight
					width = imageView.fitWidth
				}

				imageLoadingProgressIndicator = progressindicator {
					val maxSize = min(imageView.fitHeight, imageView.fitWidth) / 2
					isVisible = false
					maxWidth = maxSize
					maxHeight = maxSize
				}
			}
		}
	}

	init {
		labelNumberInSet.textProperty().bind(cardProperty.mapBinding { it?.numberInSet ?: "" })
		labelRarity.textProperty().bind(cardProperty.mapBinding { it?.rarity?.toString() ?: "" })
		labelCardName.textProperty().bind(cardProperty.mapBinding { it?.name ?: "" })

		cardProperty.addListener { _, _, _ -> loadImage() }
		imageLoadingProperty.addListener { _, _, _ -> loadImage() }
	}

	fun loadImage() {
		if(imageLoading) {
			tornadofx.runAsync {
				imageLoadingProgressIndicatorBackground.isVisible = true
				imageLoadingProgressIndicator.isVisible = true
				card?.getCachedImage()
			} ui {
				imageView.image = it
				imageLoadingProgressIndicator.isVisible = false
				imageLoadingProgressIndicatorBackground.isVisible = false
			}

			// TODO preload surrounding images?
			/*tornadofx.runAsync {
				listOf(tvCards.selectionModel.selectedIndex + 1,
						tvCards.selectionModel.selectedIndex - 1,
						tvCards.selectionModel.selectedIndex + 2,
						tvCards.selectionModel.selectedIndex + 3).forEach {
					if (0 <= it && it < tvCards.items.size) {
						tvCards.items[it].cardImage
					}
				}
			}*/
		} else {
			imageView.image = null
			imageLoadingProgressIndicatorBackground.isVisible = true
		}
	}
}

object CardImageCache: ImageCache()

fun Card?.getCachedImage(): Image? {
	return this?.image?.let { CardImageCache.getImage(it,224.0, 312.0, true, true) }
}
