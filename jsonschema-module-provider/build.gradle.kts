plugins {
    id("conventions.presets.java-lib-module")
}

description = "Extension module for jsonschema-generator-gradle-plugin"

dependencies {
    api(libs.jsonschema.generator.core)
}
