@file:Suppress("UnstableApiUsage")

plugins {
    id("conventions.presets.gradle-plugin-module")
}

dependencies {
    implementation(projects.jsonschemaModuleProvider)

    api(libs.aws.sdk.s3)
    implementation(libs.jakarta.validation.api)
    api(libs.jsonschema.generator.core)
    api(libs.jsonschema.generator.jackson)
    api(libs.jsonschema.generator.jakarta.validation)
    implementation(libs.jsonschema.generator.swagger2)
    implementation(libs.swagger.annotations)
}

gradlePlugin {
    val jsonschemaGenerator by plugins.creating {
        id = "dev.hsbrysk.jsonschema-generator"
        displayName = "Gradle Plugin for victools/jsonschema-generator"
        description = """
            Java JSON Schema Generator – creating JSON Schema (Draft 6, Draft 7, Draft 2019-09, or Draft 2020-12) from Java classes
        """.trimIndent()
        tags = listOf("json-schema")
        implementationClass = "dev.hsbrysk.jsonschema.JsonSchemaGeneratorPlugin"
    }

    website = "https://github.com/be-hase/jsonschema-generator-tools"
    vcsUrl = "https://github.com/be-hase/jsonschema-generator-tools"
}
