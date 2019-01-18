package swaggerify.models

import java.nio.file.{Files, Paths}
import java.util

import cats.data.Validated.Valid
import io.circe.syntax._
import io.circe.yaml
import io.circe.yaml.syntax._
import org.specs2.mutable.Specification
import swaggerify.validation.SwaggerValidator

class Serialization extends Specification {

  "Swagger model" should {

    "Should serialise a swagger model without paths to a valid swagger file" in {
      val swagger = Swagger(
        swagger = "2.0",
        info = Info(
          "title",
          "version",
          Some("description"),
          Some("termOfService"),
          Some(Contact("name", Some("url"), Some("email@email.com"))),
          Some(License("name", "url"))
        ),
        host = Some("host"),
        basePath = Some("/base/path"),
        schemes = List(Scheme.HTTP, Scheme.HTTPS),
        consumes = List("aa/bb", "cc/dd"),
        produces = List("aa/bb", "cc/dd"),
        paths = Map.empty,
        securityDefinitions = Map.empty,
        definitions = Map.empty,
        parameters = Map.empty,
        externalDocs = Some(ExternalDocs("desc", "url")),
        security = List.empty
      )

      val swaggerYaml = toYamlString(swagger)
      write("test1.yaml", swaggerYaml)
      SwaggerValidator.validate(swaggerYaml) must_== Valid(())
    }

    "Should serialise paths to a valid swagger file" in {
      val swagger = Swagger(
        info = Info("title", "version"),
        paths = Map(
          "/aaa/bbb" -> Path(
            get = Some(Operation(
              tags = List("tag1", "tag2"),
              summary = Some("summary"),
              description = Some("description"),
              operationId = Some("operationId"),
              schemes = List(Scheme.HTTP, Scheme.HTTPS),
              consumes = List("aa/bb"),
              produces = List("aa/bb"),
              parameters = List(), // TODO
              responses = Map(
                "200" -> Response(
                  description = "desc",
                  schema = None,
                  examples = Map.empty,
                  headers = Map.empty
                )
              ), // TODO
              externalDocs = Some(ExternalDocs("description", "url")),
              security = List.empty, // TODO
              deprecated = true
            ))
          )
        ),
      )

      val swaggerYaml = toYamlString(swagger)
      write("test2.yaml", swaggerYaml)
      SwaggerValidator.validate(swaggerYaml) must_== Valid(())
    }
  }

  def toYamlString(swagger: Swagger): String =
    yaml.Printer.spaces2.copy(dropNullKeys = true).pretty(swagger.asJson)

  def write(file: String, text: String): Unit =
    Files.write(Paths.get(s"target/$file"), util.Arrays.asList(text))
}
