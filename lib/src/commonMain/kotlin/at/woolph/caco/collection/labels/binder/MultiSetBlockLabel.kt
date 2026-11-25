/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.collection.labels.binder

import at.woolph.caco.datamodel.sets.MultiSetBlock

class MultiSetBlockLabel(
    multiSetBlock: MultiSetBlock,
) : at.woolph.caco.collection.labels.binder.AbstractLabelItem(multiSetBlock.sets) {
  override val title: String = multiSetBlock.blockName
}
