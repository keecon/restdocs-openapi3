import org.gradle.api.JavaVersion
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

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
    versionCreator("simple")
    tag {
        prefix.set("")
    }
}

val scmVer = scmVersion.version!!
val jacocoToolVersion = libs.versions.jacoco.get()
val junitPlatformLauncher = libs.junit.platform.launcher
val springBootBom = libs.spring.boot.dependencies

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

    dependencies {
        if (!isExampleProject()) {
            add("implementation", platform(springBootBom))
            add("testImplementation", platform(springBootBom))
        }
        add("testRuntimeOnly", junitPlatformLauncher)
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    jacoco {
        toolVersion = jacocoToolVersion
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(17)
    }

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(JavaVersion.current().majorVersion.toInt()))
        })
        finalizedBy(tasks.jacocoTestReport)

        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

subprojects {
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
