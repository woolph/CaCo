package at.woolph.utils

fun Number.toRoman(avoidBlasphemy: Boolean = false): String = toRoman(avoidBlasphemy, "", toInt())

internal tailrec fun toRoman(avoidBlasphemy: Boolean, romanLiteral: String, remainingNumber: Int): String {
  val (newRomanLiteral, newRemainingNumber) = when {
    remainingNumber >= 1000 -> "M" to (remainingNumber - 1000)
    remainingNumber >= 900 -> "CM" to (remainingNumber - 900)
    remainingNumber >= 500 -> "D" to (remainingNumber - 500)
    remainingNumber >= 400 -> "CD" to (remainingNumber - 400)
    remainingNumber >= 100 -> "C" to (remainingNumber - 100)
    remainingNumber >= 90 -> "XC" to (remainingNumber - 90)
    remainingNumber >= 50 -> "L" to (remainingNumber - 50)
    remainingNumber >= 40 -> "XL" to (remainingNumber - 40)
    remainingNumber >= 10 -> "X" to (remainingNumber - 10)
    remainingNumber >= 9 -> "IX" to (0)
    remainingNumber >= 5 -> "V" to (remainingNumber - 5)
    remainingNumber >= 4 && !avoidBlasphemy -> "IV" to (0)
    remainingNumber >= 1 -> "I" to (remainingNumber - 1)
    else -> return romanLiteral
  }

  return toRoman(avoidBlasphemy, romanLiteral + newRomanLiteral, newRemainingNumber)
}
