val jacksonVersion: String by extra
val springBootVersion: String by extra
val swaggerVersion: String by extra
val swaggerParserVersion: String by extra
val assertjVersion: String by extra
val jsonpathVersion: String by extra
val junitVersion: String by extra
val hibernateValidatorVersion: String by extra

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    api(project(":restdocs-api-spec-model"))
    api(project(":restdocs-api-spec-jsonschema"))

    api("io.swagger.core.v3:swagger-core:$swaggerVersion")
    implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("com.jayway.jsonpath:json-path:$jsonpathVersion")
    testImplementation("io.swagger.parser.v3:swagger-parser:$swaggerParserVersion")
    testImplementation("org.hibernate.validator:hibernate-validator:$hibernateValidatorVersion")
}
