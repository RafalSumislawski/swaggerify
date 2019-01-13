# Swaggerify
Swaggerify is a library implementing type-class approach to generating swagger files from Scala code.

Swaggerify is heavily inspired by, and based on [http4s/rho](https://github.com/http4s/rho). Swaggerify is a research towards providing similar features with more compile-time assertions, as well as improving compatibility with the [Swagger 2.0/OpenAPI 2.0 specification](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md) and [client generators](https://github.com/OpenAPITools/openapi-generator).

## Design
Swaggerify is designed aroud `Swaggerify[T]` type-class which provies facilities to generate Swagger model/property/parameter from type `T`.
`Swaggerify` instances for Algebraic Data Types (ADTs) are derived with [magnolia](https://github.com/propensive/magnolia).

## Development Status
Building  models, properties and parameters from Scala (and Java) types is mostly ready with few minor exceptions.
A minimalistic builder for describing routes/paths is under development, although the plan is to eventually replace it with a modified version of rho's DSL. Setup for testing compatibility with specification using [swagger-validator](https://github.com/swagger-api/validator-badge) is in place. I've done some test with a real-life API model, which I couldn't share here.

## License
Swaggerify is licensed under Apache License, Version 2.0 (see [LICENSE](LICENSE)).
Swaggerify contains parts of code from [http4s/rho](https://github.com/http4s/rho) licensed under Apache License, Version 2.0 (see the license of rho: [LICENSE.rho](LICENSE.rho))
