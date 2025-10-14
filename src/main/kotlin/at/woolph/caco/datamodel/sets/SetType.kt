/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel.sets

import kotlinx.serialization.Serializable

@Serializable
enum class SetType {
    CORE,
    EXPANSION,
    MASTERS,
    ALCHEMY,
    MASTERPIECE,
    ARSENAL,
    FROM_THE_VAULT,
    SPELLBOOK,
    PREMIUM_DECK,
    DUEL_DECK,
    DRAFT_INNOVATION,
    TREASURE_CHEST,
    COMMANDER,
    PLANECHASE,
    ARCHENEMY,
    VANGUARD,
    FUNNY,
    STARTER,
    BOX,
    PROMO,
    TOKEN,
    MEMORABILIA,
    MINIGAME,
}
