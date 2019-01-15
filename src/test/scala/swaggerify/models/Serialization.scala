package swaggerify.models

import java.nio.file.{Files, Paths}
import java.util

import io.swagger.util.ObjectMapperFactory
import swaggerify.SwaggerDefinitionsBuilder

import io.circe.syntax._

import io.circe.generic.auto._

object Serialization extends App {

  case class Foo(a: String, b: Array[Bar])
  case class Bar(map: Map[String, AnyRef])

  val definitions = SwaggerDefinitionsBuilder().addPropertyType[Foo].build()
  println(definitions)

  val json = definitions.asJson.spaces2
  Files.write(Paths.get("target/test.json"), util.Arrays.asList(json))
}
