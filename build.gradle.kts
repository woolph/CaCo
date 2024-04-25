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
	implementation("org.jetbrains.kotlin:kotlin-stdlib")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	implementation("io.ktor:ktor-client-core:2.2.4")
	implementation("io.ktor:ktor-client-cio:2.2.4")
	implementation("io.ktor:ktor-client-content-negotiation:2.2.4")
	implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.4")

	implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.8.0"))
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx")

	implementation(platform("org.jetbrains.exposed:exposed-bom:0.49.0"))
	implementation("org.jetbrains.exposed:exposed-dao")
	implementation("org.jetbrains.exposed:exposed-java-time")
	implementation("org.jetbrains.exposed:exposed-jdbc")
	runtimeOnly("com.h2database:h2:2.2.224")

	implementation("org.json:json:20220924")
	implementation("com.opencsv:opencsv:5.7.1")
	implementation("no.tornado:tornadofx:2.0.0-SNAPSHOT")
	implementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
	implementation("org.apache.pdfbox:pdfbox:3.0.2")
	implementation("com.github.dhorions:boxable:1.7.0")
	implementation("org.apache.xmlgraphics:batik-rasterizer:1.14")
	implementation("org.apache.xmlgraphics:batik-codec:1.14")
	implementation("org.jsoup:jsoup:1.15.3")

	implementation("com.github.ajalt.clikt:clikt:4.3.0")
	implementation("com.github.ajalt.mordant:mordant:2.4.0")
	implementation("com.github.ajalt.mordant:mordant-coroutines:2.4.0")

	implementation("org.slf4j:slf4j-api:2.0.5")
	implementation("org.slf4j:slf4j-ext:2.0.5")
	runtimeOnly("ch.qos.logback:logback-classic:1.4.5")

	testImplementation("io.kotest:kotest-property:5.5.4")
	testImplementation("io.kotest:kotest-runner-junit5-jvm:5.5.4")
	testImplementation("io.mockk:mockk:1.13.2")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

kotlin {
	jvmToolchain(libs.versions.jvmTarget.get().toInt())

	target.compilations.all {
		kotlinOptions {
			jvmTarget = libs.versions.jvmTarget.get()
		}
	}
}

javafx {
	version = libs.versions.javafx.get()
	modules = listOf("javafx.controls", "javafx.graphics")
}

application {
	mainClass.set("at.woolph.caco.gui.MainKt")
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
