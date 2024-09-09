package at.woolph.libs.regex

operator fun MatchGroupCollection?.get(index: Int): MatchGroup? = if(this!=null) this[index] else null