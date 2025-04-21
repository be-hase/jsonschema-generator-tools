plugins {
    id("conventions.presets.test-module")
}

dependencies {
    implementation(projects.test.testType)
    implementation(projects.jsonschemaModuleProvider)
}
