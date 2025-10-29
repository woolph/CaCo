package at.woolph.caco.labels.binder

import at.woolph.caco.datamodel.sets.MultiSetBlock

class MultiSetBlockLabel(
  multiSetBlock: MultiSetBlock,
) : AbstractLabelItem(multiSetBlock.sets) {
  override val title: String = multiSetBlock.blockName
}
