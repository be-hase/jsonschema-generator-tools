package dev.hsbrysk.jsonschema.module.kotlin

enum class KotlinOption {
    /**
     * Detects Kotlin nullables.
     * For example, when it is `String?`, it will be represented as `"type": ["string", "null"]`.
     */
    USE_NULLABLE,

    /**
     * If a Kotlin data class property does not have a default value, it will be treated as required.
     */
    USE_REQUIRED_VIA_DEFAULT_ARGS
}
