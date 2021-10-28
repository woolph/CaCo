package at.woolph.caco.view

import at.woolph.caco.view.collection.CardModel
import javafx.beans.value.ObservableBooleanValue
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Tooltip
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import tornadofx.*
import kotlin.math.min

class CardImageTooltip(val card: CardModel, val imageLoadingProperty: ObservableBooleanValue): Tooltip() {
	private lateinit var imageView: ImageView
	private lateinit var imageLoadingProgressIndicator: ProgressIndicator

	init {
		isAutoHide = false

		setOnShowing {
			if(imageLoadingProperty.value) {
				graphic = StackPane().apply {
					imageView = imageview {
						fitHeight = 312.0
						fitWidth = 224.0
					}

					imageLoadingProgressIndicator = progressindicator {
						val maxSize = min(imageView.fitHeight, imageView.fitWidth) / 2
						isVisible = false
						maxWidth = maxSize
						maxHeight = maxSize
					}
				}

				runAsync {
					imageLoadingProgressIndicator.isVisible = true
					card.getCachedImage()
				} ui  {
					imageView.image = it
					imageLoadingProgressIndicator.isVisible = false
				}
			}
			else {
				graphic = null
				text = card.name.value
			}
		}
	}
}