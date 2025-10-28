package at.woolph.caco.binderlabels

import at.woolph.caco.datamodel.sets.MultiSetBlock

class MultiSetBlockLabel(
  multiSetBlock: MultiSetBlock,
) : AbstractLabelItem(multiSetBlock.sets) {
  override val title: String = multiSetBlock.blockName
}
