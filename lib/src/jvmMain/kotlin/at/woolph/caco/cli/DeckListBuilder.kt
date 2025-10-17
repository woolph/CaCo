/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.cli

import at.woolph.caco.datamodel.decks.Format

class DeckListBuilder {
  var name: String? = null
  var format: Format = Format.Unknown
  val deckZones: MutableMap<DeckZone, MutableMap<String, Int>> = mutableMapOf()

  fun name(name: String) = apply { this.name = name }
  fun format(format: Format) = apply { this.format = format }
  fun add(deckZone: DeckZone, name: String, amount: Int) = apply {
    deckZones.computeIfAbsent(deckZone) { mutableMapOf() }
      .compute(name) { _, value -> if (value == null) amount else value + amount }
  }

  fun validate() {
    DeckZone.entries.forEach { deckZone ->
      if (deckZone.isMandatory)
        require(!format.zones.contains(deckZone) || !deckZones[deckZone].isNullOrEmpty()) { "$deckZone must not be empty for format $format"}
      else
        require(format.zones.contains(deckZone) || deckZones[deckZone].isNullOrEmpty()) { "$deckZone should be empty for format $format" }
    }
  }

  fun build(): DeckList {
    validate()
    return DeckList(
      name = name ?: if (format.zones.contains(DeckZone.COMMAND_ZONE)) deckZones[DeckZone.COMMAND_ZONE]!!.keys.joinToString(" & ") { it } else "<unnamed>",
      format = format,
      deckZones = deckZones.entries.associate { (deckZone, cards) -> deckZone to cards.toMap() },
    )
  }
}
