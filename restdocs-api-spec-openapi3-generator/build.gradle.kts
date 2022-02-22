plugins {
    java
    kotlin("jvm")
    signing
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation(project(":restdocs-api-spec-model"))
    implementation(project(":restdocs-api-spec-jsonschema"))

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.swagger.core.v3:swagger-core:2.1.13")

    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.assertj:assertj-core")
    testImplementation("com.jayway.jsonpath:json-path")
    testImplementation("io.swagger.parser.v3:swagger-parser:2.0.30")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("REST Docs OpenAPI 3 Spec - Generator")
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
