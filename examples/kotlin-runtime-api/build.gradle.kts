plugins {
    kotlin("jvm") version "2.4.0"
    application
}

dependencies {
    implementation("ai.kaios:runtime-core:0.3.1")
    implementation("ai.kaios:tool-runtime:0.3.1")
    implementation("ai.kaios:memory-engine:0.3.1")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("example.KaiosRuntimeApiExampleKt")
}
