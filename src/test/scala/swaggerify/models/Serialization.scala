package swaggerify.models

import java.nio.file.{Files, Paths}
import java.util

import io.circe.syntax._
import io.circe.yaml._
import io.circe.yaml.syntax._
import swaggerify.validation.SwaggerValidator

object Serialization extends App {

//  case class Foo(a: String, b: Array[Bar])
//  case class Bar(map: Map[String, AnyRef])
//
//  val definitions = SwaggerDefinitionsBuilder().addPropertyType[Foo].build()
//  println(definitions)
//
//  val json = definitions.asJson.spaces2
//  Files.write(Paths.get("target/test.json"), util.Arrays.asList(json))

  val swagger = Swagger(
    swagger = "2.0",
    info = Some(Info(
      "title",
      "version",
      Some("description"),
      Some("termOfService"),
      Some(Contact("name", Some("url"), Some("email"))),
      Some(License("name", "url"))
    )),
    host = Some("host"),
    basePath = Some(""),
    schemes = List(Scheme.HTTP, Scheme.HTTPS),
    consumes = List("aa/bb", "cc/dd"),
    produces = List("aa/bb", "cc/dd"),
    paths = Map.empty,
    securityDefinitions = Map.empty,
    definitions = Map.empty,
    parameters = Map.empty,
    externalDocs = Some(ExternalDocs("desc", "url")),
    security = Map.empty,
    vendorExtensions = Map.empty
  )

  val swaggerYaml = swagger.asJson.asYaml.spaces2
  Files.write(Paths.get("target/test.yaml"), util.Arrays.asList(swaggerYaml))
  val result = SwaggerValidator.validate(swaggerYaml)
  println(result)
}
