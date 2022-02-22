plugins {
    java
    kotlin("jvm")
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
