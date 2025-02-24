plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.javafx)
	alias(libs.plugins.spotless)
	application
	distribution
}

project.group = "at.woolph"
version = "0.2.0"

repositories {
	mavenCentral()
	maven(url = "https://oss.sonatype.org/content/repositories/snapshots") // for no.tornado:tornadofx:2.0.0-SNAPSHOT
}

dependencies {
	implementation(kotlin("reflect"))

	implementation(platform("io.ktor:ktor-bom:3.0.3"))
	implementation("io.ktor:ktor-client-core")
	implementation("io.ktor:ktor-client-cio")
	implementation("io.ktor:ktor-client-content-negotiation")
	implementation("io.ktor:ktor-serialization-kotlinx-json")

	implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.10.1"))
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx")

	implementation(platform("org.jetbrains.exposed:exposed-bom:0.57.0"))
	implementation("org.jetbrains.exposed:exposed-dao")
	implementation("org.jetbrains.exposed:exposed-java-time")
	implementation("org.jetbrains.exposed:exposed-jdbc")
	runtimeOnly("com.h2database:h2:2.3.232")

	implementation("org.jsoup:jsoup:1.18.3")
	implementation("org.json:json:20220924")
	implementation("com.opencsv:opencsv:5.7.1")
	implementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
	implementation("org.apache.pdfbox:pdfbox:3.0.4")
	implementation("com.github.dhorions:boxable:1.7.0")
	implementation("org.apache.xmlgraphics:batik-rasterizer:1.14")
	implementation("org.apache.xmlgraphics:batik-codec:1.14")
	implementation("io.github.g0dkar:qrcode-kotlin:4.2.0")

	implementation("no.tornado:tornadofx:2.0.0-SNAPSHOT")

	implementation("com.github.ajalt.clikt:clikt:5.0.2")
	implementation("com.github.ajalt.mordant:mordant:3.0.1")
	implementation("com.github.ajalt.mordant:mordant-coroutines:3.0.1")

	implementation("org.slf4j:slf4j-api:2.0.16")
	implementation("org.slf4j:slf4j-ext:2.0.16")
	runtimeOnly("ch.qos.logback:logback-classic:1.5.15")

	testImplementation("io.kotest:kotest-property:5.5.4")
	testImplementation("io.kotest:kotest-runner-junit5-jvm:5.5.4")
	testImplementation("io.mockk:mockk:1.13.2")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

kotlin {
	jvmToolchain(libs.versions.jvmTarget.get().toInt())
}

javafx {
	version = libs.versions.javafx.get()
	modules = listOf("javafx.controls", "javafx.graphics")
}

application {
	mainClass.set("at.woolph.caco.MainKt")
}

//
//spotless {
//	kotlin {
//		ktlint("0.48.2")
//		licenseHeader("/* Copyright \$YEAR Wolfgang Mayer */")
//	}
//	kotlinGradle {
//		ktlint("0.48.2")
//	}
//}
