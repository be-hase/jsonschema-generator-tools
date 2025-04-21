package dev.hsbrysk.jsonschema

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.Path

class JacksonFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    @BeforeEach
    fun beforeEach() {
        settingsFile.writeText(
            // language=kotlin
            """
            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun java() {
        buildFile.writeText(
            // language=kotlin
            """
            import com.github.victools.jsonschema.generator.SchemaVersion
            plugins {
                java
                id("dev.hsbrysk.jsonschema-generator")
            }
            dependencies {
                implementation("com.fasterxml.jackson.core:jackson-databind:$JACKSON_VERSION")
            }
            jsonSchemaGenerator {
                schemaVersion = SchemaVersion.DRAFT_2020_12
                modules {
                    jacksonEnabled = true
                }
                schemas {
                    create("Pojo") {
                        target = "com.example.Pojo"
                    }
                }
            }
            """.trimIndent(),
        )

        projectDir.resolve(Path("src", "main", "java", "com", "example").toFile()).mkdirs()
        projectDir.resolve(Path("src", "main", "java", "com", "example", "Pojo.java").toFile()).writeText(
            // language=java
            """
            package com.example;
            public class Pojo {
                private final Animal animal;

                public Pojo(Animal animal) {
                    this.animal = animal;
                }

                public Animal getAnimal() {
                    return animal;
                }
            }
            """.trimIndent(),
        )
        projectDir.resolve(Path("src", "main", "java", "com", "example", "Animal.java").toFile()).writeText(
            // language=java
            """
            package com.example;
            import com.fasterxml.jackson.annotation.JsonSubTypes;
            import com.fasterxml.jackson.annotation.JsonTypeInfo;
            @JsonTypeInfo(
                    use = JsonTypeInfo.Id.NAME,
                    property = "type",
                    defaultImpl = UMA.class
            )
            @JsonSubTypes({
                    @JsonSubTypes.Type(Dog.class),
                    @JsonSubTypes.Type(Cat.class),
                    @JsonSubTypes.Type(UMA.class)
            })
            public interface Animal {}
            """.trimIndent(),
        )
        projectDir.resolve(Path("src", "main", "java", "com", "example", "Dog.java").toFile()).writeText(
            // language=java
            """
            package com.example;
            import com.fasterxml.jackson.annotation.JsonPropertyDescription;
            import com.fasterxml.jackson.annotation.JsonTypeName;
            @JsonTypeName("dog")
            public class Dog implements Animal {
                private final String name;

                @JsonPropertyDescription("breed description")
                private final String breed;

                public Dog(String name, String breed) {
                    this.name = name;
                    this.breed = breed;
                }

                public String getName() {
                    return name;
                }

                public String getBreed() {
                    return breed;
                }
            }
            """.trimIndent(),
        )
        projectDir.resolve(Path("src", "main", "java", "com", "example", "Cat.java").toFile()).writeText(
            // language=java
            """
            package com.example;
            import com.fasterxml.jackson.annotation.JsonTypeName;
            @JsonTypeName("cat")
            public class Cat implements Animal {
                private final String name;
                private final String color;

                public Cat(String name, String color) {
                    this.name = name;
                    this.color = color;
                }

                public String getName() {
                    return name;
                }

                public String getColor() {
                    return color;
                }
            }
            """.trimIndent(),
        )

        projectDir.resolve(Path("src", "main", "java", "com", "example", "UMA.java").toFile()).writeText(
            // language=java
            """
            package com.example;
            import com.fasterxml.jackson.annotation.JsonTypeName;
            @JsonTypeName("uma")
            public class UMA implements Animal {}
            """.trimIndent(),
        )

        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("generateJsonSchema")
            .build()

        assertThat(projectDir.resolve(Path("build", "json-schemas", "Pojo.json").toFile()).readText())
            .isEqualTo(
                // language=json
                """
                {
                  "${'$'}schema" : "https://json-schema.org/draft/2020-12/schema",
                  "type" : "object",
                  "properties" : {
                    "animal" : {
                      "anyOf" : [ {
                        "type" : "object",
                        "properties" : {
                          "breed" : {
                            "type" : "string",
                            "description" : "breed description"
                          },
                          "name" : {
                            "type" : "string"
                          },
                          "type" : {
                            "const" : "dog"
                          }
                        },
                        "required" : [ "type" ]
                      }, {
                        "type" : "object",
                        "properties" : {
                          "color" : {
                            "type" : "string"
                          },
                          "name" : {
                            "type" : "string"
                          },
                          "type" : {
                            "const" : "cat"
                          }
                        },
                        "required" : [ "type" ]
                      }, {
                        "type" : "object",
                        "properties" : {
                          "type" : {
                            "const" : "uma"
                          }
                        }
                      } ]
                    }
                  }
                }
                """.trimIndent(),
            )
    }

    @Test
    fun kotlin() {
        buildFile.writeText(
            // language=kotlin
            """
            import com.github.victools.jsonschema.generator.SchemaVersion
            plugins {
                kotlin("jvm") version "$KOTLIN_VERSION"
                id("dev.hsbrysk.jsonschema-generator")
            }
            dependencies {
                implementation("com.fasterxml.jackson.core:jackson-databind:$JACKSON_VERSION")
            }
            jsonSchemaGenerator {
                schemaVersion = SchemaVersion.DRAFT_2020_12
                modules {
                    jacksonEnabled = true
                }
                schemas {
                    create("Pojo") {
                        target = "com.example.Pojo"
                    }
                }
            }
            """.trimIndent(),
        )

        projectDir.resolve(Path("src", "main", "kotlin", "com", "example").toFile()).mkdirs()
        projectDir.resolve(Path("src", "main", "kotlin", "com", "example", "Pojo.kt").toFile()).writeText(
            // language=kotlin
            """
            package com.example

            import com.fasterxml.jackson.annotation.JsonPropertyDescription
            import com.fasterxml.jackson.annotation.JsonSubTypes
            import com.fasterxml.jackson.annotation.JsonTypeInfo
            import com.fasterxml.jackson.annotation.JsonTypeName

            data class Pojo(val animal: Animal)

            @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                property = "type",
                defaultImpl = UMA::class,
            )
            @JsonSubTypes(
                JsonSubTypes.Type(Dog::class),
                JsonSubTypes.Type(Cat::class),
                JsonSubTypes.Type(UMA::class),
            )
            sealed interface Animal

            @JsonTypeName("dog")
            data class Dog(
                val name: String,
                @field:JsonPropertyDescription("breed description")
                val breed: String,
            ) : Animal

            @JsonTypeName("cat")
            data class Cat(
                val name: String,
                val color: String,
            ) : Animal

            @JsonTypeName("uma")
            data object UMA : Animal
            """.trimIndent(),
        )

        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("generateJsonSchema")
            .build()

        assertThat(projectDir.resolve(Path("build", "json-schemas", "Pojo.json").toFile()).readText())
            .isEqualTo(
                // language=json
                """
                {
                  "${'$'}schema" : "https://json-schema.org/draft/2020-12/schema",
                  "type" : "object",
                  "properties" : {
                    "animal" : {
                      "anyOf" : [ {
                        "type" : "object",
                        "properties" : {
                          "breed" : {
                            "type" : "string",
                            "description" : "breed description"
                          },
                          "name" : {
                            "type" : "string"
                          },
                          "type" : {
                            "const" : "dog"
                          }
                        },
                        "required" : [ "type" ]
                      }, {
                        "type" : "object",
                        "properties" : {
                          "color" : {
                            "type" : "string"
                          },
                          "name" : {
                            "type" : "string"
                          },
                          "type" : {
                            "const" : "cat"
                          }
                        },
                        "required" : [ "type" ]
                      }, {
                        "type" : "object",
                        "properties" : {
                          "type" : {
                            "const" : "uma"
                          }
                        }
                      } ]
                    }
                  }
                }
                """.trimIndent(),
            )
    }
}
