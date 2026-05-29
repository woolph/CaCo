

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
    mainRun { mainClass = "at.woolph.caco.cli.MainKt" }
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
        "kotlin.time.ExperimentalTime",
    )
  }

  sourceSets {
    commonMain.dependencies {
      implementation(projects.lib)

      implementation("org.jetbrains.kotlinx:kotlinx-html:0.12.0")
      implementation("com.github.ajalt.clikt:clikt:5.0.3")
      implementation("com.github.ajalt.mordant:mordant:3.0.2")
      implementation("com.github.ajalt.mordant:mordant-coroutines:3.0.2")

      implementation("co.touchlab:kermit:2.0.8")

      implementation(project.dependencies.platform("org.jetbrains.exposed:exposed-bom:1.1.1"))
      implementation("org.jetbrains.exposed:exposed-jdbc")
    }
    commonTest.dependencies { implementation(libs.kotlin.test) }
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
