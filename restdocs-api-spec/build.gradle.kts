dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(kotlin("reflect"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.restdocs.core)
    implementation(libs.bundles.jackson)
    implementation(libs.guava)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.hateoas)
    testImplementation(libs.assertj.core)
    testImplementation(libs.jsonpath)
    testImplementation(libs.hibernate.validator)
    testImplementation(libs.json.schema.validator)
    testImplementation(libs.everit.json.schema)
    testImplementation(libs.bundles.junit)
}
