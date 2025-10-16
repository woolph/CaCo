import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.composeHotReload)

  alias(libs.plugins.spotless)
}

group = "at.woolph"

version = "0.3.0"

kotlin {
  jvm()
  compilerOptions { freeCompilerArgs.add("-Xwhen-guards") }

  sourceSets {
    commonMain.dependencies {
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.ui)
      implementation(compose.components.resources)
      implementation(compose.components.uiToolingPreview)
      implementation(libs.androidx.lifecycle.viewmodelCompose)
      implementation(libs.androidx.lifecycle.runtimeCompose)
    }
    commonTest.dependencies { implementation(libs.kotlin.test) }
    jvmMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.kotlinx.coroutinesSwing)
    }
  }
}

compose.desktop {
  application {
    mainClass = "at.woolph.caco.gui.MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "at.woolph.caco"
      packageVersion = "1.0.0"
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
