package dev.hsbrysk.jsonschema

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.sun.net.httpserver.HttpServer
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetSocketAddress
import java.util.Collections
import kotlin.io.path.Path

class UploadJsonSchemaToS3FunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    // A fake S3 endpoint. The AWS SDK uses path-style requests for IP endpoints,
    // so an upload arrives as `PUT /{bucket}/{key}`.
    private lateinit var server: HttpServer
    private val recordedRequests = Collections.synchronizedList(mutableListOf<RecordedRequest>())

    data class RecordedRequest(
        val method: String,
        val path: String,
        val acl: String?,
        val body: String,
    )

    @BeforeEach
    fun beforeEach() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val rawBody = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
            // Over plain HTTP the SDK signs the payload in streaming mode (aws-chunked encoding)
            val body = if (exchange.requestHeaders.getFirst("x-amz-content-sha256")?.startsWith("STREAMING-") == true) {
                decodeAwsChunked(rawBody)
            } else {
                rawBody
            }
            recordedRequests.add(
                RecordedRequest(
                    method = exchange.requestMethod,
                    path = exchange.requestURI.path,
                    acl = exchange.requestHeaders.getFirst("x-amz-acl"),
                    body = body,
                ),
            )
            exchange.responseHeaders.add("ETag", "\"dummy-etag\"")
            exchange.sendResponseHeaders(200, -1)
            exchange.close()
        }
        server.start()

        settingsFile.writeText(
            // language=kotlin
            """
            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                }
            }
            """.trimIndent(),
        )

        projectDir.resolve(Path("src", "main", "java", "com", "example").toFile()).mkdirs()
        projectDir.resolve(Path("src", "main", "java", "com", "example", "Person.java").toFile()).writeText(
            // language=java
            """
            package com.example;
            public record Person(String name, int age, String gender) {}
            """.trimIndent(),
        )
        projectDir.resolve(Path("src", "main", "java", "com", "example", "Address.java").toFile()).writeText(
            // language=java
            """
            package com.example;
            public record Address(String city, String street) {}
            """.trimIndent(),
        )
    }

    @AfterEach
    fun afterEach() {
        server.stop(0)
    }

    @Test
    fun `upload generated schemas`() {
        buildFile.writeText(
            // language=kotlin
            """
            import com.github.victools.jsonschema.generator.SchemaVersion
            import software.amazon.awssdk.core.checksums.RequestChecksumCalculation
            import software.amazon.awssdk.regions.Region
            plugins {
                java
                id("dev.hsbrysk.jsonschema-generator")
            }
            jsonschemaGenerator {
                schemaVersion = SchemaVersion.DRAFT_2020_12
                schemas {
                    create("Person") {
                        target = "com.example.Person"
                    }
                    create("Address") {
                        target = "com.example.Address"
                    }
                }
                s3 {
                    accessKeyId = "dummy-access-key"
                    secretAccessKey = "dummy-secret-key"
                    region = Region.US_EAST_1
                    endpoint = "http://127.0.0.1:${server.address.port}"
                    bucket = "test-bucket"
                    requestChecksumCalculation = RequestChecksumCalculation.WHEN_REQUIRED
                }
            }
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments(UPLOAD_TASK_NAME)
            .build()

        // Running the upload task alone must trigger compilation and schema generation first
        assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":generateJsonSchema")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":$UPLOAD_TASK_NAME")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        val puts = recordedRequests.filter { it.method == "PUT" }.sortedBy { it.path }
        assertThat(puts.map { it.path }).isEqualTo(listOf("/test-bucket/Address.json", "/test-bucket/Person.json"))
        assertThat(puts[0].acl).isNull()
        assertThat(puts[0].body)
            .isEqualTo(projectDir.resolve(Path("build", "json-schemas", "Address.json").toFile()).readText())
        assertThat(puts[1].body)
            .isEqualTo(projectDir.resolve(Path("build", "json-schemas", "Person.json").toFile()).readText())
    }

    @Test
    fun `upload with dir and acl`() {
        buildFile.writeText(
            // language=kotlin
            """
            import com.github.victools.jsonschema.generator.SchemaVersion
            import software.amazon.awssdk.core.checksums.RequestChecksumCalculation
            import software.amazon.awssdk.regions.Region
            import software.amazon.awssdk.services.s3.model.ObjectCannedACL
            plugins {
                java
                id("dev.hsbrysk.jsonschema-generator")
            }
            jsonschemaGenerator {
                schemaVersion = SchemaVersion.DRAFT_2020_12
                schemas {
                    create("Person") {
                        target = "com.example.Person"
                    }
                }
                s3 {
                    accessKeyId = "dummy-access-key"
                    secretAccessKey = "dummy-secret-key"
                    region = Region.US_EAST_1
                    endpoint = "http://127.0.0.1:${server.address.port}"
                    bucket = "test-bucket"
                    // The trailing slash should be trimmed when building the object key
                    dir = "schemas/v1/"
                    acl = ObjectCannedACL.PUBLIC_READ
                    requestChecksumCalculation = RequestChecksumCalculation.WHEN_REQUIRED
                }
            }
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments(UPLOAD_TASK_NAME)
            .build()

        assertThat(result.task(":$UPLOAD_TASK_NAME")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        val puts = recordedRequests.filter { it.method == "PUT" && it.path.endsWith("Person.json") }
        assertThat(puts.map { it.path }).isEqualTo(listOf("/test-bucket/schemas/v1/Person.json"))
        assertThat(puts[0].acl).isEqualTo("public-read")
    }

    companion object {
        private const val UPLOAD_TASK_NAME = "uploadJsonSchemaToS3"

        // Each chunk looks like "SIZE_IN_HEX;chunk-signature=...\r\n<data>\r\n", terminated by a zero-size chunk.
        // Offset-based slicing is fine here because the schemas under test are ASCII-only.
        private fun decodeAwsChunked(raw: String): String {
            val decoded = StringBuilder()
            var index = 0
            var headerEnd = raw.indexOf("\r\n", index)
            while (headerEnd != -1) {
                val size = raw.substring(index, headerEnd).substringBefore(';').toInt(16)
                if (size == 0) break
                val dataStart = headerEnd + 2
                decoded.append(raw, dataStart, dataStart + size)
                index = dataStart + size + 2
                headerEnd = raw.indexOf("\r\n", index)
            }
            return decoded.toString()
        }
    }
}
