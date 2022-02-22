plugins {
    java
    kotlin("jvm")
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation(project(":restdocs-api-spec"))

    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.restdocs:spring-restdocs-mockmvc")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-hateoas")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit-pioneer:junit-pioneer:0.3.3")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("REST Docs OpenAPI 3 Spec - MockMVC")
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

java {
    withJavadocJar()
    withSourcesJar()
}
