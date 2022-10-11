plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.21.0"
}

gradlePlugin {
    plugins {
        register("com.keecon.restdocs-openapi3") {
            id = "com.keecon.restdocs-openapi3"
            implementationClass = "com.keecon.restdocs.apispec.gradle.RestdocsOpenApi3Plugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/keecon/restdocs-openapi3"
    vcsUrl = "https://github.com/keecon/restdocs-openapi3"
    tags = listOf("spring", "restdocs", "openapi3", "api", "specification")
    description =
        "Extends Spring REST Docs with API specifications in OpenAPI 3 formats"

    (plugins) {
        "com.keecon.restdocs-openapi3" {
            displayName = "restdocs-openapi3 gradle plugin"
        }
    }

    mavenCoordinates {
        groupId = "com.keecon"
        artifactId = "restdocs-api-spec-gradle-plugin"
    }
}

val jacksonVersion: String by extra
val swaggerVersion: String by extra
val assertjVersion: String by extra
val jsonpathVersion: String by extra
val junitVersion: String by extra

val jacocoRuntime: Configuration by configurations.creating

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(kotlin("gradle-plugin"))

    implementation(project(":restdocs-api-spec-model"))
    implementation(project(":restdocs-api-spec-openapi3-generator"))

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("io.swagger.core.v3:swagger-core:$swaggerVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit-pioneer:junit-pioneer:0.3.3")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("com.jayway.jsonpath:json-path:$jsonpathVersion")

    testCompileOnly(gradleTestKit())

    jacocoRuntime("org.jacoco:org.jacoco.agent:0.8.2:runtime")
}

// generate gradle properties file with jacoco agent configured
// see https://discuss.gradle.org/t/testkit-jacoco-coverage/18792
val createTestKitFiles: Task by tasks.creating {
    val outputDir = project.file("$buildDir/testkit")

    inputs.files(jacocoRuntime)
    outputs.dir(outputDir)

    doLast {
        outputDir.mkdirs()
        file("$outputDir/testkit-gradle.properties")
            .writeText("org.gradle.jvmargs=-javaagent:${jacocoRuntime.asPath}=destfile=$buildDir/jacoco/test.exec")
    }
}

tasks["test"].dependsOn(createTestKitFiles)

// Set Gradle plugin publishing credentials from environment
// see https://github.com/gradle/gradle/issues/1246
//     https://github.com/cortinico/kotlin-gradle-plugin-template/blob/1194fbbb2bc61857a76da5b5b2df919a558653de/plugin-build/plugin/build.gradle.kts#L43-L55
val configureGradlePluginCredentials: Task by tasks.creating {
    doLast {
        val key = System.getenv("GRADLE_PUBLISH_KEY")
        val secret = System.getenv("GRADLE_PUBLISH_SECRET")

        if (key == null || secret == null) {
            throw GradleException(
                "GRADLE_PUBLISH_KEY and/or GRADLE_PUBLISH_SECRET are not defined environment variables"
            )
        }

        System.setProperty("gradle.publish.key", key)
        System.setProperty("gradle.publish.secret", secret)
    }
}

tasks["publishPlugins"].dependsOn(configureGradlePluginCredentials)
