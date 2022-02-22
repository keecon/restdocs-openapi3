plugins {
    java
    kotlin("jvm")
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation("com.fasterxml.jackson.core:jackson-annotations")
}
