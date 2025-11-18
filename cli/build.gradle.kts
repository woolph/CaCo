plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.spotless)
}

group = "at.woolph"

version = "0.3.0"

repositories {
  mavenCentral()
}

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
      implementation(projects.lib)

      implementation("com.github.ajalt.clikt:clikt:5.0.3")
      implementation("com.github.ajalt.mordant:mordant:3.0.2")
      implementation("com.github.ajalt.mordant:mordant-coroutines:3.0.2")

//      implementation("co.touchlab:kermit:2.0.8")
//      implementation("co.touchlab:kermit-koin:2.0.8")
//
//      implementation(project.dependencies.platform("io.insert-koin:koin-bom:4.1.1"))
//      implementation("io.insert-koin:koin-core")
    }
    commonTest.dependencies { implementation(libs.kotlin.test) }

    jvmMain.dependencies {
      implementation("org.slf4j:slf4j-api:2.0.17")
      implementation("org.slf4j:slf4j-ext:2.0.17")
      runtimeOnly("ch.qos.logback:logback-classic:1.5.19")
    }

    all {
      languageSettings.enableLanguageFeature("ContextParameters")
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
