plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.spotless)
  application
  distribution
}

group = "at.woolph"
version = "0.3.0"

repositories {
  maven(url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev"))
  maven(url = uri("https://repo1.maven.org/maven2/"))
  mavenCentral()
}

val doodleVersion = "0.11.3" // <--- Latest Doodle version

// could be moved to buildSrc, but kept here for clarity
fun osTarget(): String {
  val osName = System.getProperty("os.name")
  val targetOs = when {
    osName == "Mac OS X"       -> "macos"
    osName.startsWith("Win"  ) -> "windows"
    osName.startsWith("Linux") -> "linux"
    else                       -> error("Unsupported OS: $osName")
  }

  val targetArch = when (val osArch = System.getProperty("os.arch")) {
    "x86_64", "amd64" -> "x64"
    "aarch64"         -> "arm64"
    else              -> error("Unsupported arch: $osArch")
  }

  return "${targetOs}-${targetArch}"
}

dependencies {
  implementation(kotlin("reflect"))

  implementation(platform("io.ktor:ktor-bom:3.3.1"))
  implementation("io.ktor:ktor-client-core")
  implementation("io.ktor:ktor-client-cio")
  implementation("io.ktor:ktor-client-content-negotiation")
  implementation("io.ktor:ktor-serialization-kotlinx-json")

  implementation(libs.arrow.core)
  implementation(libs.arrow.fx.coroutines)

  implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.10.2"))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

  implementation(platform("org.jetbrains.exposed:exposed-bom:0.61.0"))
  implementation("org.jetbrains.exposed:exposed-dao")
  implementation("org.jetbrains.exposed:exposed-java-time")
  implementation("org.jetbrains.exposed:exposed-jdbc")
  runtimeOnly("com.h2database:h2:2.3.232")

//    implementation("org.jsoup:jsoup:1.18.3")
//    implementation("org.json:json:20220924")
  implementation("com.opencsv:opencsv:5.7.1")
//    implementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
  implementation("org.apache.pdfbox:pdfbox:3.0.4")
  implementation("com.github.dhorions:boxable:1.7.0")
  implementation("org.apache.xmlgraphics:batik-rasterizer:1.14")
  implementation("org.apache.xmlgraphics:batik-codec:1.14")
  implementation("io.github.g0dkar:qrcode-kotlin:4.2.0")

  implementation("com.github.ajalt.clikt:clikt:5.0.2")
  implementation("com.github.ajalt.mordant:mordant:3.0.1")
  implementation("com.github.ajalt.mordant:mordant-coroutines:3.0.1")

  implementation("org.slf4j:slf4j-api:2.0.17")
  implementation("org.slf4j:slf4j-ext:2.0.17")
  runtimeOnly("ch.qos.logback:logback-classic:1.5.19")

  testImplementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.10.2"))
  testImplementation("io.kotest:kotest-property:6.0.3")
  testImplementation("io.kotest:kotest-runner-junit5-jvm:6.0.3")
  testImplementation("io.mockk:mockk:1.13.2")

  // helper to derive OS/architecture pair
  when (osTarget()) {
    "macos-x64"     -> implementation("io.nacular.doodle:desktop-jvm-macos-x64:$doodleVersion")
    "macos-arm64"   -> implementation("io.nacular.doodle:desktop-jvm-macos-arm64:$doodleVersion")
    "linux-x64"     -> implementation("io.nacular.doodle:desktop-jvm-linux-x64:$doodleVersion")
    "linux-arm64"   -> implementation("io.nacular.doodle:desktop-jvm-linux-arm64:$doodleVersion")
    "windows-x64"   -> implementation("io.nacular.doodle:desktop-jvm-windows-x64:$doodleVersion")
    "windows-arm64" -> implementation("io.nacular.doodle:desktop-jvm-windows-arm64:$doodleVersion")
  }

  implementation ("io.nacular.doodle:controls:$doodleVersion" )
  implementation ("io.nacular.doodle:animation:$doodleVersion")
  implementation ("io.nacular.doodle:themes:$doodleVersion")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

kotlin {
  jvmToolchain(libs.versions.jvmTarget.get().toInt())
  compilerOptions {
    freeCompilerArgs.add("-Xwhen-guards")
  }
}

application {
  mainClass.set("at.woolph.caco.MainKt")
}

spotless {
  kotlin {
    ktlint()
    licenseHeader("/* Copyright \$YEAR Wolfgang Mayer */")
  }
  kotlinGradle {
    ktlint()
  }
}
