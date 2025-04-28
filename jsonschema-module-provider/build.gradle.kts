plugins {
    id("conventions.presets.java-lib-module")
}

description = "Module for the jsonschema-generator-gradle-plugin"

dependencies {
    api(libs.jsonschema.generator.core)
}
