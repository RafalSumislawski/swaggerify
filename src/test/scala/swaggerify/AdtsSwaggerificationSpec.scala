package swaggerify

import cats.data.Validated.Valid
import swaggerify.SwaggerifySpec.ResultType
import swaggerify.models._

class AdtsSwaggerificationSpec extends SwaggerifySpec {

  "SwaggerBuilder" should {

    case class Prod(a: String, b: String)

    "Build a model definition for a product type" in {
      val swagger = buildSwaggerWithResultType(ResultType[Prod])

      validateAndSave(swagger) must_== Valid(())
    }

    "Build a model definition for a recursive product type" in {
      case class RecursiveProd(a: String, b: RecursiveProd)
      val swagger = buildSwaggerWithResultType(ResultType[RecursiveProd])

      validateAndSave(swagger) must_== Valid(())
      swagger.definitions.keySet must_== Set("RecursiveProd")

      swagger.definitions("RecursiveProd").properties("a").required must_== true
      swagger.definitions("RecursiveProd").properties("b").required must_== true
    }

    "Build a model definition for a product type containing another product type" in {
      case class Prod2(a: String, b: Prod)
      val swagger = buildSwaggerWithResultType(ResultType[Prod2])

      validateAndSave(swagger) must_== Valid(())
      swagger.definitions.keySet must_== Set("Prod2", "Prod")
    }

    "Build a model definition for a product type containing another product type twice" in {
      case class Prod3(a: Prod, b: Prod)
      val swagger = buildSwaggerWithResultType(ResultType[Prod3])

      validateAndSave(swagger) must_== Valid(())
      swagger.definitions.keySet must_== Set("Prod3", "Prod")
    }

    "Build a model definition for a generic product" in {
      case class GenericProd[T](a: Option[T], b: T)
      val swagger = buildSwaggerWithResultType(ResultType[GenericProd[Prod]])

      validateAndSave(swagger) must_== Valid(())
      swagger.definitions.keySet must_== Set("GenericProd[Prod]", "Prod")
      swagger.definitions("GenericProd[Prod]").properties("a").asInstanceOf[RefProperty].ref must_== "Prod"
      swagger.definitions("GenericProd[Prod]").properties("b").asInstanceOf[RefProperty].ref must_== "Prod"
    }

    "Build two separate model definitions for a generic product parametrised with two different type parameters" in {
      case class GenericProd[T](a: Option[T], b: T)
      val swagger = buildSwaggerWithResultTypes(Seq(ResultType[GenericProd[Prod]], ResultType[GenericProd[String]]))

      validateAndSave(swagger) must_== Valid(())
      swagger.definitions.keySet must_== Set("GenericProd[Prod]", "GenericProd[String]", "Prod")
    }

    "Mark non-Option filed as required and Option fields as not required" in {
      case class ProdWithOption(optString: Option[String], string: String,
                                optInt: Option[Int], intt: Int,
                                optProd: Option[Prod], prod: Prod)
      val swagger = buildSwaggerWithResultType(ResultType[ProdWithOption])

      validateAndSave(swagger) must_== Valid(())

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
      val swagger = buildSwaggerWithResultType(ResultType[Sum])

      validateAndSave(swagger) must_== Valid(())

      swagger.definitions.keySet must_== Set("Sum", "Sum1", "Sum2")

      val sumModel = swagger.definitions("Sum").asInstanceOf[ModelImpl]
      sumModel.discriminator must beSome("type")
      swagger.definitions("Sum1").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sumModel.id.shortId
      swagger.definitions("Sum2").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sumModel.id.shortId
    }

    // I would prefer a two level swagger model here,
    // but since https://github.com/propensive/magnolia/pull/98 have been merged, magnolia flattens sealed trait hierarchies.
    "Build a flattened model definition for a two level sum type" in {
      sealed trait Sum
      case class Sum1(i: Int) extends Sum
      sealed trait Sum2 extends Sum
      case class Sum2a(s: String) extends Sum2
      case class Sum2b(d: Double) extends Sum2
      val swagger = buildSwaggerWithResultType(ResultType[Sum])

      validateAndSave(swagger) must_== Valid(())

      swagger.definitions.keySet must_== Set("Sum", "Sum1", "Sum2a", "Sum2b")

      val sumModel = swagger.definitions("Sum").asInstanceOf[ModelImpl]
      sumModel.discriminator must beSome("type")
      swagger.definitions("Sum1").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sumModel.id.shortId
      swagger.definitions("Sum2a").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sumModel.id.shortId
      swagger.definitions("Sum2b").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sumModel.id.shortId
    }

    "Build a model when whole sealed trait as well as a subset of it are used." in {
      pending("multi-level hierarchies of sealed traits aren't fully supported due to flattening in Magnolia (https://github.com/propensive/magnolia/issues/88)")
      // Sum2a and Sum2b will refer to Sum2, but Sum2 will not refer to Sum. On the other hand Sum will refer to Sum1, Sum2a and Sum2b.
      sealed trait Sum
      case class Sum1(i: Int) extends Sum
      sealed trait Sum2 extends Sum
      case class Sum2a(s: String) extends Sum2
      case class Sum2b(d: Double) extends Sum2
      val swagger = buildSwaggerWithResultTypes(Seq(ResultType[Sum], ResultType[Sum2]))

      validateAndSave(swagger) must_== Valid(())

      swagger.definitions.keySet must_== Set("Sum", "Sum1", "Sum2", "Sum2a", "Sum2b")

      val sumModel = swagger.definitions("Sum").asInstanceOf[ModelImpl]
      sumModel.discriminator must beSome("type")
      swagger.definitions("Sum1").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sumModel.id.shortId
      swagger.definitions("Sum2").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sumModel.id.shortId
      val sum2Model = swagger.definitions("Sum2").asInstanceOf[ComposedModel]
      swagger.definitions("Sum2a").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sum2Model.id.shortId
      swagger.definitions("Sum2b").asInstanceOf[ComposedModel].parent.get.asInstanceOf[RefModel].ref must_== sum2Model.id.shortId
    }
  }
}