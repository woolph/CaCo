plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.spotless)
//  application
//  distribution
}

group = "at.woolph"
version = "0.3.0"

kotlin {
  jvm()

  compilerOptions {
    freeCompilerArgs.add("-Xwhen-guards")
  }

  sourceSets {
    commonMain.dependencies {
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
    }
    jvmMain.dependencies {
      implementation(projects.lib)
      implementation(kotlin("reflect"))

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
//
//application {
//  mainClass.set("at.woolph.caco.cli.MainKt")
//}
//
//spotless {
//  kotlin {
//    ktlint()
//    licenseHeader("/* Copyright \$YEAR Wolfgang Mayer */")
//  }
//  kotlinGradle {
//    ktlint()
//  }
//}
