rootProject.name = "restdocs-openapi3"

include(
    ":restdocs-api-spec",
    ":restdocs-api-spec-generator",
    ":restdocs-api-spec-jsonschema",
    ":restdocs-api-spec-mockmvc",
    ":restdocs-api-spec-model",
)

include(":restdocs-api-spec-gradle-plugin")
