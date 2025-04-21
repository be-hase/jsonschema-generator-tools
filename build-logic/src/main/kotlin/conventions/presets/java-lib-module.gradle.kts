package conventions.presets

import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    id("conventions.java")
    id("conventions.test")
    id("conventions.versioning")
    id("conventions.maven-publish")
    `java-library`
}

mavenPublishing {
    configure(
        JavaLibrary(javadocJar = JavadocJar.Javadoc(), sourcesJar = true),
    )
}
