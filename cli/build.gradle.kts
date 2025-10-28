plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.spotless)
}

group = "at.woolph"

version = "0.3.0"

kotlin {
  jvm {
    mainRun {
      mainClass = "at.woolph.caco.cli.MainKt"
    }
  }

  compilerOptions {
    languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3)
    freeCompilerArgs.addAll(
      "-Xwhen-guards",
      "-Xexpect-actual-classes",
    )
    optIn.addAll(
      "kotlin.uuid.ExperimentalUuidApi",
    )
  }

  sourceSets {
    commonMain.dependencies {
      implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
      implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")
    }
    commonTest.dependencies { implementation(libs.kotlin.test) }
    jvmMain.dependencies {
      implementation(projects.lib)

      implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
      implementation("com.github.ajalt.clikt:clikt:5.0.2")
      implementation("com.github.ajalt.mordant:mordant:3.0.1")
      implementation("com.github.ajalt.mordant:mordant-coroutines:3.0.1")

      implementation("org.slf4j:slf4j-api:2.0.17")
      implementation("org.slf4j:slf4j-ext:2.0.17")
      runtimeOnly("ch.qos.logback:logback-classic:1.5.19")

      runtimeOnly("ch.qos.logback:logback-classic:1.5.19")
    }
  }
}

spotless {
  kotlin {
    target("src/*/kotlin/**/*.kt")
    ktfmt()
    licenseHeader("/* Copyright \$YEAR Wolfgang Mayer */")
  }
  kotlinGradle { ktfmt() }
}
