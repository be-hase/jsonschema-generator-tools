package dev.hsbrysk.jsonschema.module.kotlin

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.Test

class KotlinModuleProviderTest {
    private val provider = KotlinModuleProvider()

    @Test
    fun `no config`() {
        val module = provider.provide(emptyMap()) as KotlinModule
        assertThat(module.options.toList()).isEmpty()
    }

    @Test
    fun `single option`() {
        val module = provider.provide(mapOf("kotlin.options" to "USE_NULLABLE")) as KotlinModule
        assertThat(module.options.toList()).isEqualTo(listOf(KotlinOption.USE_NULLABLE))
    }

    @Test
    fun `multiple options with whitespace`() {
        val module = provider.provide(
            mapOf("kotlin.options" to "USE_NULLABLE, USE_REQUIRED_VIA_DEFAULT_ARGS"),
        ) as KotlinModule
        assertThat(module.options.toList())
            .isEqualTo(listOf(KotlinOption.USE_NULLABLE, KotlinOption.USE_REQUIRED_VIA_DEFAULT_ARGS))
    }

    @Test
    fun `unknown option`() {
        assertFailure {
            provider.provide(mapOf("kotlin.options" to "USE_NULLABL"))
        }.isInstanceOf(IllegalArgumentException::class)
    }
}
