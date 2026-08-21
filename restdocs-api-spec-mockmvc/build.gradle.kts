dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    api(project(":restdocs-api-spec"))

    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.restdocs.mockmvc)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.restdocs)
    testImplementation(libs.spring.boot.starter.hateoas)
    testImplementation(libs.bundles.junit)
}
