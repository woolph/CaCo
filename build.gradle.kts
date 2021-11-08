/*
 * gradle build script
 * @author 001121673 Wolfgang Mayer
 */
plugins {
	kotlin("jvm") version "1.5.31"
	application
	distribution
	id("org.openjfx.javafxplugin") version "0.0.10"
}

project.group = "at.woolph"
apply(from="version.kts")

repositories {
	mavenCentral()
	maven(url = "https://oss.sonatype.org/content/repositories/snapshots") // for no.tornado:tornadofx:2.0.0-SNAPSHOT
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-stdlib")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	implementation("org.jetbrains.exposed:exposed:0.17.13")
	implementation("org.json:json:20180813")
	implementation("com.opencsv:opencsv:4.4")
	implementation("no.tornado:tornadofx:2.0.0-SNAPSHOT")
	implementation("javax.xml.bind:jaxb-api:2.1")
	implementation("com.github.dhorions:boxable:1.5")
	implementation("org.apache.xmlgraphics:batik-rasterizer:1.7")
	implementation("org.apache.xmlgraphics:batik-codec:1.7")
	implementation("org.jsoup:jsoup:1.13.1")

	implementation("org.slf4j:slf4j-api:1.7.31")
	implementation("org.slf4j:slf4j-ext:1.7.31")

	runtimeOnly("ch.qos.logback:logback-classic:1.2.3")
	runtimeOnly("com.h2database:h2:1.4.197")

	testImplementation("io.kotest:kotest-property:4.6.1")
	testImplementation("io.kotest:kotest-runner-junit5-jvm:4.6.1")
	testImplementation("io.mockk:mockk:1.9.3.kotlin12")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

java {
	sourceCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
	kotlinOptions {
//		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
	}
}

javafx {
	version = "11.0.2"
	modules = listOf("javafx.controls", "javafx.graphics")
}

//tasks.jar {
//	manifest {
//		attributes["Main-Class"] = "at.woolph.caco.MainGuiKt"
//		//attributes "SplashScreen-Image": renderSvg.resourceImage['splashScreen']
//	}
//}

application {
	mainClass.set("at.woolph.caco.MainGuiKt")
}
