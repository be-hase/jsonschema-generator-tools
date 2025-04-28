package dev.hsbrysk.jsonschema

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import com.github.victools.jsonschema.generator.OptionPreset
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class JsonSchemaGeneratorPluginTest {
    @Test
    fun `plugin registers task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")
        project.plugins.apply("dev.hsbrysk.jsonschema-generator")

        // check task
        assertThat(project.tasks.findByName("generateJsonSchema")).isNotNull()
        assertThat(project.tasks.findByName("uploadJsonSchemaToS3")).isNotNull()

        // check default property
        val extension = project.extensions.getByName("jsonschemaGenerator") as JsonSchemaGeneratorExtension
        assertThat(extension.optionPreset.get()).isEqualTo(OptionPreset.PLAIN_JSON)
        assertThat(extension.customConfigs.get()).isEmpty()
        val optionsExtension = extension.extensions.getByName("options") as OptionsExtension
        assertThat(optionsExtension.with.get()).isEmpty()
        assertThat(optionsExtension.without.get()).isEmpty()
        val modulesExtension = extension.extensions.getByName("modules") as ModulesExtension
        assertThat(modulesExtension.jacksonEnabled.get()).isFalse()
        assertThat(modulesExtension.jacksonOptions.get()).isEmpty()
        assertThat(modulesExtension.jakartaValidationEnabled.get()).isFalse()
        assertThat(modulesExtension.jakartaValidationOptions.get()).isEmpty()
        assertThat(modulesExtension.swagger2Enabled.get()).isFalse()
    }
}
