plugins {
    kotlin("jvm") version "2.4.0"
    application
}

group = "examples.kaios"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("examples.kaios.billing.AppKt")
}
