package conventions.presets

plugins {
    id("conventions.java")
    id("conventions.kotlin")
    `maven-publish`
}

group = "dev.hsbrysk.jsonschema.test"
version = "latest-SNAPSHOT"

publishing {
    publications {
        create<MavenPublication>("test") {
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
        }
    }
}
