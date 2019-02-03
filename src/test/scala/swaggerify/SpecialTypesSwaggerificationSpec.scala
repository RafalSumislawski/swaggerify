package swaggerify

import cats.data.Validated.Valid
import swaggerify.SwaggerifySpec.ResultType

class SpecialTypesSwaggerificationSpec extends SwaggerifySpec {

  "SwaggerBuilder" should {

    "Build an empty model definition for Unit" in {
      val swagger = buildSwaggerWith(ResultType[Unit])

      validateAndSave(swagger) must_== Valid(())
      swagger.definitions("Unit").asInstanceOf[swaggerify.models.ModelImpl].`type` must beSome("object")
      swagger.definitions("Unit").properties.size must_== 0
    }

    "Build an empty model definition for Void" in {
      val swagger = buildSwaggerWith(ResultType[java.lang.Void])

      validateAndSave(swagger) must_== Valid(())
      swagger.definitions("Void").asInstanceOf[swaggerify.models.ModelImpl].`type` must beSome("object")
      swagger.definitions("Void").properties.size must_== 0
    }

    "Build an empty model definition for Nothing" in {
      val swagger = buildSwaggerWith(ResultType[Nothing])

      validateAndSave(swagger) must_== Valid(())
      swagger.definitions("Nothing").asInstanceOf[swaggerify.models.ModelImpl].`type` must beSome("object")
      swagger.definitions("Nothing").properties.size must_== 0
    }

    "Build an empty model definition for Null (named Nul due to issues with YAML)" in {
      val swagger = buildSwaggerWith(ResultType[Null])

      validateAndSave(swagger) must_== Valid(())
      swagger.definitions("Nul").asInstanceOf[swaggerify.models.ModelImpl].`type` must beSome("object")
      swagger.definitions("Nul").properties.size must_== 0
    }
  }
}
