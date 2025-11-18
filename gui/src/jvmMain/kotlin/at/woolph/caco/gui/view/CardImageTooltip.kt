/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.gui.view

import at.woolph.caco.gui.view.collection.CardModel
import javafx.beans.value.ObservableBooleanValue
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Tooltip
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import tornadofx.imageview
import tornadofx.progressindicator
import kotlin.math.min

class CardImageTooltip(
    val card: CardModel,
    val imageLoadingProperty: ObservableBooleanValue,
) : Tooltip() {
    private lateinit var imageView: ImageView
    private lateinit var imageLoadingProgressIndicator: ProgressIndicator

    val coroutineScope = CoroutineScope(SupervisorJob() + CoroutineName("CardImageTooltip"))

    init {
        isAutoHide = false

        setOnHiding {
            coroutineScope.coroutineContext.cancelChildren()
        }

        setOnShowing {
            if (imageLoadingProperty.value) {
                graphic =
                    StackPane().apply {
                        imageView =
                            imageview {
                                fitHeight = 312.0
                                fitWidth = 224.0
                            }

                        imageLoadingProgressIndicator =
                            progressindicator {
                                val maxSize = min(imageView.fitHeight, imageView.fitWidth) / 2
                                isVisible = false
                                maxWidth = maxSize
                                maxHeight = maxSize
                            }
                    }

                coroutineScope.launch(Dispatchers.Main.immediate) {
                    imageLoadingProgressIndicator.isVisible = true
                    imageView.image = card.getCachedImage()
                    imageLoadingProgressIndicator.isVisible = false
                }
            } else {
                graphic = null
                text = card.name.value
            }
        }
    }
}
