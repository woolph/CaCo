/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.labels

import at.woolph.caco.datamodel.sets.ScryfallCardSet
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun fetchCardSets(codes: Iterable<String>): List<ScryfallCardSet> = transaction {
  codes.map { code ->
    ScryfallCardSet.findByCode(code)
        ?: throw IllegalArgumentException("no set with code $code found")
  }
}

fun fetchCardSetsNullable(codes: Iterable<String?>): List<ScryfallCardSet?> = transaction {
  codes.map {
    it?.let { code ->
      ScryfallCardSet.findByCode(code)
          ?: throw IllegalArgumentException("no set with code $code found")
    }
  }
}

fun fetchCardSetsNullable(vararg codes: String?) = fetchCardSetsNullable(codes.asIterable())
