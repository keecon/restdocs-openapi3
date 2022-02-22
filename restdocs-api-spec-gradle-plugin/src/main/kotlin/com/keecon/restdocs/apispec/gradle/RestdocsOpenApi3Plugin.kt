package com.keecon.restdocs.apispec.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

open class RestdocsOpenApi3Plugin : Plugin<Project> {

    private fun <T : ApiSpecTask> T.applyWithCommonConfiguration(block: T.() -> Unit): T {
        dependsOn("check")
        group = "documentation"
        block()
        return this
    }

    override fun apply(project: Project) {
        with(project) {
            extensions.create(OpenApi3Extension.name, OpenApi3Extension::class.java, project)

            afterEvaluate {
                val openapi3 = extensions.findByName(OpenApi3Extension.name) as OpenApi3Extension
                tasks.create<OpenApi3Task>("openapi3").applyWithCommonConfiguration {
                    description = "Aggregate resource fragments into an OpenAPI 3 specification"
                    applyExtension(openapi3)
                }
            }
        }
    }
}
