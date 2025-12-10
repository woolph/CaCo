package at.woolph.caco.datamodel

class ColorIdentity(val colorIdentity: Set<MtgColor>) {
  constructor(vararg color: MtgColor) : this(color.toSet())

  operator fun contains(color: ColorIdentity) =
      if (color.colorIdentity.isEmpty()) true else color.colorIdentity.all { it in colorIdentity }

  operator fun contains(color: Color) =
      if (color.colors.isEmpty()) true else color.colors.all { it in colorIdentity }

  operator fun contains(color: MtgColor) = colorIdentity.contains(color)

  override fun toString(): String = colorIdentity.toString()

  operator fun plus(colorIdentity: ColorIdentity) = this + colorIdentity.colorIdentity

  operator fun plus(colorIdentity: Set<MtgColor>) =
      ColorIdentity(this.colorIdentity + colorIdentity)

  fun encodeAsInteger(): Int = colorIdentity.fold(0) { acc: Int, mtgColor: MtgColor ->
    (acc or (1 shl mtgColor.ordinal))
  }

  companion object {
    fun decodeFromInteger(value: Int): ColorIdentity =
      ColorIdentity(
        MtgColor.entries
          .asSequence()
          .filter { mtgColor -> (value and (1 shl mtgColor.ordinal)) != 0 }
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
              ColorIdentity(
                  buildSet<MtgColor> {
                    if (colorCode.contains("w")) add(MtgColor.White)
                    if (colorCode.contains("u")) add(MtgColor.Blue)
                    if (colorCode.contains("b")) add(MtgColor.Black)
                    if (colorCode.contains("r")) add(MtgColor.Red)
                    if (colorCode.contains("g")) add(MtgColor.Green)
                  }
              )
        }

    val WHITE = ColorIdentity(MtgColor.White)
    val BLUE = ColorIdentity(MtgColor.Blue)
    val BLACK = ColorIdentity(MtgColor.Black)
    val RED = ColorIdentity(MtgColor.Red)
    val GREEN = ColorIdentity(MtgColor.Green)
    val AZORIOUS = ColorIdentity(MtgColor.Blue, MtgColor.White)
    val DIMIR = ColorIdentity(MtgColor.Blue, MtgColor.Black)
    val RAKDOS = ColorIdentity(MtgColor.Red, MtgColor.Black)
    val GRUUL = ColorIdentity(MtgColor.Red, MtgColor.Green)
    val SELESNYA = ColorIdentity(MtgColor.White, MtgColor.Green)
    val ORZHOV = ColorIdentity(MtgColor.White, MtgColor.Black)
    val GOLGARI = ColorIdentity(MtgColor.Green, MtgColor.Black)
    val SIMIC = ColorIdentity(MtgColor.Green, MtgColor.Blue)
    val IZZET = ColorIdentity(MtgColor.Red, MtgColor.Blue)
    val BOROS = ColorIdentity(MtgColor.Red, MtgColor.White)
    val ESPER = ColorIdentity(MtgColor.White, MtgColor.Blue, MtgColor.Black)
    val GRIXIS = ColorIdentity(MtgColor.Blue, MtgColor.Black, MtgColor.Red)
    val JUND = ColorIdentity(MtgColor.Black, MtgColor.Red, MtgColor.Green)
    val NAYA = ColorIdentity(MtgColor.Red, MtgColor.Green, MtgColor.White)
    val BANT = ColorIdentity(MtgColor.Green, MtgColor.White, MtgColor.Blue)
    val JESKAI = ColorIdentity(MtgColor.White, MtgColor.Blue, MtgColor.Red)
    val SULTAI = ColorIdentity(MtgColor.Blue, MtgColor.Black, MtgColor.Green)
    val MARDU = ColorIdentity(MtgColor.Black, MtgColor.Red, MtgColor.White)
    val TEMUR = ColorIdentity(MtgColor.Red, MtgColor.Green, MtgColor.Blue)
    val ABZAN = ColorIdentity(MtgColor.Green, MtgColor.White, MtgColor.Black)
    val COLORLESS = ColorIdentity(MtgColor.Colorless)
    val FIVE_COLORED =
        ColorIdentity(
            MtgColor.White,
            MtgColor.Blue,
            MtgColor.Black,
            MtgColor.Red,
            MtgColor.Green,
        )
  }
}
