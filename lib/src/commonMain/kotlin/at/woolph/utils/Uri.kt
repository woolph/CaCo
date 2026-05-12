/* Copyright 2026 Wolfgang Mayer */
package at.woolph.utils

import kotlinx.serialization.Serializable

@Serializable(with = UriSerializer::class) expect class Uri(value: String) {}
