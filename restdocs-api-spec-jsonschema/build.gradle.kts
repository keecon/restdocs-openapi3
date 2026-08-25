dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    api(project(":restdocs-api-spec-model"))
    api(libs.everit.json.schema)

    implementation(libs.bundles.jackson3)

    testImplementation(libs.jsonpath)
    testImplementation(libs.assertj.core)
    testImplementation(libs.hibernate.validator)
    testImplementation(libs.json.schema.validator)
    testImplementation(libs.bundles.junit)
}
