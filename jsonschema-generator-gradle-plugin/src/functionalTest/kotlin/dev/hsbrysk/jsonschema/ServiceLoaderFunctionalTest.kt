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
    fun test() {
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
            jsonSchemaGenerator {
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
            .forwardOutput()
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
