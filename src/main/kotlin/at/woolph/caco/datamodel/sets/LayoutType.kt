package at.woolph.caco.datamodel.sets

import kotlinx.serialization.Serializable

@Serializable
enum class LayoutType(
  val isDoubleFaced: Boolean = false,
  val isMemorabilia: Boolean = false,
  val isAdditionalGamePiece: Boolean = false,
) {
  NORMAL,
  TOKEN(isAdditionalGamePiece = true),
  DOUBLE_FACED_TOKEN(isAdditionalGamePiece = true, isDoubleFaced = true),
  EMBLEM(isAdditionalGamePiece = true),
  REVERSIBLE_CARD,
  TRANSFORM(isDoubleFaced = true),
  SPLIT,
  MUTATE,
  CLASS,
  CASE,
  FLIP,
  HOST,
  AUGMENT,
  ADVENTURE,
  LEVELER,
  PROTOTYPE,
  SAGA,
  MELD(isDoubleFaced = true),
  MODAL_DFC(isDoubleFaced = true),
  SCHEME(isAdditionalGamePiece = true),
  PLANAR(isAdditionalGamePiece = true),
  VANGUARD(isAdditionalGamePiece = true),
  ART_SERIES(isMemorabilia = true),
}
