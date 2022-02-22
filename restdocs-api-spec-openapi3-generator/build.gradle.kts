plugins {
    java
    kotlin("jvm")
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
