import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.6.10" apply false
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    jacoco
    `maven-publish`
}

repositories {
    google()
    mavenCentral()
}

allprojects {

    group = "com.keecon"
    version = "0.16.0"

    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "jacoco")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    repositories {
        google()
        mavenCentral()
    }
}

fun Project.isPluginProject() = this.name.contains("plugin")

subprojects {

    val jacksonVersion by extra { "2.13.1" }
    val springBootVersion by extra { "2.6.3" }
    val springRestDocsVersion by extra { "2.0.6.RELEASE" }
    val junitVersion by extra { "5.8.2" }
    val jsonpathVersion by extra { "2.6.0" }
    val swaggerVersion by extra { "2.1.13" }
    val swaggerParserVersion by extra { "2.0.30" }
    val assertjVersion by extra { "3.21.0" }

    if (!isPluginProject()) {
        java {
            withSourcesJar()
            withJavadocJar()
        }

        publishing {
            publications {
                create<MavenPublication>(project.name) {
                    from(components["java"])

                    // groupId = project.group.toString()
                    // artifactId = project.name
                    // version = project.version.toString()
                }
            }
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "11"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    tasks.withType<JacocoReport> {
        dependsOn("test")
        reports {
            html.required.set(true)
            xml.required.set(true)
        }
    }
}
