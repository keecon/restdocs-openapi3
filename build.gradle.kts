import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21" apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    id("pl.allegro.tech.build.axion-release") version "1.14.2"
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

allprojects {

    group = "com.keecon"
    version = scmVer

    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "jacoco")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    repositories {
        google()
        mavenCentral()
    }

    tasks.test {
        useJUnitPlatform()
        finalizedBy(tasks.jacocoTestReport)

        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

subprojects {

    val jacksonVersion by extra { "2.13.1" }
    val springBootVersion by extra { "2.6.4" }
    val springRestDocsVersion by extra { "2.0.6.RELEASE" }
    val junitVersion by extra { "5.8.2" }
    val jsonpathVersion by extra { "2.6.0" }
    val swaggerVersion by extra { "2.1.13" }
    val swaggerParserVersion by extra { "2.0.30" }
    val assertjVersion by extra { "3.21.0" }
    val hibernateValidatorVersion by extra { "6.2.2.Final" }
    val jsonSchemaValidatorVersion by extra { "2.2.14" }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = JavaVersion.VERSION_11.toString()
        }
    }

    if (!isPluginProject()) {
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

jacoco {
    toolVersion = "0.8.7"
}

val jacocoMergeData = tasks.create<JacocoMerge>("jacocoMergeData") {
    dependsOn(subprojects.map { it.tasks.jacocoTestReport })
    destinationFile = file("$buildDir/jacoco/all-tests.exec")
    executionData(files(subprojects.map { it.tasks.jacocoTestReport.get().executionData }).filter { it.exists() })
}

tasks.jacocoTestReport {
    dependsOn(tasks.test, jacocoMergeData)
    description = "Generates an aggregate report from all subprojects"

    sourceSets(*subprojects.map { it.sourceSets.main.get() }.toTypedArray())
    executionData(jacocoMergeData.executionData)

    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(file("$buildDir/jacocoHtml"))
    }
}
