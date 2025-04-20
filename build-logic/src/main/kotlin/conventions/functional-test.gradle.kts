@file:Suppress("HasPlatformType")

package conventions

plugins {
    kotlin("jvm")
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest")

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

val functionalTestTask = tasks.register<Test>("functionalTest") {
    description = "Runs the functional tests."
    group = "verification"
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
    mustRunAfter(tasks.test)
}

tasks.check {
    dependsOn(functionalTestTask)
}
