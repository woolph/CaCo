package at.woolph.caco.gui.view

import at.woolph.caco.gui.Styles
import at.woolph.caco.gui.view.collection.CardModel
import at.woolph.libs.ktfx.selectNullable
import at.woolph.libs.ktfx.toStringBinding
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.image.ImageView
import javafx.scene.paint.Color
import javafx.scene.shape.Shape
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.javafx.asFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import tornadofx.Fragment
import tornadofx.addClass
import tornadofx.getValue
import tornadofx.gridpane
import tornadofx.gridpaneConstraints
import tornadofx.imageview
import tornadofx.label
import tornadofx.paddingAll
import tornadofx.progressindicator
import tornadofx.rectangle
import tornadofx.row
import tornadofx.setValue
import tornadofx.stackpane
import tornadofx.whenDocked
import tornadofx.whenUndocked
import kotlin.math.min

@OptIn(ExperimentalCoroutinesApi::class)
class CardDetailsView(val cardProperty: SimpleObjectProperty<CardModel?> = SimpleObjectProperty(null)) : Fragment() {
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
		addClass(Styles.cardDetailsView)

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

	val coroutineScope = CoroutineScope(SupervisorJob() + CoroutineName("CardDetailsView"))

	init {
		labelNumberInSet.textProperty().bind(cardProperty.selectNullable { it?.numberInSet }.toStringBinding())
		labelRarity.textProperty().bind(cardProperty.selectNullable { it?.rarity }.toStringBinding())
		labelCardName.textProperty().bind(cardProperty.selectNullable { it?.name })

//		cardProperty.addListener { _, _, _ -> loadImage() }
//		imageLoadingProperty.addListener { _, _, _ -> loadImage() }

		whenDocked {
			coroutineScope.launch(Dispatchers.Default) {
				combine(
					cardProperty.asFlow().onEach { LOG.trace("selected card changed to {}", it) },
					imageLoadingProperty.asFlow(),
				) { cardModel, imageLoading -> Pair(cardModel, imageLoading) }
					.collectLatest { (cardModel, imageLoading) ->
						withContext(Dispatchers.Main.immediate) {
							if (imageLoading) {
								LOG.trace("loading image for {}", cardModel)
								imageLoadingProgressIndicatorBackground.isVisible = true
								imageLoadingProgressIndicator.isVisible = true
								imageView.image = cardModel?.getCachedImage()
								imageLoadingProgressIndicator.isVisible = false
								imageLoadingProgressIndicatorBackground.isVisible = false
								LOG.trace("loaded image for {}", cardModel)
							} else {
								imageView.image = null
								imageLoadingProgressIndicatorBackground.isVisible = true
							}
						}
					}
			}
		}

		whenUndocked {
			coroutineScope.coroutineContext.cancelChildren()
		}
	}

	companion object {
	    val LOG = LoggerFactory.getLogger(this::class.java.declaringClass)
	}
}

