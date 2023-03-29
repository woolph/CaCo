package at.woolph.caco.view.collection

import at.woolph.caco.datamodel.sets.Card
import at.woolph.caco.imagecache.ImageCache
import io.ktor.http.content.*
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

open class CardModel(card: Card): ItemViewModel<Card>(card), Comparable<CardModel>{
	val id = bind(Card::id)
	val set = bind(Card::set)
	val numberInSet = bind(Card::numberInSet)
	val name = bind(Card::name)
	val nameDE = bind(Card::nameDE)
	val arenaId = bind(Card::arenaId)
	val rarity = bind(Card::rarity)
	val promo = bind(Card::promo)
	val token = bind(Card::token)
	val image = bind(Card::image)
	val cardmarketUri = bind(Card::cardmarketUri)

	val currencyNumberFormat = NumberFormat.getCurrencyInstance()
	val price = bind(Card::price)
	val priceFoil = bind(Card::priceFoil)

	val priceString = price.stringBinding { currencyNumberFormat.format(it) }

	val extra = bind(Card::extra)
	val nonfoilAvailable = bind(Card::nonfoilAvailable)
	val foilAvailable = bind(Card::foilAvailable)
	val fullArt = bind(Card::fullArt)
	val extendedArt = bind(Card::extendedArt)
	val specialDeckRestrictions: Property<Int?> = bind(Card::specialDeckRestrictions, defaultValue = null)

	val names: Sequence<String>
		get() = sequenceOf(name, nameDE).mapNotNull { it.value }

	override fun compareTo(other: CardModel): Int {
		// TODO dateOfRelease not reachable
//		if (set.value != other.set.value) {
//			return set.value.set.dateOfRelease.compareTo(other.set.value.set.dateOfRelease)
//		}
		return numberInSet.value.compareTo(other.numberInSet.value)
	}

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
				val imageContent =image.value.toURL().readBytes()

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

//				ImageIO.write()
//				BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
//					BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
//					Graphics2D graphics2D = resizedImage.createGraphics();
//					graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
//					graphics2D.dispose();
//					return resizedImage;
//				}
			} catch (t: Throwable) {
				LOG.warn("unable to load ${image.value}", t)
				null
			}
		}

	companion object {
		val LOG = LoggerFactory.getLogger(this::class.java.declaringClass)
	}
}
