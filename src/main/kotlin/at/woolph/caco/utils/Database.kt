/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.utils

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass

fun <ID : Comparable<ID>, T : Entity<ID>> EntityClass<ID, T>.newOrUpdate(
    id: ID,
    setter: (T) -> Unit,
): T = findById(id)?.apply(setter) ?: new(id, setter)
