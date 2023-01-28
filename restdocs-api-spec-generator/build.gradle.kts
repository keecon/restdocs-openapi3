dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    api(project(":restdocs-api-spec-model"))
    api(project(":restdocs-api-spec-jsonschema"))

    api(libs.swagger.core)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.bundles.jackson)

    testImplementation(libs.assertj.core)
    testImplementation(libs.jsonpath)
    testImplementation(libs.swagger.parser)
    testImplementation(libs.hibernate.validator)
    testImplementation(libs.bundles.junit)
}
