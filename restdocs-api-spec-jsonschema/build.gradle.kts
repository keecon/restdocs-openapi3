plugins {
    java
    kotlin("jvm")
    signing
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation(project(":restdocs-api-spec-model"))

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.github.erosb:everit-json-schema:1.11.0")

    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("com.jayway.jsonpath:json-path")
    testImplementation("org.assertj:assertj-core")
    testImplementation("javax.validation:validation-api")
    testImplementation("com.github.java-json-tools:json-schema-validator:2.2.14")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("REST Docs OpenAPI 3 Spec - JSON Schema")
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
