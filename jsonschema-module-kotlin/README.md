# jsonschema-module-kotlin

Kotlin module for [victools/jsonschema-generator](https://github.com/victools/jsonschema-generator).

## Getting Start

```kotlin
val generator = SchemaGenerator(
    SchemaGeneratorConfigBuilder(...
)
    .with(KotlinModule(KotlinOption.USE_NULLABLE))
    .build()
)
```

## Options

- USE_NULLABLE
    - Detects Kotlin nullables.
      For example, when it is `String?`, it will be represented as `"type": ["string", "null"]`.
- USE_REQUIRED_VIA_DEFAULT_ARGS
    - If a Kotlin data class property does not have a default value, it will be treated as required.

## Use via Gradle Plugin

```kotlin
plugins {
    // ...
    id("dev.hsbrysk.jsonschema-generator")
}
dependencies {
    jsonschemaGenerator("dev.hsbrysk.jsonschema:jsonschema-module-kotlin:latest-SNAPSHOT")
}
jsonSchemaGenerator {
    // ...
    customConfigs = mapOf(
        "kotlin.options" to "USE_NULLABLE, USE_REQUIRED_VIA_DEFAULT_ARGS" // comma separated values
    )
    // ...
}
```
