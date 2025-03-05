package at.woolph.caco.gui.view.collection

import at.woolph.caco.datamodel.sets.Card
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

open class CardModel(card: Card): ItemViewModel<Card>(card), Comparable<CardModel>{
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
	val nonfoilAvailable = bind(Card::nonfoilAvailable)
	val foilAvailable = bind(Card::foilAvailable)
	val fullArt = bind(Card::fullArt)
	val extendedArt = bind(Card::extendedArt)
	val specialDeckRestrictions: Property<Int?> = bind(Card::specialDeckRestrictions, defaultValue = null)

	val names: Sequence<String>
		get() = sequenceOf(name, nameDE).mapNotNull { it.value }

	override fun compareTo(other: CardModel): Int {
		if (set.value != other.set.value) {
			val lengthCompare = set.value.setCode.length.compareTo(other.set.value.setCode.length)
			if (lengthCompare != 0)
				return lengthCompare
			return set.value.setCode.compareTo(other.set.value.setCode)
		}
		val (prefix, number, suffix) = splitCollectorNumber(collectorNumber.value)
		val (otherPrefix, otherNumber, otherSuffix) = splitCollectorNumber(other.collectorNumber.value)

		return prefix.compareToNullable(otherPrefix)
			?: number.compareToNullable(otherNumber)
			?: suffix.compareToNullable(otherSuffix)
			?: 0
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
		private val COLLECTION_NUMBER_PATTERN = Regex("^(?<prefix>\\w+-)?(?<number>\\d+)(?<suffix>.+)?$")

		fun splitCollectorNumber(collectorNumber: String): Triple<String?, Int, String?> {
			val match = COLLECTION_NUMBER_PATTERN.find(collectorNumber) ?: return Triple(null, 0, null)
			val prefix = match.groups["prefix"]?.value
			val number = match.groups["number"]!!.value.toInt()
			val suffix = match.groups["suffix"]?.value
			return Triple(prefix, number, suffix)
		}

		fun <A: Comparable<A>> A?.compareToNullable(other: A?, isNullLowerWeightThanValue: Boolean = true): Int? {
			if (this == null && other == null) return null
			if (this == null) return if (isNullLowerWeightThanValue) -1 else 1
			if (other == null) return if (isNullLowerWeightThanValue) 1 else -1
			val compareResult = this.compareTo(other)
			if (compareResult == 0) return null
			return compareResult
		}
	}
}
