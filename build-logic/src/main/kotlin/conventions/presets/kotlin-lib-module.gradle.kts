package conventions.presets

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

plugins {
    id("conventions.java")
    id("conventions.kotlin")
    id("conventions.test")
    id("conventions.versioning")
    id("conventions.maven-publish")
    id("org.jetbrains.dokka")
}

mavenPublishing {
    configure(
        KotlinJvm(javadocJar = JavadocJar.Dokka("dokkaGenerateHtml"), sourcesJar = true),
    )
}
