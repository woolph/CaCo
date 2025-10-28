plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.spotless)
}

group = "at.woolph"

version = "0.3.0"

kotlin {
  jvm { testRuns["test"].executionTask.configure { useJUnitPlatform() } }

  compilerOptions {
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
      implementation(project.dependencies.platform("io.ktor:ktor-bom:3.3.1"))
      implementation("io.ktor:ktor-client-core")
      implementation("io.ktor:ktor-client-cio")
      implementation("io.ktor:ktor-client-content-negotiation")
      implementation("io.ktor:ktor-serialization-kotlinx-json")
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
    }
    jvmMain.dependencies {
      implementation(kotlin("reflect"))

      api(libs.arrow.core)
      implementation(libs.arrow.fx.coroutines)

      api(project.dependencies.platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.10.2"))
      api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

      api(project.dependencies.platform("org.jetbrains.exposed:exposed-bom:0.61.0"))
      api("org.jetbrains.exposed:exposed-dao")
      implementation("org.jetbrains.exposed:exposed-java-time")
      runtimeOnly("org.jetbrains.exposed:exposed-jdbc")
      runtimeOnly("com.h2database:h2:2.3.232")

      //    implementation("org.jsoup:jsoup:1.18.3")
      //    implementation("org.json:json:20220924")
      implementation("com.opencsv:opencsv:5.7.1")
      //    implementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
      api(
          "org.apache.pdfbox:pdfbox:3.0.4"
      ) // currently api as cli is using pdf stuff (should be moved here?)
      api(
          "com.github.dhorions:boxable:1.7.0"
      ) // currently api as cli is using pdf stuff (should be moved here?)
      implementation("org.apache.xmlgraphics:batik-rasterizer:1.14")
      implementation("org.apache.xmlgraphics:batik-codec:1.14")
      implementation("io.github.g0dkar:qrcode-kotlin:4.2.0")

      implementation("org.slf4j:slf4j-api:2.0.17")
      implementation("org.slf4j:slf4j-ext:2.0.17")
    }

    jvmTest.dependencies {
      implementation("io.kotest:kotest-property:6.0.3")
      implementation(libs.kotlin.testJunit)
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
