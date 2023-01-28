@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    `java-gradle-plugin`
    alias(libs.plugins.plugin.publish)
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

val jacocoRuntime: Configuration by configurations.creating

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(kotlin("gradle-plugin"))

    implementation(project(":restdocs-api-spec-model"))
    implementation(project(":restdocs-api-spec-generator"))

    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.swagger.core)
    implementation(libs.bundles.jackson)

    testImplementation(libs.assertj.core)
    testImplementation(libs.jsonpath)
    testImplementation(libs.bundles.junit)

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
            throw GradleException("GRADLE_PUBLISH_KEY/GRADLE_PUBLISH_SECRET are not defined environment variables")
        }

        System.setProperty("gradle.publish.key", key)
        System.setProperty("gradle.publish.secret", secret)
    }
}

tasks["publishPlugins"].dependsOn(configureGradlePluginCredentials)
