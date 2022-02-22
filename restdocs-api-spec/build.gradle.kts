plugins {
    java
    kotlin("jvm")
}

val jacksonVersion: String by extra
val springBootVersion: String by extra
val springRestDocsVersion: String by extra
val assertjVersion: String by extra
val jsonpathVersion: String by extra
val junitVersion: String by extra

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(kotlin("reflect"))

    implementation("org.springframework.boot:spring-boot-starter-web:$springRestDocsVersion")
    implementation("org.springframework.restdocs:spring-restdocs-core:$springRestDocsVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test:$springRestDocsVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit-pioneer:junit-pioneer:0.3.3")
    testImplementation("org.springframework.boot:spring-boot-starter-hateoas:$springRestDocsVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("com.jayway.jsonpath:json-path:$jsonpathVersion")
    testImplementation("org.hibernate.validator:hibernate-validator:6.2.0.Final")
    testImplementation("com.github.java-json-tools:json-schema-validator:2.2.14")
    testImplementation("com.github.erosb:everit-json-schema:1.11.0")
}
