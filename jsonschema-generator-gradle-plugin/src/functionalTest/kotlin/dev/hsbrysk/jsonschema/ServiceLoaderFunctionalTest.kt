package dev.hsbrysk.jsonschema

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.Path

/**
 * To run this test, please execute `./gradlew publishToMavenLocal` beforehand.
 */
class ServiceLoaderFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val propertiesFile by lazy { projectDir.resolve("gradle.properties") }

    @BeforeEach
    fun beforeEach() {
        settingsFile.writeText(
            // language=kotlin
            """
            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                    mavenLocal()
                }
            }
            """.trimIndent(),
        )
        propertiesFile.writeText(
            // language=kotlin
            """
            """.trimIndent(),
        )
    }

    @Test
    fun basic() {
        buildFile.writeText(
            // language=kotlin
            """
            import com.github.victools.jsonschema.generator.SchemaVersion
            plugins {
                kotlin("jvm") version "$KOTLIN_VERSION"
                id("dev.hsbrysk.jsonschema-generator")
            }
            dependencies {
                implementation("dev.hsbrysk.jsonschema:jsonschema-module-provider:latest-SNAPSHOT")
            }
            jsonschemaGenerator {
                schemaVersion = SchemaVersion.DRAFT_2020_12
                schemas {
                    create("Person") {
                        target = "com.example.Person"
                    }
                }
                customConfigs = mapOf("hoge-key" to "hoge-value")
            }
            """.trimIndent(),
        )

        projectDir.resolve(Path("src", "main", "kotlin", "com", "example").toFile()).mkdirs()
        projectDir.resolve(Path("src", "main", "kotlin", "com", "example", "Person.kt").toFile()).writeText(
            // language=kotlin
            """
            package com.example

            import com.example.SecretString
            import com.github.victools.jsonschema.generator.Module
            import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
            import dev.hsbrysk.jsonschema.ModuleProvider

            data class SecretString(val string: String)

            class SecretStringModule : Module {
                override fun applyToConfigBuilder(builder: SchemaGeneratorConfigBuilder) {
                    builder.forFields().withTargetTypeOverridesResolver { field ->
                        if (SecretString::class.java.isAssignableFrom(field.rawMember.type)) {
                            listOf(field.context.resolve(String::class.java))
                        } else {
                            null
                        }
                    }
                }
            }

            class SecretStringModuleProvider : ModuleProvider {
                override fun provide(customConfigs: Map<String, String>): Module = SecretStringModule()
            }

            data class Person(val loginId: String, val password: SecretString)
            """.trimIndent(),
        )
        projectDir.resolve(Path("src", "main", "resources", "META-INF", "services").toFile()).mkdirs()
        projectDir.resolve(
            Path("src", "main", "resources", "META-INF", "services", "dev.hsbrysk.jsonschema.ModuleProvider").toFile(),
        ).writeText(
            """
            com.example.SecretStringModuleProvider
            """.trimIndent(),
        )

        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("generateJsonSchema")
            .build()

        assertThat(projectDir.resolve(Path("build", "json-schemas", "Person.json").toFile()).readText())
            .isEqualTo(
                // language=json
                """
                {
                  "${'$'}schema" : "https://json-schema.org/draft/2020-12/schema",
                  "type" : "object",
                  "properties" : {
                    "loginId" : {
                      "type" : "string"
                    },
                    "password" : {
                      "type" : "string"
                    }
                  }
                }
                """.trimIndent(),
            )
    }

    @Test
    fun `external dependency`() {
        buildFile.writeText(
            // language=kotlin
            """
            import com.github.victools.jsonschema.generator.SchemaVersion
            plugins {
                java
                id("dev.hsbrysk.jsonschema-generator")
            }
            dependencies {
                implementation("dev.hsbrysk.jsonschema.test:test-type:latest-SNAPSHOT")
                jsonschemaGenerator("dev.hsbrysk.jsonschema.test:test-type-module-provider:latest-SNAPSHOT")
            }
            jsonschemaGenerator {
                schemaVersion = SchemaVersion.DRAFT_2020_12
                schemas {
                    create("Person") {
                        target = "com.example.Person"
                    }
                }
            }
            """.trimIndent(),
        )

        projectDir.resolve(Path("src", "main", "java", "com", "example").toFile()).mkdirs()
        projectDir.resolve(Path("src", "main", "java", "com", "example", "Person.java").toFile()).writeText(
            // language=java
            """
            package com.example;
            import com.example.SecretString;
            public record Person(String loginId, SecretString password) {}
            """.trimIndent(),
        )

        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("generateJsonSchema")
            .build()

        assertThat(projectDir.resolve(Path("build", "json-schemas", "Person.json").toFile()).readText())
            .isEqualTo(
                // language=json
                """
                {
                  "${'$'}schema" : "https://json-schema.org/draft/2020-12/schema",
                  "type" : "object",
                  "properties" : {
                    "loginId" : {
                      "type" : "string"
                    },
                    "password" : {
                      "type" : "string"
                    }
                  }
                }
                """.trimIndent(),
            )
    }
}
