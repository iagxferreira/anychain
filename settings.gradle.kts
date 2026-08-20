rootProject.name = "anychain"

include("core", "cli", "api")

project(":core").projectDir = file("core")
project(":cli").projectDir = file("cli")
project(":api").projectDir = file("api")
