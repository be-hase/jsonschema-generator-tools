package dev.hsbrysk.jsonschema.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.net.URI

@DisableCachingByDefault(because = "Uploads files to S3 and produces no cacheable outputs")
abstract class UploadJsonSchemaToS3Task : DefaultTask() {
    @get:Input
    @get:Optional
    abstract val accessKeyId: Property<String>

    @get:Input
    @get:Optional
    abstract val secretAccessKey: Property<String>

    @get:Input
    @get:Optional
    abstract val region: Property<Region>

    @get:Input
    @get:Optional
    abstract val endpoint: Property<String>

    @get:Input
    abstract val bucket: Property<String>

    @get:Input
    @get:Optional
    abstract val dir: Property<String>

    @get:Input
    @get:Optional
    abstract val acl: Property<ObjectCannedACL>

    @get:Input
    @get:Optional
    abstract val requestChecksumCalculation: Property<RequestChecksumCalculation>

    @get:Input
    @get:Optional
    abstract val responseChecksumValidation: Property<ResponseChecksumValidation>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val schemaFiles: ConfigurableFileCollection

    @TaskAction
    fun uploadJsonSchemaToS3() {
        val bucket = bucket.get()
        val dir = dir.orNull?.removeSuffix("/")

        buildS3Client().use { s3Client ->
            schemaFiles.forEach { file ->
                s3Client.putObject(
                    PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(if (dir == null) file.name else "$dir/${file.name}")
                        .apply {
                            if (acl.isPresent) {
                                acl(acl.get())
                            }
                        }
                        .build(),
                    RequestBody.fromFile(file),
                )
            }
        }
    }

    private fun buildS3Client() = S3Client
        .builder()
        .apply {
            if (region.isPresent) {
                region(region.get())
            }
            if (accessKeyId.isPresent && secretAccessKey.isPresent) {
                credentialsProvider {
                    AwsBasicCredentials.create(accessKeyId.get(), secretAccessKey.get())
                }
            }
            if (endpoint.isPresent) {
                endpointOverride(URI(endpoint.get()))
            }
            if (requestChecksumCalculation.isPresent) {
                requestChecksumCalculation(requestChecksumCalculation.get())
            }
            if (responseChecksumValidation.isPresent) {
                responseChecksumValidation(responseChecksumValidation.get())
            }
        }
        .build()

    companion object {
        const val TASK_NAME = "uploadJsonSchemaToS3"
    }
}
