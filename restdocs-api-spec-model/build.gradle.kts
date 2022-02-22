plugins {
    java
    kotlin("jvm")
}

val jacksonVersion: String by extra

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
}
