package conventions

plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(
        project.group.toString(),
        project.name,
        project.version.toString(),
    )
    afterEvaluate {
        pom {
            name = project.name
            description = project.description
            url = "https://github.com/be-hase/jsonschema-generator-tools"
            licenses {
                license {
                    name = "MIT License"
                    url = "https://opensource.org/license/mit"
                }
            }
            developers {
                developer {
                    id = "be-hase"
                    name = "Ryosuke Hasebe"
                    email = "hsb.1014@gmail.com"
                }
            }
            scm {
                connection.set("scm:git:git://github.com/be-hase/jsonschema-generator-tools.git")
                developerConnection.set("scm:git:ssh://github.com:be-hase/jsonschema-generator-tools.git")
                url.set("https://github.com/be-hase/jsonschema-generator-tools")
            }
        }
    }
}
