plugins {
    id("conventions.presets.kotlin-lib-module")
}

description = "Module for the jsonschema-generator – Kotlin support"

dependencies {
    api(libs.jsonschema.generator.core)
    implementation(projects.jsonschemaModuleProvider)
    implementation(libs.kotlin.reflect)
}
