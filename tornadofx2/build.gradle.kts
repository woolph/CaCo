plugins {
  alias(libs.plugins.kotlin.multiplatform)
  id("com.google.osdetector") version "1.7.3"
}

group = "no.tornado"
version = "2.0.0"


repositories {
  mavenCentral()
}

kotlin {
  jvm()

  sourceSets {
    jvmMain {
      dependencies {
        dependencies {
          // As JavaFX have platform-specific dependencies, we need to add them manually
          val fxSuffix = when (osdetector.classifier) {
            "linux-x86_64" -> "linux"
            "linux-aarch_64" -> "linux-aarch64"
            "windows-x86_64" -> "win"
            "osx-x86_64" -> "mac"
            "osx-aarch_64" -> "mac-aarch64"
            else -> throw IllegalStateException("Unknown OS: ${osdetector.classifier}")
          }

          api("org.openjfx:javafx-base:21.0.9:${fxSuffix}")
          api("org.openjfx:javafx-graphics:21.0.9:${fxSuffix}")
          api("org.openjfx:javafx-controls:21.0.9:${fxSuffix}")
          compileOnly("org.openjfx:javafx-fxml:21.0.9:${fxSuffix}")
          compileOnly("org.openjfx:javafx-media:21.0.9:${fxSuffix}")
          compileOnly("org.openjfx:javafx-web:21.0.9:${fxSuffix}")
        }

        implementation(kotlin("reflect"))

        api("org.glassfish:javax.json:1.1.2")
        api("org.apache.httpcomponents:httpclient:4.5.3")
        api("de.jensd:fontawesomefx-fontawesome:4.7.0-9.1.2")
        implementation("org.apache.felix:org.apache.felix.framework:6.0.1")
      }
    }
    jvmTest {
      dependencies {
        //common
        implementation("org.hamcrest:hamcrest:3.0")
        implementation("org.hamcrest:hamcrest-library:3.0")
        implementation("org.testfx:testfx-junit5:4.0.18")
        //Junit 5
        implementation("org.junit.jupiter:junit-jupiter:6.0.1")
        implementation("org.jetbrains.kotlin:kotlin-test-junit5")
        runtimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.1")
        //headless
        runtimeOnly("org.testfx:openjfx-monocle:21.0.2")
      }
    }
  }
}
