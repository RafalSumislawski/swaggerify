package swaggerify

import java.nio.file.{Files, Paths}
import java.util
import java.util.concurrent.ThreadLocalRandom

import cats.data.{NonEmptyList, Validated}
import com.typesafe.scalalogging.StrictLogging
import io.swagger.util.Yaml
import org.specs2.mutable.Specification
import swaggerify.SwaggerBuilder.Route
import swaggerify.SwaggerifySpec.ResultType
import swaggerify.models.Swagger
import swaggerify.validation.SwaggerValidator

trait SwaggerifySpec extends Specification with StrictLogging {

  def validateAndSave(swagger: Swagger): Validated[NonEmptyList[String], Unit] = {
    save(swagger)
    validate(swagger)
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

  def buildSwaggerFileWith(resultTypes: ResultType[_]*): Swagger = {
    resultTypes.foldLeft(SwaggerBuilder())((sb, resultType) =>
      sb.add(Route("getPath1", Some("Description"),
        "get", "path",
        responses = Seq(SwaggerBuilder.ok(resultType.swaggerify))))
    ).build(swaggerify.models.Info("title", "version"))
  }
}

object SwaggerifySpec{
  case class ResultType[T]()(implicit val swaggerify: Swaggerify[T])
}

