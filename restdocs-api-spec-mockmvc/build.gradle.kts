dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    api(project(":restdocs-api-spec"))
    api(libs.spring.restdocs.mockmvc)

    implementation(libs.spring.boot.starter.validation)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.restdocs)
    testImplementation(libs.spring.boot.starter.hateoas)
    testImplementation(libs.bundles.junit)
}
