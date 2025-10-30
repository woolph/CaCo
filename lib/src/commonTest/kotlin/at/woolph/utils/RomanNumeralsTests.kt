package at.woolph.utils

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RomanNumeralsTests {
  @Test
  fun toRoman() {
    1.toRoman() shouldBe "I"
    2.toRoman() shouldBe "II"
    3.toRoman() shouldBe "III"
    4.toRoman() shouldBe "IV"
    5.toRoman() shouldBe "V"
    14.toRoman() shouldBe "XIV"
    1000.toRoman() shouldBe "M"
  }

  @Test
  fun toRoman_avoidBlasphemy() {
    1.toRoman(avoidBlasphemy = true) shouldBe "I"
    2.toRoman(avoidBlasphemy = true) shouldBe "II"
    3.toRoman(avoidBlasphemy = true) shouldBe "III"
    4.toRoman(avoidBlasphemy = true) shouldBe "IIII"
    5.toRoman(avoidBlasphemy = true) shouldBe "V"
    14.toRoman(avoidBlasphemy = true) shouldBe "XIIII"
    1000.toRoman() shouldBe "M"
  }
}