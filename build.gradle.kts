plugins {
    kotlin("jvm") version "1.5.31"
    application
}

group = "ru.spbu.math-cs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val osName = System.getProperty("os.name")
val targetOs = when {
    osName == "Mac OS X" -> "macos"
    osName.startsWith("Win") -> "windows"
    osName.startsWith("Linux") -> "linux"
    else -> error("Unsupported OS: $osName")
}

val osArch = System.getProperty("os.arch")
var targetArch = when (osArch) {
    "x86_64", "amd64" -> "x64"
    "aarch64" -> "arm64"
    else -> error("Unsupported arch: $osArch")
}

val target = "${targetOs}-${targetArch}"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.4.1")
    implementation("org.apache.xmlgraphics:batik-all:1.14")
    implementation("com.github.ajalt.clikt:clikt:3.3.0")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.1.0")
    testImplementation(kotlin("test"))
}


tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")
}