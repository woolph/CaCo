/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.datamodel.sets

sealed interface Block {
  val blockCode: String
  val blockName: String
}

data class SingleSetBlock(val set: ScryfallCardSet) : Block {
  override val blockCode: String
    get() = set.code

  override val blockName: String
    get() = set.name
}

data class MultiSetBlock(
    override val blockCode: String,
    override val blockName: String,
    val sets: List<ScryfallCardSet>,
) : Block
