package dev.hsbrysk.jsonschema.module.kotlin

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion
import org.junit.jupiter.api.Test

class KotlinModuleTest {
    @Test
    fun useNullable() {
        val generator = SchemaGenerator(
            SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(KotlinModule(KotlinOption.USE_NULLABLE))
                .build(),
        )
        assertThat(generator.generateSchema(Person::class.java).toPrettyString()).isEqualTo(
            """
            {
              "${'$'}schema" : "https://json-schema.org/draft/2020-12/schema",
              "type" : "object",
              "properties" : {
                "age" : {
                  "type" : "integer"
                },
                "gender" : {
                  "type" : [ "string", "null" ]
                },
                "name" : {
                  "type" : "string"
                }
              }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun useRequiredViaDefaultArgs() {
        val generator = SchemaGenerator(
            SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(KotlinModule(KotlinOption.USE_REQUIRED_VIA_DEFAULT_ARGS))
                .build(),
        )
        assertThat(generator.generateSchema(Person::class.java).toPrettyString()).isEqualTo(
            """
            {
              "${'$'}schema" : "https://json-schema.org/draft/2020-12/schema",
              "type" : "object",
              "properties" : {
                "age" : {
                  "type" : "integer"
                },
                "gender" : {
                  "type" : "string"
                },
                "name" : {
                  "type" : "string"
                }
              },
              "required" : [ "age", "gender" ]
            }
            """.trimIndent(),
        )
    }

    @Test
    fun useRequiredViaDefaultArgsWithBodyProperty() {
        val generator = SchemaGenerator(
            SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(KotlinModule(KotlinOption.USE_REQUIRED_VIA_DEFAULT_ARGS))
                .build(),
        )
        // A property declared in the class body is not a constructor parameter, so it must not be required
        assertThat(generator.generateSchema(WithBodyProperty::class.java).toPrettyString()).isEqualTo(
            """
            {
              "${'$'}schema" : "https://json-schema.org/draft/2020-12/schema",
              "type" : "object",
              "properties" : {
                "age" : {
                  "type" : "integer"
                },
                "name" : {
                  "type" : "string"
                },
                "nickname" : {
                  "type" : "string"
                }
              },
              "required" : [ "age" ]
            }
            """.trimIndent(),
        )
    }

    @Test
    fun javaClass() {
        val generator = SchemaGenerator(
            SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(KotlinModule(KotlinOption.USE_NULLABLE, KotlinOption.USE_REQUIRED_VIA_DEFAULT_ARGS))
                .build(),
        )
        // Non-Kotlin classes must not crash; nullability and required fall back to the defaults
        assertThat(generator.generateSchema(JavaPerson::class.java).toPrettyString()).isEqualTo(
            """
            {
              "${'$'}schema" : "https://json-schema.org/draft/2020-12/schema",
              "type" : "object",
              "properties" : {
                "age" : {
                  "type" : "integer"
                },
                "name" : {
                  "type" : "string"
                }
              }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun classWithoutPrimaryConstructor() {
        val generator = SchemaGenerator(
            SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(KotlinModule(KotlinOption.USE_NULLABLE, KotlinOption.USE_REQUIRED_VIA_DEFAULT_ARGS))
                .build(),
        )
        // Without a primary constructor there is no default-argument information,
        // so nothing is required; nullability still works via the property types
        assertThat(generator.generateSchema(NoPrimaryConstructor::class.java).toPrettyString()).isEqualTo(
            """
            {
              "${'$'}schema" : "https://json-schema.org/draft/2020-12/schema",
              "type" : "object",
              "properties" : {
                "age" : {
                  "type" : [ "integer", "null" ]
                },
                "name" : {
                  "type" : "string"
                }
              }
            }
            """.trimIndent(),
        )
    }
}

data class Person(val name: String = "NONAME", val age: Int, val gender: String?)

data class WithBodyProperty(val name: String = "NONAME", val age: Int) {
    val nickname: String = name
}

class NoPrimaryConstructor {
    val name: String
    val age: Int?

    constructor(name: String, age: Int?) {
        this.name = name
        this.age = age
    }
}
