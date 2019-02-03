package swaggerify

import cats.data.Validated.Valid
import swaggerify.SwaggerifySpec._

class BodyParametersSwaggerificationSpec extends SwaggerifySpec {

  "SwaggerBuilder" should {
    "Model product type body parameter as an object" in {
      case class Prod(a: String)
      val swagger = buildSwaggerWith(BodyParameter[Prod])

      validateAndSave(swagger) must_== Valid(())
      val bodyParameter = swagger.paths.values.head.get.get.parameters.head.asInstanceOf[models.BodyParameter]
      bodyParameter.in must_== "body"
      bodyParameter.schema.isDefined must_== true
      bodyParameter.schema.get.asInstanceOf[models.RefModel].ref must_== "Prod"

      swagger.definitions("Prod").asInstanceOf[models.ModelImpl].`type` must beSome("object")
    }
  }
}
