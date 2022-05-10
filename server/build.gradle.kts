plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    application
}

group = "com.lightningkite.ktor-batteries"

repositories {
    mavenLocal()
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven(url = "https://s01.oss.sonatype.org/content/repositories/releases/")
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
    mavenCentral()
}

val batteriesVersion: String by extra
dependencies {
    api("com.lightningkite.ktorbatteries:server:$batteriesVersion")
    ksp("com.lightningkite.ktorbatteries:processor:$batteriesVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}
