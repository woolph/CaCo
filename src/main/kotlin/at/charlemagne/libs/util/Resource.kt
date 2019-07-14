package at.charlemagne.libs.util

inline fun <reified Clazz> getResource(name: String) = Clazz::class.java.getResource(name)

inline fun <reified Clazz> getResourceAsStream(name: String) = Clazz::class.java.getResourceAsStream(name)
