val jacksonVersion: String by extra
val assertjVersion: String by extra
val jsonpathVersion: String by extra
val junitVersion: String by extra
val hibernateValidatorVersion: String by extra
val jsonSchemaValidatorVersion: String by extra

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation(project(":restdocs-api-spec-model"))

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.github.erosb:everit-json-schema:1.11.0")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("com.jayway.jsonpath:json-path:$jsonpathVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("org.hibernate.validator:hibernate-validator:$hibernateValidatorVersion")
    testImplementation("com.github.java-json-tools:json-schema-validator:$jsonSchemaValidatorVersion")
}
