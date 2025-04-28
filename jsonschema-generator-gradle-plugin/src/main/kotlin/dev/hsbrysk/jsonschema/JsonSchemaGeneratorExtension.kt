package dev.hsbrysk.jsonschema

import com.github.victools.jsonschema.generator.Option
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaVersion
import com.github.victools.jsonschema.module.jackson.JacksonOption
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class JsonSchemaGeneratorExtension @Inject constructor(project: Project) : ExtensionAware {
    abstract val schemaVersion: Property<SchemaVersion>

    abstract val optionPreset: Property<OptionPreset>

    val options: OptionsExtension = project.objects.newInstance(OptionsExtension::class.java)

    val modules: ModulesExtension = project.objects.newInstance(ModulesExtension::class.java)

    val typeMappings: MapProperty<String, String> = project.objects.mapProperty(String::class.java, String::class.java)

    val schemas: NamedDomainObjectContainer<SchemaExtension> = project.container(SchemaExtension::class.java)

    fun schemas(action: Action<NamedDomainObjectContainer<SchemaExtension>>) {
        action.execute(schemas)
    }

    val s3: S3Extension = project.objects.newInstance(S3Extension::class.java)

    abstract val customConfigs: MapProperty<String, String>
}

interface SchemaExtension : Named {
    val target: Property<String>
}

interface OptionsExtension {
    val with: SetProperty<Option>
    val without: SetProperty<Option>
}

interface ModulesExtension {
    val jacksonEnabled: Property<Boolean>
    val jacksonOptions: SetProperty<JacksonOption>
    val jakartaValidationEnabled: Property<Boolean>
    val jakartaValidationOptions: SetProperty<JakartaValidationOption>
    val swagger2Enabled: Property<Boolean>
}

interface S3Extension {
    val accessKeyId: Property<String>
    val secretAccessKey: Property<String>
    val region: Property<Region>
    val endpoint: Property<String>
    val bucket: Property<String>
    val dir: Property<String>
    val acl: Property<ObjectCannedACL>
    val requestChecksumCalculation: Property<RequestChecksumCalculation>
    val responseChecksumValidation: Property<ResponseChecksumValidation>
}
