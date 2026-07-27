# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```sh
# Some functional tests (e.g. KotlinModuleFunctionalTest) resolve the
# `latest-SNAPSHOT` artifacts from mavenLocal, so publish first.
./gradlew publishToMavenLocal

# Full CI check (unit tests + functional tests + ktlint + detekt)
./gradlew check detektMain detektTest detektFunctionalTest

# Unit tests only / functional tests only
./gradlew test
./gradlew :jsonschema-generator-gradle-plugin:functionalTest

# Single test class
./gradlew :jsonschema-generator-gradle-plugin:functionalTest --tests 'dev.hsbrysk.jsonschema.BasicFunctionalTest'

# Lint
./gradlew ktlintCheck        # or ktlintFormat to auto-fix
./gradlew detektMain detektTest detektFunctionalTest   # config: config/detekt/detekt.yml
```

Java toolchain is 17 (Temurin, auto-provisioned via foojay). Kotlin compiles with `allWarningsAsErrors = true`.

## Architecture

Gradle multi-project build providing tooling around [victools/jsonschema-generator](https://github.com/victools/jsonschema-generator). Everything is published under group `dev.hsbrysk.jsonschema`.

- **jsonschema-generator-gradle-plugin** — the Gradle plugin (id `dev.hsbrysk.jsonschema-generator`). `JsonSchemaGeneratorPlugin` creates the `jsonschemaGenerator` extension (with nested `options`, `modules`, `schemaProperty`, `s3` extensions) and registers two tasks:
  - `GenerateJsonSchemaTask` (`generateJsonSchema`): builds a `URLClassLoader` from the consuming project's main runtime classpath plus the plugin-defined `jsonschemaGenerator` configuration, loads target classes by FQCN, discovers `ModuleProvider` implementations via `ServiceLoader` on that classloader, and writes schemas to `build/json-schemas/{name}.json`.
  - `UploadJsonSchemaToS3Task` (`uploadJsonSchemaToS3`): uploads the generated schemas via the AWS SDK.
- **jsonschema-module-provider** — tiny Java SPI module defining the `ModuleProvider` interface (`provide(Map<String, String> customConfigs): Module`). Consumers register implementations in `META-INF/services/dev.hsbrysk.jsonschema.ModuleProvider`; the plugin passes the extension's `customConfigs` map through.
- **jsonschema-module-kotlin** — `KotlinModule` for victools (nullable detection, required-via-default-args) plus its `ModuleProvider`, configured through `customConfigs` key `kotlin.options` (comma-separated `KotlinOption` values).
- **test/** (`test-type`, `test-type-module-provider`) — fixture projects consumed by functional tests; not published.
- **build-logic/** — included build with convention plugins (`conventions.*`). `conventions.presets.*` compose them per module type (gradle-plugin / kotlin-lib / java-lib / test-module). `conventions.versioning` sets the version: `latest-SNAPSHOT` by default, overridden by `-PpublishVersion` or `PUBLISH_VERSION` env (releases are driven by `v*` git tags → Maven Central + Gradle Plugin Portal).

Other things worth knowing:

- The codebase uses Jackson 3 (`tools.jackson.*` packages), matching jsonschema-generator v5.
- Functional tests use Gradle TestKit (`GradleRunner` + `withPluginClasspath()`), generating throwaway projects in temp dirs. `Constants.kt` in the functionalTest source set pins the Kotlin/Jackson/etc. versions used inside those generated builds.
- Dependency versions live in `gradle/libs.versions.toml` (also consumed by build-logic).
