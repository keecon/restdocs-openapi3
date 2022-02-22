val jacksonVersion: String by extra
val swaggerVersion: String by extra
val swaggerParserVersion: String by extra
val assertjVersion: String by extra
val jsonpathVersion: String by extra
val junitVersion: String by extra

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation(project(":restdocs-api-spec-model"))
    implementation(project(":restdocs-api-spec-jsonschema"))

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("io.swagger.core.v3:swagger-core:$swaggerVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("com.jayway.jsonpath:json-path:$jsonpathVersion")
    testImplementation("io.swagger.parser.v3:swagger-parser:$swaggerParserVersion")
}
