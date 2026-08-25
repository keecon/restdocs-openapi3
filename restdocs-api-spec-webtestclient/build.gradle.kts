dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    api(project(":restdocs-api-spec"))
    api(libs.spring.restdocs.webtestclient)

    implementation(libs.spring.webflux)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webflux.test)
    testImplementation(libs.bundles.junit)
}
