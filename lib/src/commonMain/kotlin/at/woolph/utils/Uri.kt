package at.woolph.utils

import kotlinx.serialization.Serializable

@Serializable(with = UriSerializer::class)
expect class Uri(value: String) {
}
