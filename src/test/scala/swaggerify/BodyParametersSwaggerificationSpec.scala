package swaggerify

import cats.data.Validated.Valid
import swaggerify.SwaggerifySpec._

class BodyParametersSwaggerificationSpec extends SwaggerifySpec {

  "SwaggerBuilder" should {
    "Model product type body parameter as a ref to object model" in {
      case class Prod(a: String)
      val swagger = buildSwaggerWithBodyParam(BodyParameter[Prod])

      validateAndSave(swagger) must_== Valid(())
      val bodyParameter = swagger.paths.values.head.get.get.parameters.head.asInstanceOf[models.BodyParameter]
      bodyParameter.in must_== "body"
      bodyParameter.schema.get.asInstanceOf[models.RefModel].ref must_== "Prod"

      swagger.definitions("Prod").asInstanceOf[models.ModelImpl].`type` must beSome("object")
    }

    "Model string body parameter as a string" in {
      val swagger = buildSwaggerWithBodyParam(BodyParameter[String])

      validateAndSave(swagger) must_== Valid(())
      val bodyParameter = swagger.paths.values.head.get.get.parameters.head.asInstanceOf[models.BodyParameter]
      bodyParameter.in must_== "body"
      bodyParameter.schema.get.asInstanceOf[models.ModelImpl].`type` must beSome("string")
      swagger.definitions.size must_== 0
    }

    "Model int body parameter as an int" in {
      val swagger = buildSwaggerWithBodyParam(BodyParameter[Short])

      validateAndSave(swagger) must_== Valid(())
      val bodyParameter = swagger.paths.values.head.get.get.parameters.head.asInstanceOf[models.BodyParameter]
      bodyParameter.in must_== "body"
      bodyParameter.schema.get.asInstanceOf[models.ModelImpl].`type` must beSome("integer")
      bodyParameter.schema.get.asInstanceOf[models.ModelImpl].format must beSome("int32")
      bodyParameter.schema.get.asInstanceOf[models.ModelImpl].description must beSome("short")
      swagger.definitions.size must_== 0
    }

    "Model array body parameter as an array" in {
      val swagger = buildSwaggerWithBodyParam(BodyParameter[Array[Short]])

      validateAndSave(swagger) must_== Valid(())
      val bodyParameter = swagger.paths.values.head.get.get.parameters.head.asInstanceOf[models.BodyParameter]
      bodyParameter.in must_== "body"
      bodyParameter.schema.get.asInstanceOf[models.ArrayModel].uniqueItems must_== false
      bodyParameter.schema.get.asInstanceOf[models.ArrayModel].items.get.`type` must_== "integer"
      bodyParameter.schema.get.asInstanceOf[models.ArrayModel].items.get.format must beSome("int32")
      bodyParameter.schema.get.asInstanceOf[models.ArrayModel].items.get.description must beSome("short")
      swagger.definitions.size must_== 0
    }

  }
}
