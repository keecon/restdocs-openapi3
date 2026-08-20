@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        register("com.keecon.restdocs-openapi3") {
            id = "com.keecon.restdocs-openapi3"
            implementationClass = "com.keecon.restdocs.apispec.gradle.RestdocsOpenApi3Plugin"
        }
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

    jacocoRuntime(libs.jacoco.agent) {
        artifact {
            classifier = "runtime"
        }
    }
}

// generate gradle properties file with jacoco agent configured
// see https://discuss.gradle.org/t/testkit-jacoco-coverage/18792
val createTestKitFiles: Task by tasks.creating {
    val buildDir = "${getProjectDir()}/build"
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
