package swaggerify

import java.nio.file.{Files, Paths}
import java.util
import java.util.concurrent.ThreadLocalRandom

import cats.data.Validated.Valid
import cats.data.{NonEmptyList, Validated}
import com.typesafe.scalalogging.StrictLogging
import io.swagger.util.Yaml
import org.specs2.mutable.Specification
import swaggerify.AdtsSwaggerificationSpec.ResultType
import swaggerify.SwaggerBuilder._
import swaggerify.models._
import swaggerify.validation.SwaggerValidator

class AdtsSwaggerificationSpec extends Specification with StrictLogging {

  "SwaggerBuilder" should {

    case class Prod(a: String, b: String)

    "Build a model definition for a product type" in {
      val swagger = buildSwaggerFileWith(ResultType[Prod])

      save(swagger)
      validate(swagger) must_== Valid(())
    }


    "Build a model definition for a recursive product type" in {
      case class RecursiveProd(a: String, b: RecursiveProd)
      val swagger = buildSwaggerFileWith(ResultType[RecursiveProd])

      save(swagger)
      validate(swagger) must_== Valid(())
      swagger.definitions.keySet must_== Set("RecursiveProd")

      swagger.definitions("RecursiveProd").properties("a").required must_== true
      swagger.definitions("RecursiveProd").properties("b").required must_== true
    }


    "Build a model definition for a product type containing another product type" in {
      case class Prod2(a: String, b: Prod)
      val swagger = buildSwaggerFileWith(ResultType[Prod2])

      save(swagger)
      validate(swagger) must_== Valid(())
      swagger.definitions.keySet must_== Set("Prod2", "Prod")
    }


    "Build a model definition for a product type containing another product type twice" in {
      case class Prod3(a: Prod, b: Prod)
      val swagger = buildSwaggerFileWith(ResultType[Prod3])

      save(swagger)
      validate(swagger) must_== Valid(())
      swagger.definitions.keySet must_== Set("Prod3", "Prod")
    }

    "Build a model definition for a generic product" in {
      case class GenericProd[T](a: Option[T], b: T)
      val swagger = buildSwaggerFileWith(ResultType[GenericProd[Prod]])

      save(swagger)
      validate(swagger) must_== Valid(())
      swagger.definitions.keySet must_== Set("GenericProd", "Prod")
      swagger.definitions("GenericProd").properties("a").asInstanceOf[RefProperty].ref must_== "Prod"
      swagger.definitions("GenericProd").properties("b").asInstanceOf[RefProperty].ref must_== "Prod"
    }

    "Build two separate model definitions for a generic product parametrised with two different type parameters" in {
      case class GenericProd[T](a: Option[T], b: T)
      val swagger = buildSwaggerFileWith(ResultType[GenericProd[Prod]], ResultType[GenericProd[String]])

      save(swagger)
      validate(swagger) must_== Valid(())
      swagger.definitions.keySet must_== Set("GenericProd", "Prod")
    }

    "Mark non-Option filed as required and Option fields as not required" in {
      case class ProdWithOption(optString: Option[String], string: String,
                                optInt: Option[Int], intt: Int,
                                optProd: Option[Prod], prod: Prod)
      val swagger = buildSwaggerFileWith(ResultType[ProdWithOption])

      save(swagger)
      validate(swagger) must_== Valid(())

      val properties = swagger.definitions("ProdWithOption").properties
      properties("optString").required must_== false
      properties("string").required must_== true
      properties("optInt").required must_== false
      properties("intt").required must_== true
      properties("optProd").required must_== false
      properties("prod").required must_== true
    }

    "Build a model definition for a sum type consisting of two product types" in {
      sealed trait Sum
      case class Sum1(i: Int) extends Sum
      case class Sum2(s: String) extends Sum
      val swagger = buildSwaggerFileWith(ResultType[Sum])

      save(swagger)
      validate(swagger) must_== Valid(())

      swagger.definitions.keySet must_== Set("Sum", "Sum1", "Sum2")

      val sumModel = swagger.definitions("Sum").asInstanceOf[ModelImpl]
      sumModel.discriminator must_== Some("type")
      swagger.definitions("Sum1").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sumModel.id2
      swagger.definitions("Sum2").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sumModel.id2
    }



    // I would prefer a two level swagger model here,
    // but since https://github.com/propensive/magnolia/pull/98 have been merged, magnolia flattens sealed trait hierarchies.
    "Build a flattened model definition for a two level sum type" in {
      sealed trait Sum
      case class Sum1(i: Int) extends Sum
      sealed trait Sum2 extends Sum
      case class Sum2a(s: String) extends Sum2
      case class Sum2b(d: Double) extends Sum2
      val swagger = buildSwaggerFileWith(ResultType[Sum])

      save(swagger)
      validate(swagger) must_== Valid(())

      swagger.definitions.keySet must_== Set("Sum", "Sum1", "Sum2a", "Sum2b")

      val sumModel = swagger.definitions("Sum").asInstanceOf[ModelImpl]
      sumModel.discriminator must_== Some("type")
      swagger.definitions("Sum1").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sumModel.id2
      swagger.definitions("Sum2a").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sumModel.id2
      swagger.definitions("Sum2b").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sumModel.id2
    }

    "Build a model when whole sealed trait as well as a subset of it are used." in {
      pending("multi-level hierarchies of sealed traits aren't fully supported do to fattening in magnolia")
      // Sum2a and Sum2b will refer to Sum2, but Sum2 will not refer to Sum. On the other hand Sum will refer to Sum1, Sum2a and Sum2b.
      sealed trait Sum
      case class Sum1(i: Int) extends Sum
      sealed trait Sum2 extends Sum
      case class Sum2a(s: String) extends Sum2
      case class Sum2b(d: Double) extends Sum2
      val swagger = buildSwaggerFileWith(ResultType[Sum], ResultType[Sum2])

      save(swagger)
      validate(swagger) must_== Valid(())

      swagger.definitions.keySet must_== Set("Sum", "Sum1", "Sum2", "Sum2a", "Sum2b")

      val sumModel = swagger.definitions("Sum").asInstanceOf[ModelImpl]
      sumModel.discriminator must_== Some("type")
      swagger.definitions("Sum1").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sumModel.id2
      swagger.definitions("Sum2").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sumModel.id2
      val sum2Model = swagger.definitions("Sum2").asInstanceOf[ComposedModel]
      swagger.definitions("Sum2a").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sum2Model.id2
      swagger.definitions("Sum2b").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sum2Model.id2
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

  private def buildSwaggerFileWith(resultTypes: ResultType[_]*): Swagger = {
    resultTypes.foldLeft(SwaggerBuilder())((sb, resultType) =>
      sb.add(Route("getPath1", Some("Description"),
        "get", "path",
        responses = Seq(SwaggerBuilder.ok(resultType.swaggerify))))
    ).build(swaggerify.models.Info("title", "version"))
  }
}

object AdtsSwaggerificationSpec{
  private case class ResultType[T]()(implicit val swaggerify: Swaggerify[T])
}
