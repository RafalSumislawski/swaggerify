package swaggerify

import java.nio.file.{Files, Paths}
import java.util
import java.util.concurrent.ThreadLocalRandom

import cats.data.Validated.Valid
import cats.data.{NonEmptyList, Validated}
import com.typesafe.scalalogging.StrictLogging
import io.swagger.util.Yaml
import org.specs2.mutable.Specification
import swaggerify.SwaggerBuilder._
import swaggerify.models._
import swaggerify.validation.SwaggerValidator

class ModelSwaggerificationSpec extends Specification with StrictLogging {
  "SwaggerBuilder" should {

    case class Prod(a: String, b: String)

    "Build a model definition for a product type" in {
      val swagger = buildSwaggerFileWithResultType[Prod]

      save(swagger)
      validate(swagger) must_== Valid(())
    }

    case class RecursiveProd(a: String, b: RecursiveProd)

    "Build a model definition for a recursive product type" in {
      val swagger = buildSwaggerFileWithResultType[RecursiveProd]

      save(swagger)
      validate(swagger) must_== Valid(())
      swagger.definitions.keySet must_== Set("RecursiveProd")

      swagger.definitions("RecursiveProd").properties("a").required must_== true
      swagger.definitions("RecursiveProd").properties("b").required must_== true
    }

    case class Prod2(a: String, b: Prod)

    "Build a model definition for a product type containing another product type" in {
      val swagger = buildSwaggerFileWithResultType[Prod2]

      save(swagger)
      validate(swagger) must_== Valid(())
      swagger.definitions.keySet must_== Set("RecursiveProd", "Prod")
    }

    case class ProdWithOption(a: Option[String], b: String)

    "Mark non-Option filed as required and Option fields as not required" in {
      val swagger = buildSwaggerFileWithResultType[ProdWithOption]

      save(swagger)
      validate(swagger) must_== Valid(())

      swagger.definitions("ProdWithOption").properties("a").required must_== false
      swagger.definitions("ProdWithOption").properties("b").required must_== true
    }
  }

  private def validate(swagger: Swagger): Validated[NonEmptyList[String], Unit] =
    SwaggerValidator.validate(toYamlString(swagger))

  private def toYamlString(swagger: Swagger): String =
    Yaml.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(swagger.toJModel)

  private def save(swagger: Swagger): Unit = {
    val filePath = Paths.get(f"target/${System.currentTimeMillis()}-${ThreadLocalRandom.current().nextInt(1000)}%03d.yaml")
    logger.debug(s"Writing swagger YAML file to $filePath")
    Files.write(filePath, util.Arrays.asList(toYamlString(swagger)))
  }

  private def buildSwaggerFileWithResultType[T: Swaggerify]: Swagger = {
    SwaggerBuilder()
      .add(Route("getPath", Some("Description"),
        "get", "path",
        responses = Seq(SwaggerBuilder.ok[T])))
      .build(swaggerify.models.Info("title", "version"))
  }
}


