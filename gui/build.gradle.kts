plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.spotless)
}

group = "at.woolph"

version = "0.3.0"

repositories { mavenCentral() }

kotlin {
  jvm {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    mainRun { mainClass = "at.woolph.caco.gui.MainKt" }
  }

  compilerOptions {
    languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3)
    freeCompilerArgs.addAll(
      "-Xjsr305=strict",
      "-Xcontext-parameters",
      "-Xcontext-sensitive-resolution",
      "-Xallow-reified-type-in-catch",
      "-Xexpect-actual-classes",
    )
    optIn.addAll(
        "kotlin.uuid.ExperimentalUuidApi",
    )
  }

  sourceSets {
    commonMain { dependencies { implementation(projects.lib) } }
    jvmMain.dependencies {
      implementation(projects.tornadofx2)
      implementation(libs.coroutines.javafx)
    }
  }
}

spotless {
  kotlin {
    target("src/*/kotlin/**/*.kt")
    ktfmt()
    licenseHeader($$"/* Copyright $YEAR Wolfgang Mayer */")
  }
  kotlinGradle { ktfmt() }
}
