plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    application
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // kotlinx-serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
}

application {
    mainClass.set("lab.MainKt")
}

kotlin {
    jvmToolchain(21)
}
