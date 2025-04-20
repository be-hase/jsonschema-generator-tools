package conventions.presets

plugins {
    id("conventions.detekt")
    id("conventions.functional-test")
    id("conventions.java")
    id("conventions.kotlin")
    id("conventions.ktlint")
    id("conventions.test")
    id("conventions.versioning")
    `java-gradle-plugin`
    signing
    id("com.gradle.plugin-publish")
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

signing {
    if (project.version.toString().endsWith("-SNAPSHOT")) {
        isRequired = false
    }
    useInMemoryPgpKeys(
        providers.environmentVariable("SIGNING_PGP_KEY").orNull,
        providers.environmentVariable("SIGNING_PGP_PASSWORD").orNull,
    )
}
