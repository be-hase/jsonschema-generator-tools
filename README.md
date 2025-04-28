# jsonschema-generator-tools

Gradle plugin and others for [victools/jsonschema-generator](https://github.com/victools/jsonschema-generator).

By using this Gradle plugin, you can generate a JSON Schema from a Java class.

## Getting Start

For example, if you want to generate a JSON Schema from the following Java class.

```java
package com.example;

public record Person(String name, int age, String gender) {}
```

Configure the Gradle plugin as follows.

```kotlin
import com.github.victools.jsonschema.generator.SchemaVersion

plugins {
    // ...
    id("dev.hsbrysk.jsonschema-generator")
}

jsonSchemaGenerator {
    schemaVersion = SchemaVersion.DRAFT_2020_12
    schemas {
        create("Person") {
            target = "com.example.Person"
        }
    }
}
```

When you run `./gradlew generateJsonSchema`,
the following JSON Schema will be generated at `build/json-schemas/Person.json`.

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "age": {
      "type": "integer"
    },
    "gender": {
      "type": "string"
    },
    "name": {
      "type": "string"
    }
  }
}
```

## Configuration

```kotlin
jsonSchemaGenerator {
    // [Required]
    // Designated JSON Schema version.
    // See https://github.com/victools/jsonschema-generator/blob/3d8c8ff1af451b6465be76a562956f5ec30ed2c4/jsonschema-generator/src/main/java/com/github/victools/jsonschema/generator/SchemaVersion.java
    schemaVersion = com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_2020_12

    // Default settings for standard Option value
    // See https://github.com/victools/jsonschema-generator/blob/3d8c8ff1af451b6465be76a562956f5ec30ed2c4/jsonschema-generator/src/main/java/com/github/victools/jsonschema/generator/OptionPreset.java
    // Default: OptionPreset.PLAIN_JSON
    optionPreset = com.github.victools.jsonschema.generator.OptionPreset.PLAIN_JSON

    // See https://victools.github.io/jsonschema-generator/#generator-options
    options {
        // Enable options for the schema generation.
        // Default: empty
        with = setOf(...)
        // Disable options for the schema generation.
        // Default: empty
        without = setOf(...)
    }

    modules {
        // Whether to enable the Jackson module or not
        // See https://victools.github.io/jsonschema-generator/#jackson-module
        // Default: false
        jacksonEnabled = false
        // Options to enable
        // See https://github.com/victools/jsonschema-generator/blob/3d8c8ff1af451b6465be76a562956f5ec30ed2c4/jsonschema-module-jackson/src/main/java/com/github/victools/jsonschema/module/jackson/JacksonOption.java
        // Default: empty
        jacksonOptions = setOf(...)

        // Whether to enable the Jakarta validation module or not
        // See https://victools.github.io/jsonschema-generator/#jakarta-validation-module
        // Default: false
        jakartaValidationEnabled = false
        // Options to enable
        // See https://github.com/victools/jsonschema-generator/blob/3d8c8ff1af451b6465be76a562956f5ec30ed2c4/jsonschema-module-jakarta-validation/src/main/java/com/github/victools/jsonschema/module/jakarta/validation/JakartaValidationOption.java
        // Default: empty
        jakartaValidationOptions = false

        // Whether to enable the Swagger2 module or not
        // See https://victools.github.io/jsonschema-generator/#swagger-2-module
        // Default: false
        swagger2Enabled = false
    }

    // Use this when you want to treat a specific type as a different type.
    // It is useful when you are using something like a value object.
    // Default: empty
    typeMappings = mapOf(
        "com.example.SecretString" to "java.lang.Integer",
    )

    schemas {
        // The specified string will be used to generate the schema at `build/json-schemas/{...}.json`.
        create("Hoge") {
            // Specify the fully qualified name (FQDN) of the class for which
            // you want to generate the JSON Schema as a string.
            target = "com.example.Hoge"
        }
        create("Bar") {
            target = "com.example.Bar"
        }
    }

    // Configure this setting if you want to upload the generated JSON Schema to S3.
    s3 {
        // Value used for authentication.
        // If nothing is specified, the environment variables AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY will be used.
        accessKeyId = "..."
        secretAccessKey = "..."

        region = Region.blahblah

        endpoint = "..."

        bucket = "..."

        // If omitted, it will default to the root directory of the bucket.
        dir = "..."

        acl = ObjectCannedACL.blahblah

        requestChecksumCalculation = RequestChecksumCalculation.blahblah
        responseChecksumValidation = ResponseChecksumValidation.blahblah
    }

    // If you are developing a ModuleProvider using `dev.hsbrysk.jsonschema:jsonschema-module-provider`,
    // use this as needed.
    customConfigs = mapOf(...)
}
```

## Tasks

- `generateJsonSchema`
    - Generates JSON Schemas
- `uploadJsonSchemaToS3`
    - Upload JSON Schemas to S3

## Advanced and flexible configuration using ServiceLoader

By using `dev.hsbrysk.jsonschema:jsonschema-module-provider`, you can apply advanced and flexible
configurations.

For example, suppose you are using the following value object. You want this class to be treated as a string in
the JSON Schema.

```java
public record TargetPojo(String loginId, SecretString password) {}

public record SecretString(String value) {}
```

Add `dev.hsbrysk.jsonschema:jsonschema-module-provider` as a dependency and create a class like the following.

```java
public class SecretStringModule implements Module {
    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields().withTargetTypeOverridesResolver(field -> {
            if (SecretString.class.isAssignableFrom(field.getRawMember().getType())) {
                final var resolvedStringType = field.getContext().resolve(String.class);
                return Collections.singletonList(resolvedStringType);
            } else {
                return null;
            }
        });
    }
}
```

```java
package com.example;
// ...

public class SecretStringModuleProvider implements ModuleProvider {
    // An arbitrary map that can be passed in the Gradle DSL.
    // By using these, you can generate a module with configurable settings.
    @Override
    public Module provide(Map<String, String> customConfigs) {
        return new SecretStringModule();
    }
}
```

Then, place a file like the following in
`main/resources/META-INF/services/dev.hsbrysk.jsonschema.ModuleProvider`.
(If you're familiar with ServiceLoader, no further explanation is needed.)

```text
com.example.SecretStringModuleProvider
```

If you don't want to include such modules in the compile/runtimeClasspath, you can use the `jsonschemaGenerator`
configuration instead.

```kotlin
plugins {
    // ...
    id("dev.hsbrysk.jsonschema-generator")
}
dependencies {
    jsonschemaGenerator(project("YOUR_PROJECT"))
}
jsonSchemaGenerator {
    // ...
}
```
