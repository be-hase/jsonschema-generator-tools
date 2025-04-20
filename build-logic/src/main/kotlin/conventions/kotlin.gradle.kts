package conventions

plugins {
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        javaParameters = true
        allWarningsAsErrors = true
        freeCompilerArgs = listOf(
            "-Xjsr305=strict",
        )
    }
}
