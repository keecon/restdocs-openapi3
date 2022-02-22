plugins {
    java
    kotlin("jvm")
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation(project(":restdocs-api-spec"))

    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.restdocs:spring-restdocs-mockmvc")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-hateoas")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit-pioneer:junit-pioneer:0.3.3")
}
