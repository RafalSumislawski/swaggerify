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

    case class Foo(a: String, b: String)

    "Build a model definition for a case class" in {

      val swagger = buildSwaggerFileWithResultType[Foo]

      save(swagger)
      validate(swagger) must_== Valid(())
    }
  }

  def validate(swagger: Swagger): Validated[NonEmptyList[String], Unit] =
    SwaggerValidator.validate(toYamlString(swagger))

  def toYamlString(swagger: Swagger): String =
    Yaml.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(swagger.toJModel)

  def save(swagger: Swagger): Unit = {
    val filePath = Paths.get(f"target/${System.currentTimeMillis()}-${ThreadLocalRandom.current().nextInt(1000)}%03d.yaml")
    logger.debug(s"Writing swagger YAML file to $filePath")
    Files.write(filePath, util.Arrays.asList(toYamlString(swagger)))
  }

  def buildSwaggerFileWithResultType[T: Swaggerify]: Swagger = {
    SwaggerBuilder()
      .add(Route("getPath", Some("Description"),
        "get", "path",
        responses = Seq(SwaggerBuilder.ok[T])))
      .build(swaggerify.models.Info("title", "version"))
  }
}


