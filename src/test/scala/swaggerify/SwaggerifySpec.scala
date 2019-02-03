package swaggerify

import java.nio.file.{Files, Paths}
import java.util
import java.util.concurrent.ThreadLocalRandom

import cats.data.{NonEmptyList, Validated}
import com.typesafe.scalalogging.StrictLogging
import io.swagger.util.{Json, Yaml}
import org.apache.commons
import org.specs2.mutable.Specification
import swaggerify.SwaggerBuilder.{PathVar, Route, pathFromString}
import swaggerify.SwaggerifySpec._
import swaggerify.models.Swagger
import swaggerify.validation.SwaggerValidator

trait SwaggerifySpec extends Specification with StrictLogging {

  def validateAndSave(swagger: Swagger): Validated[NonEmptyList[String], Unit] = {
    save(swagger)
    validate(swagger)
  }

  def save(swagger: Swagger): Unit = {
    val name = f"${System.currentTimeMillis()}-${ThreadLocalRandom.current().nextInt(1000)}%03d"

    val targetDir = cleanTargetDirectory
    val yamlFilePath = targetDir.resolve(s"$name.yaml")
    logger.debug(s"Writing swagger YAML file to $yamlFilePath")
    Files.write(yamlFilePath, util.Arrays.asList(toYamlString(swagger)))

    val jsonFilePath = targetDir.resolve(s"$name.json")
    logger.debug(s"Writing swagger JSON file to $jsonFilePath")
    Files.write(jsonFilePath, util.Arrays.asList(toJsonString(swagger)))
  }

  def validate(swagger: Swagger): Validated[NonEmptyList[String], Unit] =
    SwaggerValidator.validate(toYamlString(swagger))
      .andThen(_ => SwaggerValidator.validate(toJsonString(swagger)))

  def toYamlString(swagger: Swagger): String =
    Yaml.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(swagger.toJModel)

  def toJsonString(swagger: Swagger): String =
    Json.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(swagger.toJModel)

  def buildSwaggerWithResultTypes(resultTypes: Seq[ResultType[_]],
                                  swaggerBuilder: SwaggerBuilder = new SwaggerBuilder()): Swagger = {
    resultTypes.foldLeft(swaggerBuilder)((sb, resultType) =>
      sb.add(Route("getPath1", Some("Description"),
        "get", "path",
        responses = Seq(SwaggerBuilder.ok(resultType.swaggerify))))
    ).build(swaggerify.models.Info("title", "version"))
  }

  def buildSwaggerWithResultType(resultTypes: ResultType[_],
                                 swaggerBuilder: SwaggerBuilder = new SwaggerBuilder()): Swagger = {
    Seq(resultTypes).foldLeft(swaggerBuilder)((sb, resultType) =>
      sb.add(Route("getPath1", Some("Description"),
        "get", "path",
        responses = Seq(SwaggerBuilder.ok(resultType.swaggerify))))
    ).build(swaggerify.models.Info("title", "version"))
  }

  def buildSwaggerWithBodyParam(parameter: BodyParameter[_],
                                swaggerBuilder: SwaggerBuilder = new SwaggerBuilder()): Swagger = {
    Seq(parameter).foldLeft(swaggerBuilder)((sb, param) =>
      sb.add(Route("getPath1", Some("Description"),
        "get", "path",
        responses = Seq(SwaggerBuilder.ok[String]),
        bodyParameter = Some(SwaggerBuilder.BodyParameter("body")(param.swaggerify))
      ))
    ).build(swaggerify.models.Info("title", "version"))
  }

  def buildSwaggerWithQueryParam(parameter: QueryParameter[_],
                                 swaggerBuilder: SwaggerBuilder = new SwaggerBuilder()): Swagger = {
    Seq(parameter).foldLeft(swaggerBuilder)((sb, param) =>
      sb.add(Route("getPath1", Some("Description"),
        "get", "path",
        responses = Seq(SwaggerBuilder.ok[String]),
        queryParameters = Seq(SwaggerBuilder.NonBodyParameter("queryParam1")(param.swaggerify))
      ))
    ).build(swaggerify.models.Info("title", "version"))
  }

  def buildSwaggerWithPathParam(parameter: PathParameter[_],
                                swaggerBuilder: SwaggerBuilder = new SwaggerBuilder()): Swagger = {
    Seq(parameter).foldLeft(swaggerBuilder)((sb, param) =>
      sb.add(Route("getPath1", Some("Description"),
        "get", "path" / PathVar("pathParam1")(param.swaggerify),
        responses = Seq(SwaggerBuilder.ok[String]),
      ))
    ).build(swaggerify.models.Info("title", "version"))
  }
}

object SwaggerifySpec {
  case class ResultType[T]()(implicit val swaggerify: Swaggerify[T])
  case class BodyParameter[T]()(implicit val swaggerify: Swaggerify[T])
  case class QueryParameter[T]()(implicit val swaggerify: Swaggerify[T])
  case class PathParameter[T]()(implicit val swaggerify: Swaggerify[T])

  private lazy val cleanTargetDirectory = {
    // this cleans the target directory once per test run
    val targetDir = Paths.get("target/test-swaggers")
    commons.io.FileUtils.deleteDirectory(targetDir.toFile)
    Files.createDirectories(targetDir)
  }
}
