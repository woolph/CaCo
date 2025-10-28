package at.woolph.caco.binderlabels

import at.woolph.caco.datamodel.sets.IScryfallCardSet
import at.woolph.caco.icon.lazyIconMythic
import at.woolph.caco.icon.lazyIconUncommon

open class AbstractLabelItem(
  val sets: List<IScryfallCardSet>,
) : MapLabelItem {
  override val title: String
    get() = sets[0].name

  override val subTitle: String?
    get() =
        (if (title == sets[0].name) sets.drop(1) else sets).let { subTitleSets ->
          when (subTitleSets.size) {
            0 -> null
            1 -> "incl. ${subTitleSets[0].name}"
            else ->
                "incl. ${subTitleSets.dropLast(1).joinToString(", ") { it.name }}, & ${subTitleSets.last().name}"
          }
        }

  override val code: String
    get() = sets[0].code

  override val subCode: String
    get() = sets.drop(1).joinToString("/") { it.code }

  private val setsWithDistinctIcons: List<IScryfallCardSet> = sets.distinctBy { it.icon }
  override val mainIcon: ByteArray? by setsWithDistinctIcons.getOrNull(0).lazyIconMythic
  override val subIconLeft: ByteArray? by
      setsWithDistinctIcons.getOrNull(if (sets.size > 2) 1 else 2).lazyIconUncommon
  override val subIconRight: ByteArray? by
      setsWithDistinctIcons.getOrNull(if (sets.size > 2) 2 else 1).lazyIconUncommon
  override val subIconLeft2: ByteArray? by setsWithDistinctIcons.getOrNull(3).lazyIconUncommon
  override val subIconRight2: ByteArray? by setsWithDistinctIcons.getOrNull(4).lazyIconUncommon
}
