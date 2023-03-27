package at.woolph.caco.view

import at.woolph.caco.datamodel.sets.renderSvg
import at.woolph.caco.gui.Styles
import at.woolph.caco.imagecache.ImageCache
import at.woolph.caco.view.collection.CardSetModel
import at.woolph.libs.ktfx.*
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.paint.Color
import javafx.scene.shape.Shape
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.javafx.asFlow
import kotlinx.coroutines.launch
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


	val coroutineScope = CoroutineScope(SupervisorJob() + CoroutineName("SetDetailsView"))

	init {
		labelNumberInSet.textProperty().bind(setProperty.selectNullable { it?.officalCardCount }.toStringBinding())
		labelSetName.textProperty().bind(setProperty.selectNullable { it?.name })

		coroutineScope.launch(Dispatchers.Main.immediate) {
			setProperty.asFlow()
				.collectLatest { set ->
					imageLoadingProgressIndicatorBackground.isVisible = true
					imageLoadingProgressIndicator.isVisible = true
					imageView.image = set?.getCachedImage()
					imageLoadingProgressIndicator.isVisible = false
					imageLoadingProgressIndicatorBackground.isVisible = false
				}
		}
	}
}

suspend fun CardSetModel.getCachedImage(): Image? =
	ImageCache.getImage(this.icon.value.toString()) { this.icon.value.renderSvg(48.0f) }
