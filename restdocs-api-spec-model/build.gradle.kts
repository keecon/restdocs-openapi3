plugins {
    java
    kotlin("jvm")
    signing
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation("com.fasterxml.jackson.core:jackson-annotations")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("REST Docs OpenAPI 3 Spec - Model")
                description.set("Adds API specification support to Spring REST Docs ")
                url.set("https://github.com/keecon/restdocs-openapi3")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/keecon/restdocs-openapi3/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("keecon")
                        name.set("keecon dev")
                        email.set("info@keecon.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/keecon/restdocs-openapi3.git")
                    developerConnection.set("scm:git:ssh://github.com/keecon/restdocs-openapi3.git")
                    url.set("https://github.com/keecon/restdocs-openapi3")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

java {
    withJavadocJar()
    withSourcesJar()
}
