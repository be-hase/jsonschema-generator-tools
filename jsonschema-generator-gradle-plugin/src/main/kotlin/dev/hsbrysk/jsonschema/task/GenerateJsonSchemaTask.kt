package dev.hsbrysk.jsonschema.task

import com.github.victools.jsonschema.generator.Option
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion
import com.github.victools.jsonschema.module.jackson.JacksonModule
import com.github.victools.jsonschema.module.jackson.JacksonOption
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption
import com.github.victools.jsonschema.module.swagger2.Swagger2Module
import dev.hsbrysk.jsonschema.JsonSchemaGeneratorPlugin.Companion.CONFIGURATION_JSONSCHEMA_GENERATOR
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URLClassLoader
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory

abstract class GenerateJsonSchemaTask : DefaultTask() {
    @get:Input
    abstract val schemaVersion: Property<SchemaVersion>

    @get:Input
    @get:Optional
    abstract val optionPreset: Property<OptionPreset>

    @get:Input
    @get:Optional
    abstract val withOptions: SetProperty<Option>

    @get:Input
    @get:Optional
    abstract val withoutOptions: SetProperty<Option>

    @get:Input
    @get:Optional
    abstract val jacksonEnabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val jacksonOptions: SetProperty<JacksonOption>

    @get:Input
    @get:Optional
    abstract val jakartaValidationEnabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val jakartaValidationOptions: SetProperty<JakartaValidationOption>

    @get:Input
    @get:Optional
    abstract val swagger2Enabled: Property<Boolean>

    @get:Input
    abstract val schemas: MapProperty<String, String>

    @get:Classpath
    val compileClasspath: FileCollection

    @get:Classpath
    val runtimeClasspath: FileCollection

    @get:Classpath
    val pluginClasspath: FileCollection

    init {
        val javaExtension = project.extensions.getByType(JavaPluginExtension::class.java)
        val mainSourceSet = javaExtension.sourceSets.getByName("main")
        compileClasspath = mainSourceSet.compileClasspath
        runtimeClasspath = mainSourceSet.runtimeClasspath
        pluginClasspath = project.configurations.getByName(CONFIGURATION_JSONSCHEMA_GENERATOR)
    }

    @TaskAction
    fun generateJsonSchema() {
//        (mainDependencies() + pluginClasspath).forEach {
//            println("Generating $it")
//        }
        val classLoader = buildClassLoader(mainDependencies() + pluginClasspath)
        val generator = buildSchemaGenerator()

        project.layout.buildDirectory.get().file("json-schemas").asFile.mkdirs()
        schemas.get().forEach { name, target ->
            val clazz = classLoader.loadClass(target)
            val result = generator.generateSchema(clazz).toPrettyString()

            val outPath = project.layout.buildDirectory.get().file("json-schemas/$name.json").asFile
            outPath.writeText(result)
            println("Generated JSON schema: ${outPath.absolutePath}")
        }
    }

    private fun buildSchemaGenerator() = SchemaGenerator(
        SchemaGeneratorConfigBuilder(
            schemaVersion.get(),
            optionPreset.get(),
        )
            .apply {
                if (jacksonEnabled.get()) {
                    with(JacksonModule(*jacksonOptions.get().toTypedArray()))
                }
                if (jakartaValidationEnabled.get()) {
                    with(JakartaValidationModule(*jakartaValidationOptions.get().toTypedArray()))
                }
                if (swagger2Enabled.get()) {
                    with(Swagger2Module())
                }
                withOptions.get().forEach {
                    with(it)
                }
                withoutOptions.get().forEach {
                    without(it)
                }
            }
            .build(),
    )

    private fun mainDependencies(): List<File> = buildList<File> {
        (compileClasspath + runtimeClasspath).forEach { file ->
            val path = file.toPath().normalize()

            // When using the java-library plugin, the `jar` task of the dependency is not executed, so the jar file is not created.
            // To handle such cases, instead of referencing the jar,
            // replace it with references to "**/build/classes/*/main" and "**/build/resources/main".
            // Let me know if there's a better way to handle this.
            if (isBuiltJar(path)) {
                addAll(findClassesAndResourcesFromBuildJar(path))
            } else {
                add(file)
            }
        }
    }

    // We will refer to the jar built within a Gradle project as `builtJar`.
    private val builtJarMatcher = FileSystems.getDefault().getPathMatcher("glob:**/build/libs/*.jar")

    fun isBuiltJar(path: Path): Boolean = builtJarMatcher.matches(path)

    fun findClassesAndResourcesFromBuildJar(path: Path): List<File> {
        // **/build/libs/*.jar -> **/build
        val buildPath = path.parent.parent
        return buildList<File> {
            val classesPath = buildPath.resolve("classes") // **/build/classes
            if (classesPath.isDirectory()) {
                Files.list(classesPath)
                    .filter { it.isDirectory() }
                    .map { it.resolve("main") } // **/build/classes/*/main
                    .filter { it.isDirectory() }
                    .forEach {
                        add(it.toFile())
                    }
            }

            val resourcesMainPath = buildPath.resolve("resources").resolve("main") // **/build/resources/main
            if (resourcesMainPath.isDirectory()) {
                add(resourcesMainPath.toFile())
            }
        }
    }

    private fun buildClassLoader(files: List<File>) = URLClassLoader(
        files.map { it.toURI().toURL() }.toTypedArray(),
        Thread.currentThread().getContextClassLoader(),
    )

    companion object {
        const val TASK_NAME = "generateJsonSchema"
    }
}
