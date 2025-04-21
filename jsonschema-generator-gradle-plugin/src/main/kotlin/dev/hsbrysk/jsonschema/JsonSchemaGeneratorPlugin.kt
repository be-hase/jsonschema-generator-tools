package dev.hsbrysk.jsonschema

import com.github.victools.jsonschema.generator.OptionPreset
import dev.hsbrysk.jsonschema.task.GenerateJsonSchemaTask
import dev.hsbrysk.jsonschema.task.UploadJsonSchemaToS3Task
import org.gradle.api.Plugin
import org.gradle.api.Project

class JsonSchemaGeneratorPlugin : Plugin<Project> {
    private lateinit var extension: JsonSchemaGeneratorExtension
    private lateinit var optionsExtension: OptionsExtension
    private lateinit var modulesExtension: ModulesExtension
    private lateinit var s3Extension: S3Extension

    override fun apply(project: Project) {
        extension = project.extensions.create("jsonSchemaGenerator", JsonSchemaGeneratorExtension::class.java).apply {
            optionPreset.convention(OptionPreset.PLAIN_JSON)
            customConfigs.convention(emptyMap())
        }
        optionsExtension = extension.extensions.create("options", OptionsExtension::class.java).apply {
            with.convention(emptySet())
            without.convention(emptySet())
        }
        modulesExtension = extension.extensions.create("modules", ModulesExtension::class.java).apply {
            jacksonEnabled.convention(false)
            jacksonOptions.convention(emptySet())
            jakartaValidationEnabled.convention(false)
            jakartaValidationOptions.convention(emptySet())
            swagger2Enabled.convention(false)
        }
        s3Extension = extension.extensions.create("s3", S3Extension::class.java)

        project.configurations.create(CONFIGURATION_JSONSCHEMA_GENERATOR) { configuration ->
            configuration.isVisible = false
            configuration.isTransitive = true
            configuration.description = "The jsonschemaGenerator dependencies to be used for this project."
            configuration.isCanBeResolved = true
            configuration.isCanBeConsumed = false
        }

        val generateJsonSchemaTask = project.tasks.register(
            GenerateJsonSchemaTask.TASK_NAME,
            GenerateJsonSchemaTask::class.java,
        ) { task ->
            task.schemaVersion.set(extension.schemaVersion)
            task.optionPreset.set(extension.optionPreset)
            task.withOptions.set(optionsExtension.with)
            task.withoutOptions.set(optionsExtension.without)
            task.jacksonEnabled.set(modulesExtension.jacksonEnabled)
            task.jacksonOptions.convention(modulesExtension.jacksonOptions)
            task.jakartaValidationEnabled.set(modulesExtension.jakartaValidationEnabled)
            task.jakartaValidationOptions.set(modulesExtension.jakartaValidationOptions)
            task.swagger2Enabled.set(modulesExtension.swagger2Enabled)
            task.schemas.set(project.provider { extension.schemas.associate { it.name to it.target.get() } })
            task.customConfigs.set(extension.customConfigs)

            task.dependsOn("classes")
        }

        project.tasks.register(
            UploadJsonSchemaToS3Task.TASK_NAME,
            UploadJsonSchemaToS3Task::class.java,
        ) { task ->
            task.accessKeyId.set(s3Extension.accessKeyId)
            task.secretAccessKey.set(s3Extension.secretAccessKey)
            task.region.set(s3Extension.region)
            task.endpoint.set(s3Extension.endpoint)
            task.bucket.set(s3Extension.bucket)
            task.dir.set(s3Extension.dir)
            task.acl.set(s3Extension.acl)
            task.requestChecksumCalculation.set(s3Extension.requestChecksumCalculation)
            task.responseChecksumValidation.set(s3Extension.responseChecksumValidation)

            task.dependsOn(generateJsonSchemaTask)
        }
    }

    companion object {
        internal const val CONFIGURATION_JSONSCHEMA_GENERATOR = "jsonschemaGenerator"
    }
}
