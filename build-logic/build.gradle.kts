plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradle.plugin.detekt)
    implementation(libs.gradle.plugin.dokka)
    implementation(libs.gradle.plugin.kotlin)
    implementation(libs.gradle.plugin.ktlint)
    implementation(libs.gradle.plugin.maven.publish)
    implementation(libs.gradle.plugin.plugin.publish)

    // ref: https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
