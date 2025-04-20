package conventions

group = "dev.hsbrysk.jsonschema"

val defaultVersion = "latest-SNAPSHOT"

version = providers.gradleProperty("publishVersion").orNull
    ?: providers.environmentVariable("PUBLISH_VERSION").orNull
    ?: defaultVersion
