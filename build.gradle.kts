plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.javafx)
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

	implementation("org.jetbrains.exposed:exposed-dao:0.40.1")
	implementation("org.jetbrains.exposed:exposed-java-time:0.40.1")
	implementation("org.jetbrains.exposed:exposed-jdbc:0.40.1")
	runtimeOnly("com.h2database:h2:2.1.214")

	implementation("org.json:json:20220924")
	implementation("com.opencsv:opencsv:5.7.1")
	implementation("no.tornado:tornadofx:2.0.0-SNAPSHOT")
	implementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
	implementation("org.apache.pdfbox:pdfbox:2.0.27")
	implementation("com.github.dhorions:boxable:1.7.0")
	implementation("org.apache.xmlgraphics:batik-rasterizer:1.14")
	implementation("org.apache.xmlgraphics:batik-codec:1.14")
	implementation("org.jsoup:jsoup:1.15.3")
	implementation("org.jline:jline:3.21.0")

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
