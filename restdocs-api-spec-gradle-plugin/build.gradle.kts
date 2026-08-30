@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        register("com.keecon.restdocs-openapi3") {
            id = "com.keecon.restdocs-openapi3"
            implementationClass = "com.keecon.restdocs.apispec.gradle.RestdocsOpenApi3Plugin"
        }
    }
}

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(kotlin("gradle-plugin"))

    implementation(project(":restdocs-api-spec-model"))
    implementation(project(":restdocs-api-spec-generator"))

    implementation(libs.swagger.core)
    implementation(libs.bundles.jackson3)
    implementation(libs.jackson3.dataformat.yaml)

    testImplementation(libs.assertj.core)
    testImplementation(libs.jsonpath)
    testImplementation(libs.bundles.junit)

    testCompileOnly(gradleTestKit())
}
