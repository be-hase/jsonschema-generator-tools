package dev.hsbrysk.jsonschema.task

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
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
import dev.hsbrysk.jsonschema.ModuleProvider
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
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.net.URLClassLoader
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.ServiceLoader
import kotlin.io.path.isDirectory

@DisableCachingByDefault(because = "Schema generation is fast enough that caching is not worthwhile")
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
    @get:Optional
    abstract val schemaPropertyEnabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val schemaPropertyRequired: Property<Boolean>

    @get:Input
    abstract val typeMappings: MapProperty<String, String>

    @get:Input
    abstract val schemas: MapProperty<String, String>

    @get:Input
    @get:Optional
    abstract val customConfigs: MapProperty<String, String>

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
        val classLoader = buildClassLoader(mainDependencies() + pluginClasspath)
        val generator = buildSchemaGenerator(classLoader)

        project.layout.buildDirectory.get().file("json-schemas").asFile.mkdirs()
        schemas.get().forEach { name, target ->
            val clazz = classLoader.loadClass(target)
            val schema = generator.generateSchema(clazz)
            if (schemaPropertyEnabled.get()) {
                injectSchemaProperty(schema)
            }

            val outPath = project.layout.buildDirectory.get().file("json-schemas/$name.json").asFile
            outPath.writeText(schema.toPrettyString())
            println("Generated JSON schema: ${outPath.absolutePath}")
        }
    }

    // Allows instance documents to declare which schema they conform to via the `$schema` key.
    // See https://github.com/be-hase/jsonschema-generator-tools/issues/122
    private fun injectSchemaProperty(schema: ObjectNode) {
        val target = resolveLocalRef(schema)
        val properties = target.withObject("/properties")
        if (!properties.has(SCHEMA_PROPERTY_NAME)) {
            properties.putObject(SCHEMA_PROPERTY_NAME).put("type", "string")
        }
        if (schemaPropertyRequired.get()) {
            val required = target.withArray("/required")
            if (required.none { it.asText() == SCHEMA_PROPERTY_NAME }) {
                required.add(SCHEMA_PROPERTY_NAME)
            }
        }
    }

    // With Option.DEFINITION_FOR_MAIN_SCHEMA, the root is only a local `$ref` and the actual
    // object schema lives under `definitions`/`$defs`, so follow the ref before injecting.
    // A definition referenced from anywhere else as well (e.g. a recursive model) must not be
    // modified, because `$schema` should only be allowed/required at the root; in that case the
    // definition is inlined into the root and the original is left untouched for other referrers.
    private fun resolveLocalRef(schema: ObjectNode): ObjectNode {
        var current = schema
        repeat(MAX_REF_RESOLUTION) {
            val ref = current.path(REF_KEYWORD).asText("")
            if (!ref.startsWith("#/")) {
                return current
            }
            val resolved = schema.at(ref.substring(1))
            if (resolved !is ObjectNode) {
                return current
            }
            if (countLocalRefs(schema, ref) > 1) {
                current.remove(REF_KEYWORD)
                current.setAll<ObjectNode>(resolved.deepCopy())
                return current
            }
            current = resolved
        }
        return current
    }

    private fun countLocalRefs(
        node: JsonNode,
        ref: String,
    ): Int {
        var count = if (node.path(REF_KEYWORD).asText("") == ref) 1 else 0
        node.forEach { child ->
            count += countLocalRefs(child, ref)
        }
        return count
    }

    private fun buildSchemaGenerator(classLoader: ClassLoader) = SchemaGenerator(
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

                val typeMappingsList = typeMappings.get()
                    .map { classLoader.loadClass(it.key) to classLoader.loadClass(it.value) }
                if (typeMappingsList.isNotEmpty()) {
                    forFields()
                        .withTargetTypeOverridesResolver { field ->
                            val type = field.type.erasedType
                            typeMappingsList.firstOrNull { (src, _) -> src.isAssignableFrom(type) }
                                ?.let { (_, dist) ->
                                    listOf(field.context.resolve(dist))
                                }
                        }
                }

                ServiceLoader.load(ModuleProvider::class.java, classLoader).forEach { provider ->
                    with(provider.provide(customConfigs.get()))
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
        private const val SCHEMA_PROPERTY_NAME = "\$schema"
        private const val REF_KEYWORD = "\$ref"
        private const val MAX_REF_RESOLUTION = 10
    }
}
