plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "anychain"

include("core", "cli", "api")

project(":core").projectDir = file("core")
project(":cli").projectDir = file("cli")
project(":api").projectDir = file("api")
