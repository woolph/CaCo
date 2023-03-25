package at.woolph.caco.view

import at.woolph.caco.gui.Styles
import at.woolph.caco.view.collection.CardSetModel
import at.woolph.libs.ktfx.*
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.paint.Color
import javafx.scene.shape.Shape
import tornadofx.Fragment
import tornadofx.*
import kotlin.math.min

class SetDetailsView(val setProperty: SimpleObjectProperty<CardSetModel?> = SimpleObjectProperty(null)) : Fragment() {
	var set by setProperty

	private lateinit var labelNumberInSet: Label
	private lateinit var labelRarity: Label
	private lateinit var labelSetName: Label

	private lateinit var imageView: ImageView
	private lateinit var imageLoadingProgressIndicatorBackground: Shape
	private lateinit var imageLoadingProgressIndicator: ProgressIndicator

	override val root =  gridpane {
		addClass(Styles.setDetailsView)

		paddingAll = 10.0
		hgap = 10.0
		vgap = 10.0

		row {
			labelNumberInSet = label()
			labelRarity = label()
			labelSetName = label()
		}

		row {
			stackpane {
				gridpaneConstraints {
					columnSpan = 3
				}

				imageView = imageview {
					fitHeight = 128.0
					fitWidth = 128.0
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
		labelNumberInSet.textProperty().bind(setProperty.selectNullable { it?.officalCardCount }.toStringBinding())
		labelSetName.textProperty().bind(setProperty.selectNullable { it?.name })

		setProperty.addListener { _, _, _ -> loadImage() }
	}

	fun loadImage() {
		tornadofx.runAsync {
			imageLoadingProgressIndicatorBackground.isVisible = true
			imageLoadingProgressIndicator.isVisible = true
			set?.getCachedImage()
		} ui {
			imageView.image = it
			imageLoadingProgressIndicator.isVisible = false
			imageLoadingProgressIndicatorBackground.isVisible = false
		}
	}
}

object SetImageCache: ImageCache()

fun CardSetModel?.getCachedImage(): Image? {
	return this?.icon?.value?.let { SetImageCache.getImage(it,128.0, 128.0, true, true) }
}
