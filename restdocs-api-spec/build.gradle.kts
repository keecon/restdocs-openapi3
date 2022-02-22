plugins {
    java
    kotlin("jvm")
}
dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(kotlin("reflect"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.restdocs:spring-restdocs-core")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit-pioneer:junit-pioneer:1.6.1")
    testImplementation("org.springframework.boot:spring-boot-starter-hateoas")
    testImplementation("org.hibernate.validator:hibernate-validator")
    testImplementation("org.assertj:assertj-core")
    testImplementation("com.jayway.jsonpath:json-path")
    testImplementation("com.github.java-json-tools:json-schema-validator:2.2.14")
    testImplementation("com.github.erosb:everit-json-schema:1.11.0")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("REST Docs OpenAPI 3 Spec")
                description.set("Adds API specification support to Spring REST Docs")
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

java {
    withJavadocJar()
    withSourcesJar()
}
