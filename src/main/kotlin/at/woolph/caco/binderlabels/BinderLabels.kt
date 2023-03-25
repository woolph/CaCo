package at.woolph.caco.binderlabels

import at.woolph.caco.datamodel.sets.CardSet
import at.woolph.caco.datamodel.sets.renderSvg
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

val darkLabels = false
val iconResolution = 256f

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
    override val mainIcon: ByteArray? = URI("https://c2.scryfall.com/file/scryfall-symbols/sets/star.svg?1624852800").renderSvgAsMythic()
}

object BlankLabel: MapLabelItem {
    override val code: String = ""
    override val title: String = ""
    override val mainIcon: ByteArray? = null
}

open class GenericLabel(override val code: String, override val title: String, override val subTitle: String? = null): MapLabelItem {
    override val mainIcon: ByteArray? = URI("https://c2.scryfall.com/file/scryfall-symbols/sets/default.svg?1647835200").renderSvgAsMythic()
}

open class SimpleSet(override val code: String): MapLabelItem {
    override val title: String
    override val mainIcon: ByteArray?

    init {
        val set = transaction {
            CardSet.findById(code) ?: throw IllegalArgumentException("no set with code $code found")
        }

        title = set.name
        mainIcon = set.icon.renderSvgAsMythic()
    }
}

class SetWithCommander(override val code: String, override val subCode: String): MapLabelItem {
    override val title: String
    override val mainIcon: ByteArray?
    override val subTitle: String
    override val subIconRight: ByteArray?

    init {
        val (mainSet, commanderSet) = transaction {
            arrayOf(
                CardSet.findById(code) ?: throw IllegalArgumentException("no set with code $code found"),
                CardSet.findById(subCode) ?: throw IllegalArgumentException("no set with code $subCode found"),
            )
        }

        title = mainSet.name
        mainIcon = mainSet.icon.renderSvgAsMythic()

        subTitle = "incl. ${commanderSet.name}"
        subIconRight = commanderSet.icon.renderSvgAsUncommon()
    }
}

class SetWithCommanderAndAncillary(override val code: String, val commanderCode: String, val ancillaryCode: String): MapLabelItem {
    override val title: String
    override val mainIcon: ByteArray?
    override val subCode: String
    override val subTitle: String
    override val subIconRight: ByteArray?
    override val subIconLeft: ByteArray?

    init {
        val (mainSet, commanderSet, ancillarySet) = transaction {
            arrayOf(
                CardSet.findById(code) ?: throw IllegalArgumentException("no set with code $code found"),
                CardSet.findById(commanderCode) ?: throw IllegalArgumentException("no set with code $commanderCode found"),
                CardSet.findById(ancillaryCode) ?: throw IllegalArgumentException("no set with code $ancillaryCode found"),
            )
        }

        title = mainSet.name
        mainIcon = mainSet.icon.renderSvgAsMythic()
        subCode = "$commanderCode/$ancillaryCode"

        subTitle = "incl. ${commanderSet.name} & ${ancillarySet.name}"
        subIconRight = commanderSet.icon.renderSvgAsUncommon()
        subIconLeft = ancillarySet.icon.renderSvgAsUncommon()
    }
}

class TwoSetBlock(blockTitle: String, code0: String, code1: String): MapLabelItem {
    override val title: String = blockTitle
    override val code: String = code0
    override val mainIcon: ByteArray?
    override val subCode: String = code1
    override val subTitle: String
    override val subIconRight: ByteArray?

    init {
        val cardSets = transaction {
            listOf(code0, code1).map { _code ->
                CardSet.findById(_code) ?: throw IllegalArgumentException("no set with code $code found")
            }
        }

        subTitle = "incl. ${cardSets.joinToString(" & ") { it.name }}"
        mainIcon = cardSets.component1().icon.renderSvgAsMythic()
        subIconRight = cardSets.component2().icon.renderSvgAsUncommon()
    }
}

class ThreeSetBlock(blockTitle: String, code0: String, code1: String, code2: String): MapLabelItem {
    override val title: String = blockTitle
    override val code: String = code0
    override val mainIcon: ByteArray?
    override val subCode: String = "$code1 / $code2"
    override val subTitle: String
    override val subIconLeft: ByteArray?
    override val subIconRight: ByteArray?

    init {
        val cardSets = transaction {
            listOf(code0, code1, code2).map { _code ->
                CardSet.findById(_code) ?: throw IllegalArgumentException("no set with code $code found")
            }
        }

        subTitle =  "incl. ${cardSets.dropLast(1).joinToString(", ") { it.name }}, & ${cardSets.last().name}"
        mainIcon = cardSets.component1().icon.renderSvgAsMythic()
        subIconLeft = cardSets.component2().icon.renderSvgAsUncommon()
        subIconRight = cardSets.component3().icon.renderSvgAsUncommon()
    }
}

class FiveSetBlock(blockTitle: String, code0: String, code1: String, code2: String, code3: String, code4: String): MapLabelItem {
    override val title: String = blockTitle
    override val code: String
    override val mainIcon: ByteArray?
    override val subCode: String
    override val subTitle: String
    override val subIconLeft: ByteArray?
    override val subIconRight: ByteArray?
    override val subIconLeft2: ByteArray?
    override val subIconRight2: ByteArray?

    init {
        val codes = listOf(code0, code1, code2, code3, code4)
        val cardSets = transaction {
            codes.map { CardSet.findById(it) ?: throw IllegalArgumentException("no set with code $it found") }
        }

        code = "UN*"
        subCode = ""
//		code = codes.first()
//		subCode = codes.drop(1).joinToString("/")

        subTitle =  "incl. ${cardSets.dropLast(1).joinToString(", ") { it.name }}, & ${cardSets.last().name}"
        mainIcon = cardSets.component1().icon.renderSvgAsMythic()
        subIconLeft = cardSets.component2().icon.renderSvgAsUncommon()
        subIconRight = cardSets.component3().icon.renderSvgAsUncommon()
        subIconLeft2 = cardSets.component4().icon.renderSvgAsUncommon()
        subIconRight2 = cardSets.component5().icon.renderSvgAsUncommon()
    }
}
