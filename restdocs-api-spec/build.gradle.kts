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
    testImplementation("org.junit-pioneer:junit-pioneer:0.3.3")
    testImplementation("org.springframework.boot:spring-boot-starter-hateoas")
    testImplementation("org.hibernate.validator:hibernate-validator")
    testImplementation("org.assertj:assertj-core")
    testImplementation("com.jayway.jsonpath:json-path")
    testImplementation("com.github.java-json-tools:json-schema-validator:2.2.14")
    testImplementation("com.github.erosb:everit-json-schema:1.11.0")
}
