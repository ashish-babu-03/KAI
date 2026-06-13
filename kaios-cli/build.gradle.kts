plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

dependencies {
    implementation(project(":runtime-core"))
    implementation(project(":tool-runtime"))
    implementation(project(":memory-engine"))
    implementation(project(":model-providers"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}

application {
    applicationName = "kaios"
    mainClass.set("ai.kaios.cli.MainKt")
}
