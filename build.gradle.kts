import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.axion.release)
    java
    jacoco
    `maven-publish`
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

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = JavaVersion.VERSION_17.toString()
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
        finalizedBy(tasks.jacocoTestCoverageVerification)

        reports {
            xml.required.set(true)
            html.required.set(true)
            html.outputLocation.set(file("$buildDir/jacocoHtml"))
        }
    }

    tasks.jacocoTestCoverageVerification {
        violationRules {
            rule {
                limit {
                    // 'counter'    -> 'INSTRUCTION' (default)
                    // 'value'      -> 'COVEREDRATIO' (default)
                    minimum = "0.30".toBigDecimal()
                }
            }

            // rule {
            //     enabled = true
            //     element = "CLASS"
            //
            //     limit {
            //         counter = "BRANCH"
            //         value = "COVEREDRATIO"
            //         minimum = "0.90".toBigDecimal()
            //     }
            //
            //     limit {
            //         counter = "LINE"
            //         value = "COVEREDRATIO"
            //         minimum = "0.80".toBigDecimal()
            //     }
            //
            //     limit {
            //         counter = "LINE"
            //         value = "TOTALCOUNT"
            //         maximum = "200".toBigDecimal()
            //     }
            //
            //     excludes = listOf(
            //         "*.test.*",
            //     )
            // }
        }
    }
}

val mergeTargets = subprojects.filterNot { it.isExampleProject() }

val jacocoMergeData = tasks.create<JacocoMerge>("jacocoMergeData") {
    dependsOn(mergeTargets.map { it.tasks.jacocoTestReport })
    destinationFile = file("$buildDir/jacoco/all-tests.exec")
    executionData(files(mergeTargets.map { it.tasks.jacocoTestReport.get().executionData }).filter { it.exists() })
}

tasks.jacocoTestReport {
    dependsOn(tasks.test, jacocoMergeData)
    description = "Generates an aggregate report from all subprojects"

    sourceSets(*mergeTargets.map { it.sourceSets.main.get() }.toTypedArray())
    executionData(jacocoMergeData.executionData)

    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(file("$buildDir/jacocoHtml"))
    }
}
