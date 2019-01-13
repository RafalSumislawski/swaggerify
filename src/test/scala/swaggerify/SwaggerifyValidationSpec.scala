package swaggerify

import io.swagger.validator.services.ValidatorService
import org.specs2.mutable.Specification

import scala.collection.JavaConverters._
import scala.io.Source

class SwaggerifyValidationSpec extends Specification {
  "SwaggerifyValidationSpec" should {
    "Validate swagger" in {
      val swagger = Source.fromResource("xyz.yaml").mkString
      val validationResponse = new ValidatorService().debugByContent(null, null, swagger)
      val messages = Option(validationResponse.getMessages).toSeq.flatMap(_.asScala) ++
        Option(validationResponse.getSchemaValidationMessages).toSeq.flatMap(_.asScala).map(_.getMessage)
      messages must beEmpty
    }
  }
}


