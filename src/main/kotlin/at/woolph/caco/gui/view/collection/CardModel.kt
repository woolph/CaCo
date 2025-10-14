/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.gui.view.collection

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.datamodel.sets.Finish
import at.woolph.caco.masterdata.imagecache.ImageCache
import javafx.beans.property.Property
import javafx.scene.image.Image
import org.slf4j.LoggerFactory
import tornadofx.ItemViewModel
import tornadofx.stringBinding
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import javax.imageio.ImageIO

open class CardModel(
    card: Card,
) : ItemViewModel<Card>(card),
    Comparable<CardModel> {
    val id = bind(Card::id)
    val set = bind(Card::set)
    val collectorNumber = bind(Card::collectorNumber)
    val name = bind(Card::name)
    val nameDE = bind(Card::nameDE)
    val arenaId = bind(Card::arenaId)
    val rarity = bind(Card::rarity)
    val promo = bind(Card::promo)
    val token = bind(Card::token)
    val image = bind(Card::thumbnail)
    val cardmarketUri = bind(Card::cardmarketUri)

    val currencyNumberFormat = NumberFormat.getCurrencyInstance()
    val price = bind(Card::price)
    val priceFoil = bind(Card::priceFoil)

    val priceString = price.stringBinding { currencyNumberFormat.format(it) }

    val extra = bind(Card::extra)
    val nonfoilAvailable = itemProperty.map { it.finishes.contains(Finish.Normal) }
    val foilAvailable = itemProperty.map { it.finishes.contains(Finish.Foil) }
    val fullArt = bind(Card::fullArt)
    val extendedArt = bind(Card::extendedArt)
    val specialDeckRestrictions: Property<Int?> = bind(Card::specialDeckRestrictions, defaultValue = null)

    val names: Sequence<String>
        get() = sequenceOf(name, nameDE).mapNotNull { it.value }

    suspend fun getCachedImage(): Image? =
        ImageCache.getImage(image.value.toString()) {
            try {
                image.value.toURL().readBytes()
            } catch (t: Throwable) {
                LOG.warn("unable to load ${image.value}", t)
                null
            }
        }

    suspend fun cacheImage() =
        ImageCache.cacheImage(image.value.toString()) {
            try {
                LOG.info("precache image ${image.value}")
                val imageContent = image.value.toURL().readBytes()

                val scaleDown = true
                if (scaleDown) {
                    val baos = ByteArrayOutputStream()
                    baos.use {
                        val bufferedImage = ImageIO.read(ByteArrayInputStream(imageContent))
                        val resizedImage = BufferedImage(224, 312, BufferedImage.TYPE_INT_RGB)
                        val graphics2D = resizedImage.createGraphics()
                        graphics2D.drawImage(bufferedImage, 0, 0, 224, 312, null)
                        graphics2D.dispose()
                        ImageIO.write(resizedImage, "png", baos)
                        baos.flush()
                    }
                    return@cacheImage baos.toByteArray()
                } else {
                    imageContent
                }
            } catch (t: Throwable) {
                LOG.warn("unable to load ${image.value}", t)
                null
            }
        }

    override fun compareTo(other: CardModel): Int = item.compareTo(other.item)

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java.declaringClass)
    }
}
