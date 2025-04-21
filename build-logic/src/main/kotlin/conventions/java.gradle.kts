package conventions

import gradle.kotlin.dsl.accessors._a878d13f580598fb4efe3d6e9a8b2361.java

plugins {
    java
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
        vendor = JvmVendorSpec.ADOPTIUM
    }
}
