plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":runtime-core"))
    implementation(project(":tool-runtime"))
    implementation(project(":memory-engine"))
}

application {
    applicationName = "kaios"
    mainClass.set("ai.kaios.cli.MainKt")
}
