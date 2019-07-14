package at.charlemagne.libs.util

val String?.isTrue
	get() = if(this == null) false
		else equals("true", ignoreCase = true)
			|| equals("yes", ignoreCase = true)
			|| equals("1", ignoreCase = true)
			|| equals("t", ignoreCase = true)
			|| equals("y", ignoreCase = true)

/**
 * compare for nullable strings
 */
fun String?.compareTo(other: String?) = when {
	this!=null && other!=null -> this.compareTo(other)
	this!=null && other==null ->  1
	this==null && other!=null ->  -1
	else -> 0
}