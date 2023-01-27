dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation(project(":restdocs-api-spec-model"))

    implementation(libs.everit.json.schema)
    implementation(libs.bundles.jackson)

    testImplementation(libs.jsonpath)
    testImplementation(libs.assertj.core)
    testImplementation(libs.hibernate.validator)
    testImplementation(libs.json.schema.validator)
    testImplementation(libs.bundles.junit)
}
