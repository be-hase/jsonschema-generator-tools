package dev.hsbrysk.jsonschema.task

import com.github.victools.jsonschema.generator.*
import com.github.victools.jsonschema.module.jackson.JacksonModule
import com.github.victools.jsonschema.module.jackson.JacksonOption
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption
import com.github.victools.jsonschema.module.swagger2.Swagger2Module
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
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
    abstract val optionPreset: Property<OptionPreset>

    @get:Input
    abstract val withOptions: SetProperty<Option>

    @get:Input
    abstract val withoutOptions: SetProperty<Option>

    @get:Input
    abstract val jacksonEnabled: Property<Boolean>

    @get:Input
    abstract val jacksonOptions: SetProperty<JacksonOption>

    @get:Input
    abstract val jakartaValidationEnabled: Property<Boolean>

    @get:Input
    abstract val jakartaValidationOptions: SetProperty<JakartaValidationOption>

    @get:Input
    abstract val swagger2Enabled: Property<Boolean>

    @get:Classpath
    abstract val pluginClasspath: ConfigurableFileCollection

    @get:Input
    abstract val schemas: MapProperty<String, String>

    @TaskAction
    fun generateJsonSchema() {
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

    private fun mainDependencies(): List<File> {
        val javaExtension = project.extensions.getByType(JavaPluginExtension::class.java)
        val mainSourceSet = javaExtension.sourceSets.getByName("main")
        return buildList<File> {
            (mainSourceSet.compileClasspath + mainSourceSet.runtimeClasspath).forEach { file ->
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
