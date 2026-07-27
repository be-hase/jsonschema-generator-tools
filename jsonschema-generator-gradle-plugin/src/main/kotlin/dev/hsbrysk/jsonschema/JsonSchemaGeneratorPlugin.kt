package dev.hsbrysk.jsonschema

import com.github.victools.jsonschema.generator.OptionPreset
import dev.hsbrysk.jsonschema.task.GenerateJsonSchemaTask
import dev.hsbrysk.jsonschema.task.UploadJsonSchemaToS3Task
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet

class JsonSchemaGeneratorPlugin : Plugin<Project> {
    private lateinit var extension: JsonSchemaGeneratorExtension
    private lateinit var optionsExtension: OptionsExtension
    private lateinit var modulesExtension: ModulesExtension
    private lateinit var schemaPropertyExtension: SchemaPropertyExtension
    private lateinit var s3Extension: S3Extension

    override fun apply(project: Project) {
        createExtensions(project)

        project.configurations.create(CONFIGURATION_JSONSCHEMA_GENERATOR) { configuration ->
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
            task.jacksonOptions.set(modulesExtension.jacksonOptions)
            task.jakartaValidationEnabled.set(modulesExtension.jakartaValidationEnabled)
            task.jakartaValidationOptions.set(modulesExtension.jakartaValidationOptions)
            task.swagger2Enabled.set(modulesExtension.swagger2Enabled)
            task.schemaPropertyEnabled.set(schemaPropertyExtension.enabled)
            task.schemaPropertyRequired.set(schemaPropertyExtension.required)
            task.typeMappings.set(extension.typeMappings)
            task.schemas.set(project.provider { extension.schemas.associate { it.name to it.target.get() } })
            task.customConfigs.set(extension.customConfigs)
            task.pluginClasspath.from(project.configurations.named(CONFIGURATION_JSONSCHEMA_GENERATOR))
            task.outputDirectory.convention(project.layout.buildDirectory.dir(OUTPUT_DIRECTORY_NAME))
        }

        // The main source set's classpaths carry their own task dependencies (e.g. `classes`),
        // so no explicit dependsOn is needed here.
        project.plugins.withType(JavaPlugin::class.java) {
            generateJsonSchemaTask.configure { task ->
                val mainSourceSet = project.extensions.getByType(JavaPluginExtension::class.java)
                    .sourceSets
                    .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                task.compileClasspath.from(mainSourceSet.compileClasspath)
                task.runtimeClasspath.from(mainSourceSet.runtimeClasspath)
            }
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

            // Wiring the generate task's output as an input also carries the task dependency.
            task.schemaFiles.from(
                generateJsonSchemaTask.flatMap { it.outputDirectory }.map { dir ->
                    dir.asFileTree.matching { pattern -> pattern.include("*.json") }
                },
            )
        }
    }

    private fun createExtensions(project: Project) {
        extension = project.extensions.create("jsonschemaGenerator", JsonSchemaGeneratorExtension::class.java).apply {
            optionPreset.convention(OptionPreset.PLAIN_JSON)
            typeMappings.convention(emptyMap())
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
        schemaPropertyExtension = extension.extensions.create("schemaProperty", SchemaPropertyExtension::class.java)
            .apply {
                enabled.convention(false)
                required.convention(true)
            }
        s3Extension = extension.extensions.create("s3", S3Extension::class.java)
    }

    companion object {
        internal const val CONFIGURATION_JSONSCHEMA_GENERATOR = "jsonschemaGenerator"
        internal const val OUTPUT_DIRECTORY_NAME = "json-schemas"
    }
}
