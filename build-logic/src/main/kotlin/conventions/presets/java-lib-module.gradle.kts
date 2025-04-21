package conventions.presets

import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    id("conventions.java")
    id("conventions.maven-publish")
    id("conventions.test")
    id("conventions.versioning")
}

mavenPublishing {
    configure(
        JavaLibrary(JavadocJar.Javadoc(), sourcesJar = true),
    )
}
