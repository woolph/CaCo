package at.woolph.caco.binderlabels

import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.renderSvg
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

val darkLabels = false
val iconResolution = 256f

fun URI?.renderSvg(): ByteArray? = try { this.renderSvg(iconResolution) } catch (e: Exception) { null }
fun URI?.renderSvgAsMythic(): ByteArray? = try { this.renderSvg(iconResolution, if (darkLabels) "gray" else "black","#c54326", "#f7971c") } catch (e: Exception) { null }
fun URI?.renderSvgAsRare(): ByteArray? = try { this.renderSvg(iconResolution, if (darkLabels) "gray" else "black", "#8d7431", "#f6db94") } catch (e: Exception) { null }
fun URI?.renderSvgAsUncommon(): ByteArray? = try { this.renderSvg(iconResolution, if (darkLabels) "gray" else "black", "#626e77", "#c8e2f2") } catch (e: Exception) { null }
fun URI?.renderSvgAsCommon(): ByteArray? = try { this.renderSvg(iconResolution, if (darkLabels) "gray" else "black", "black", "black") } catch (e: Exception) { null }

interface MapLabelItem {
    val code: String
    val title: String
    val mainIcon: ByteArray?
    val subCode: String?
        get() = null
    val subTitle: String?
        get() = null
    val subIconLeft: ByteArray?
        get() = null
    val subIconRight: ByteArray?
        get() = null
    val subIconLeft2: ByteArray?
        get() = null
    val subIconRight2: ByteArray?
        get() = null
}

object PromosLabel: MapLabelItem {
    override val code: String = "PRM"
    override val title: String = "Promos & Specials"
    override val mainIcon: ByteArray? by lazy { URI("https://c2.scryfall.com/file/scryfall-symbols/sets/star.svg?1624852800").renderSvgAsMythic() }
}

object BlankLabel: MapLabelItem {
    override val code: String = ""
    override val title: String = ""
    override val mainIcon: ByteArray? = null
}

open class GenericLabel(override val code: String, override val title: String, override val subTitle: String? = null): MapLabelItem {
    override val mainIcon: ByteArray? by lazy { URI("https://c2.scryfall.com/file/scryfall-symbols/sets/default.svg?1647835200").renderSvgAsMythic() }
}

fun fetchCardSets(vararg codes: String): List<CardSet> = transaction {
    codes.map { CardSet.findById(it) ?: throw IllegalArgumentException("no set with code $it found") }
}
val CardSet?.lazyIconMythic: Lazy<ByteArray?> get() = lazy { this?.icon?.renderSvgAsMythic() }
val CardSet?.lazyIconUncommon: Lazy<ByteArray?> get() = lazy { this?.icon?.renderSvgAsUncommon() }

abstract class AbstractLabelItem(
    val sets: List<CardSet>
): MapLabelItem {
    override val title: String get() = sets[0].name
    override val subTitle: String?
        get() = (if (title == sets[0].name) sets.drop(1) else sets).let { subTitleSets ->
            when (subTitleSets.size) {
                0 -> null
                1 -> "incl. ${subTitleSets[0].name}"
                else -> "incl. ${subTitleSets.dropLast(1).joinToString(", ") { it.name }}, & ${subTitleSets.last().name}"
            }
        }

    override val code: String get() = sets[0].shortName.value
    override val subCode: String get() = sets.drop(1).joinToString("/") { it.shortName.value }

    override val mainIcon: ByteArray? by sets.getOrNull(0).lazyIconMythic
    override val subIconLeft: ByteArray? by sets.getOrNull(if (sets.size > 2) 1 else 2).lazyIconUncommon
    override val subIconRight: ByteArray? by sets.getOrNull(if (sets.size > 2) 2 else 1).lazyIconUncommon
    override val subIconLeft2: ByteArray? by sets.getOrNull(3).lazyIconUncommon
    override val subIconRight2: ByteArray? by sets.getOrNull(4).lazyIconUncommon
}

open class SimpleSet(override val code: String): AbstractLabelItem(fetchCardSets(code))

open class SeparatePreconPartSet(code: String): SimpleSet(code) {
    override val subTitle: String? get() = "Preconstructed Commander Decks"
    override val subCode: String
        get() = "Precons"
}

class SetWithCommander(override val code: String, override val subCode: String): AbstractLabelItem(fetchCardSets(code, subCode))

class SetWithCommanderAndAncillary(override val code: String, commanderCode: String, ancillaryCode: String): AbstractLabelItem(fetchCardSets(code, commanderCode, ancillaryCode))

class TwoSetBlock(blockTitle: String, override val code: String, override val subCode: String): AbstractLabelItem(fetchCardSets(code, subCode)) {
    override val title: String = blockTitle
}

class ThreeSetBlock(blockTitle: String, override val code: String, code1: String, code2: String): AbstractLabelItem(fetchCardSets(code, code1, code2)) {
    override val title: String = blockTitle
}

class FiveSetBlock(blockTitle: String, code0: String, code1: String, code2: String, code3: String, code4: String): AbstractLabelItem(fetchCardSets(code0, code1, code2, code3, code4)) {
    override val title: String = blockTitle
}
