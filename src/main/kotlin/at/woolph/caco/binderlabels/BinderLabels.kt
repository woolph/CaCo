package at.woolph.caco.binderlabels

import at.woolph.caco.datamodel.sets.ScryfallCardSet
import at.woolph.caco.icon.lazyIconMythic
import at.woolph.caco.icon.lazyIconUncommon
import at.woolph.caco.icon.lazySetIcon
import at.woolph.caco.icon.mythicBinderLabelIconRenderer
import org.jetbrains.exposed.sql.transactions.transaction

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

object PromosLabel : MapLabelItem {
  override val code: String = "PRM"
  override val title: String = "Promos & Specials"
  override val mainIcon by lazySetIcon("star", mythicBinderLabelIconRenderer)
}

object BlankLabel : MapLabelItem {
  override val code: String = ""
  override val title: String = ""
  override val mainIcon: ByteArray? = null
}

open class GenericLabel(override val code: String, override val title: String, override val subTitle: String? = null) :
  MapLabelItem {
  override val mainIcon by lazySetIcon("default", mythicBinderLabelIconRenderer)
}

fun fetchCardSets(vararg codes: String): List<ScryfallCardSet> = transaction {
  codes.map { code ->
    ScryfallCardSet.findByCode(code) ?: throw IllegalArgumentException("no set with code $code found")
  }
}

fun fetchCardSetsNullable(vararg codes: String?): List<ScryfallCardSet?> = transaction {
  codes.map {
    it?.let { code ->
      ScryfallCardSet.findByCode(code) ?: throw IllegalArgumentException("no set with code $code found")
    }
  }
}

abstract class AbstractLabelItem(
    val sets: List<ScryfallCardSet>,
) : MapLabelItem {
  override val title: String get() = sets[0].name
  override val subTitle: String?
    get() = (if (title == sets[0].name) sets.drop(1) else sets).let { subTitleSets ->
      when (subTitleSets.size) {
        0 -> null
        1 -> "incl. ${subTitleSets[0].name}"
        else -> "incl. ${subTitleSets.dropLast(1).joinToString(", ") { it.name }}, & ${subTitleSets.last().name}"
      }
    }

  override val code: String get() = sets[0].code
  override val subCode: String get() = sets.drop(1).joinToString("/") { it.code }

  override val mainIcon: ByteArray? by sets.getOrNull(0).lazyIconMythic
  override val subIconLeft: ByteArray? by sets.getOrNull(if (sets.size > 2) 1 else 2).lazyIconUncommon
  override val subIconRight: ByteArray? by sets.getOrNull(if (sets.size > 2) 2 else 1).lazyIconUncommon
  override val subIconLeft2: ByteArray? by sets.getOrNull(3).lazyIconUncommon
  override val subIconRight2: ByteArray? by sets.getOrNull(4).lazyIconUncommon
}

open class SimpleSet(override val code: String) : AbstractLabelItem(fetchCardSets(code))

open class SeparatePreconPartSet(code: String) : SimpleSet(code) {
  override val subTitle: String? get() = "Preconstructed Commander Decks"
  override val subCode: String
    get() = "Precons"
}

class SetWithCommander(override val code: String, override val subCode: String) :
  AbstractLabelItem(fetchCardSets(code, subCode))

class SetWithCommanderAndAncillary(override val code: String, commanderCode: String, ancillaryCode: String) :
  AbstractLabelItem(fetchCardSets(code, commanderCode, ancillaryCode))

class Block(blockTitle: String, override val code: String, vararg codes: String) :
  AbstractLabelItem(fetchCardSets(code, *codes)) {
  override val title: String = blockTitle
}
