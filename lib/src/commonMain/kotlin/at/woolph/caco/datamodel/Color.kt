/* Copyright 2026 Wolfgang Mayer */
package at.woolph.caco.datamodel

class Color(val colors: Set<MtgColor>) {
  constructor(vararg color: MtgColor) : this(color.toSet())

  val isColorless: Boolean = colors.isEmpty()

  operator fun contains(color: Color) =
      if (color.colors.isEmpty()) true else color.colors.all { it in colors }

  operator fun contains(color: MtgColor) = colors.contains(color)

  override fun toString(): String = colors.toString()

  operator fun plus(colorIdentity: Color) = this + colorIdentity.colors

  operator fun plus(colorIdentity: Set<MtgColor>) = Color(this.colors + colorIdentity)

  fun encodeAsInteger(): Int =
      colors.fold(0) { acc: Int, MtgColor: MtgColor -> (acc or (1 shl MtgColor.ordinal)) }

  companion object {
    fun decodeFromInteger(value: Int): Color =
        Color(
            MtgColor.entries
                .asSequence()
                .filter { MtgColor -> (value and (1 shl MtgColor.ordinal)) != 0 }
                .toSet()
        )

    operator fun invoke(string: String) =
        when (val colorCode = string.lowercase()) {
          "" -> COLORLESS
          "azorious" -> AZORIOUS
          "dimir" -> DIMIR
          "rakdos" -> RAKDOS
          "gruul" -> GRUUL
          "selesnya" -> SELESNYA
          "orzhov" -> ORZHOV
          "golgari" -> GOLGARI
          "simic" -> SIMIC
          "izzet" -> IZZET
          "boros" -> BOROS
          "esper" -> ESPER
          "grixis" -> GRIXIS
          "jund" -> JUND
          "naya" -> NAYA
          "bant" -> BANT
          "jeskai" -> JESKAI
          "sultai" -> SULTAI
          "mardu" -> MARDU
          "temur" -> TEMUR
          "abzan" -> ABZAN
          "5c" -> FIVE_COLORED
          else ->
              Color(
                  buildSet<MtgColor> {
                    if (colorCode.contains("w")) add(MtgColor.White)
                    if (colorCode.contains("u")) add(MtgColor.Blue)
                    if (colorCode.contains("b")) add(MtgColor.Black)
                    if (colorCode.contains("r")) add(MtgColor.Red)
                    if (colorCode.contains("g")) add(MtgColor.Green)
                  }
              )
        }

    val WHITE = Color(MtgColor.White)
    val BLUE = Color(MtgColor.Blue)
    val BLACK = Color(MtgColor.Black)
    val RED = Color(MtgColor.Red)
    val GREEN = Color(MtgColor.Green)
    val AZORIOUS = Color(MtgColor.Blue, MtgColor.White)
    val DIMIR = Color(MtgColor.Blue, MtgColor.Black)
    val RAKDOS = Color(MtgColor.Red, MtgColor.Black)
    val GRUUL = Color(MtgColor.Red, MtgColor.Green)
    val SELESNYA = Color(MtgColor.White, MtgColor.Green)
    val ORZHOV = Color(MtgColor.White, MtgColor.Black)
    val GOLGARI = Color(MtgColor.Green, MtgColor.Black)
    val SIMIC = Color(MtgColor.Green, MtgColor.Blue)
    val IZZET = Color(MtgColor.Red, MtgColor.Blue)
    val BOROS = Color(MtgColor.Red, MtgColor.White)
    val ESPER = Color(MtgColor.White, MtgColor.Blue, MtgColor.Black)
    val GRIXIS = Color(MtgColor.Blue, MtgColor.Black, MtgColor.Red)
    val JUND = Color(MtgColor.Black, MtgColor.Red, MtgColor.Green)
    val NAYA = Color(MtgColor.Red, MtgColor.Green, MtgColor.White)
    val BANT = Color(MtgColor.Green, MtgColor.White, MtgColor.Blue)
    val JESKAI = Color(MtgColor.White, MtgColor.Blue, MtgColor.Red)
    val SULTAI = Color(MtgColor.Blue, MtgColor.Black, MtgColor.Green)
    val MARDU = Color(MtgColor.Black, MtgColor.Red, MtgColor.White)
    val TEMUR = Color(MtgColor.Red, MtgColor.Green, MtgColor.Blue)
    val ABZAN = Color(MtgColor.Green, MtgColor.White, MtgColor.Black)
    val COLORLESS = Color(MtgColor.Colorless)
    val FIVE_COLORED =
        Color(
            MtgColor.White,
            MtgColor.Blue,
            MtgColor.Black,
            MtgColor.Red,
            MtgColor.Green,
        )
  }
}
