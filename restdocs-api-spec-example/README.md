# Spring REST Docs OpenAPI 3 Examples

## How to run

Run the following command from the root directory.

```shell
# generate OpenAPI Specification
gradle :restdocs-api-spec-example:openapi3

# serving HTML API Documents with ReDoc
npx @redocly/cli preview-docs restdocs-api-spec-example/build/api-spec/openapi3.yaml
```
