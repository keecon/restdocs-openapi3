import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.axion.release)
    java
    jacoco
    `maven-publish`
    `jacoco-report-aggregation`
}

repositories {
    google()
    mavenCentral()
}

scmVersion {
    tag {
        prefix.set("")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val scmVer = scmVersion.version!!

fun Project.isPluginProject() = this.name.contains("plugin")
fun Project.isExampleProject() = this.name.contains("example")

allprojects {

    group = "com.keecon"
    version = scmVer

    repositories {
        google()
        mavenCentral()
    }

    if (!isExampleProject()) {
        apply(plugin = "maven-publish")
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
    }

    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "jacoco")

    tasks.test {
        useJUnitPlatform()
        finalizedBy(tasks.jacocoTestReport)

        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

subprojects {

    tasks.named("compileKotlin", KotlinCompilationTask::class.java) {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }

    if (!isPluginProject() && !isExampleProject()) {
        java {
            withSourcesJar()
            withJavadocJar()
        }

        publishing {
            publications {
                create<MavenPublication>(project.name) {
                    from(components["java"])
                }
            }
        }
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test)

        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

dependencies {
    jacocoAggregation(project(":restdocs-api-spec"))
    jacocoAggregation(project(":restdocs-api-spec-generator"))
    jacocoAggregation(project(":restdocs-api-spec-gradle-plugin"))
    jacocoAggregation(project(":restdocs-api-spec-jsonschema"))
    jacocoAggregation(project(":restdocs-api-spec-mockmvc"))
    jacocoAggregation(project(":restdocs-api-spec-model"))
}

tasks.testCodeCoverageReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
