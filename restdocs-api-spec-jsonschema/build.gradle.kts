val jacksonVersion: String by extra
val assertjVersion: String by extra
val jsonpathVersion: String by extra
val junitVersion: String by extra

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation(project(":restdocs-api-spec-model"))

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.github.erosb:everit-json-schema:1.11.0")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("com.jayway.jsonpath:json-path:$jsonpathVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("javax.validation:validation-api:2.0.1.Final")
    testImplementation("com.github.java-json-tools:json-schema-validator:2.2.14")
}
