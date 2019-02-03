package swaggerify

import cats.data.Validated.Valid
import swaggerify.SwaggerifySpec.ResultType

class PropertiesSwaggerificationSpec extends SwaggerifySpec {

  "SwaggerBuilder" should {

    "Model Byte properties as integer(int32)" in {
      case class Bytes(s: Byte, j: java.lang.Byte)
      val swagger = buildSwaggerWithResultType(ResultType[Bytes])

      validateAndSave(swagger) must_== Valid(())
      val properties = swagger.definitions.values.head.properties
      properties.size must_== 2

      properties("s").`type` must_== "integer"
      properties("s").format must beSome("int32")
      properties("s").required must_== true
      properties("j") must_== properties("s")
    }

    "Model Short properties as integer(int32)" in {
      case class Shorts(s: Short, j: java.lang.Short)
      val swagger = buildSwaggerWithResultType(ResultType[Shorts])

      validateAndSave(swagger) must_== Valid(())
      val properties = swagger.definitions.values.head.properties
      properties.size must_== 2

      properties("s").`type` must_== "integer"
      properties("s").format must beSome("int32")
      properties("s").required must_== true
      properties("j") must_== properties("s")
    }

    "Model Int properties as integer(int32)" in {
      case class Ints(s: Int, j: java.lang.Integer)
      val swagger = buildSwaggerWithResultType(ResultType[Ints])

      validateAndSave(swagger) must_== Valid(())
      val properties = swagger.definitions.values.head.properties
      properties.size must_== 2

      properties("s").`type` must_== "integer"
      properties("s").format must beSome("int32")
      properties("s").required must_== true
      properties("j") must_== properties("s")
    }

    "Model Long properties as integer(int64)" in {
      case class Longs(s: Long, j: java.lang.Long)
      val swagger = buildSwaggerWithResultType(ResultType[Longs])

      validateAndSave(swagger) must_== Valid(())
      val properties = swagger.definitions.values.head.properties
      properties.size must_== 2

      properties("s").`type` must_== "integer"
      properties("s").format must beSome("int64")
      properties("s").required must_== true
      properties("j") must_== properties("s")
    }

    "Model Float properties as number(float)" in {
      case class Floats(s: Float, j: java.lang.Float)
      val swagger = buildSwaggerWithResultType(ResultType[Floats])

      validateAndSave(swagger) must_== Valid(())
      val properties = swagger.definitions.values.head.properties
      properties.size must_== 2

      properties("s").`type` must_== "number"
      properties("s").format must beSome("float")
      properties("s").required must_== true
      properties("j") must_== properties("s")
    }

    "Model Double properties as number(double)" in {
      case class Doubles(s: Double, j: java.lang.Double)
      val swagger = buildSwaggerWithResultType(ResultType[Doubles])

      validateAndSave(swagger) must_== Valid(())
      val properties = swagger.definitions.values.head.properties
      properties.size must_== 2

      properties("s").`type` must_== "number"
      properties("s").format must beSome("double")
      properties("s").required must_== true
      properties("j") must_== properties("s")
    }

    "Model sequence properties as arrays" in {
      // obviously these aren't all possible type of "arrays" in Scala/Java.
      case class Seqs(s: Seq[Int], a: Array[Int], l: List[Int], v: Vector[Int],
                      jl: java.util.List[Int], jal: java.util.ArrayList[Int], jll: java.util.LinkedList[Int])
      val swagger = buildSwaggerWithResultType(ResultType[Seqs])

      validateAndSave(swagger) must_== Valid(())
      val properties = swagger.definitions.values.head.properties

      properties("s").`type` must_== "array"
      properties("s").required must_== true
      properties("s").asInstanceOf[models.ArrayProperty].uniqueItems must_== false
      properties("s").asInstanceOf[models.ArrayProperty].items.`type` must_== "integer"
      properties("s").asInstanceOf[models.ArrayProperty].items.format must beSome("int32")

      properties("a") must_== properties("s")
      properties("l") must_== properties("s")
      properties("v") must_== properties("s")
      properties("jl") must_== properties("s")
      properties("jal") must_== properties("s")
      properties("jll") must_== properties("s")
    }

    "Model set properties as arrays with uniqueItems" in {
      // obviously these aren't all possible type of "arrays" in Scala/Java.
      case class Sets(s: Set[Int], js: java.util.Set[Int])
      val swagger = buildSwaggerWithResultType(ResultType[Sets])

      validateAndSave(swagger) must_== Valid(())
      val properties = swagger.definitions.values.head.properties

      properties("s").`type` must_== "array"
      properties("s").required must_== true
      properties("s").asInstanceOf[models.ArrayProperty].uniqueItems must_== true
      properties("s").asInstanceOf[models.ArrayProperty].items.`type` must_== "integer"
      properties("s").asInstanceOf[models.ArrayProperty].items.format must beSome("int32")

      properties("js") must_== properties("s")
    }

    "Model map properties as objects with additionalProperties" in {
      case class Maps(m: Map[String, Int], jm: java.util.Map[String, Int])
      val swagger = buildSwaggerWithResultType(ResultType[Maps])

      validateAndSave(swagger) must_== Valid(())
      val properties = swagger.definitions.values.head.properties

      properties("m").`type` must_== "object"
      properties("m").required must_== true
      properties("m").asInstanceOf[models.MapProperty].additionalProperties.`type` must_== "integer"
      properties("m").asInstanceOf[models.MapProperty].additionalProperties.format must beSome("int32")

      properties("jm") must_== properties("m")
    }

    "Model properties of sealed traits of case objects as enum strings" in {
      sealed trait AnEnum
      case object Value1 extends AnEnum
      case object Value2 extends AnEnum
      case class TypeWithEnumField(e: AnEnum)
      Value1; Value2 // avoid "unused" warning

      val swagger = buildSwaggerWithResultType(ResultType[TypeWithEnumField])


      validateAndSave(swagger) must_== Valid(())
      val properties = swagger.definitions.values.head.properties

      properties("e").`type` must_== "string"
      properties("e").required must_== true
      properties("e").asInstanceOf[models.StringProperty].enums must_== Set("Value1", "Value2")
    }
  }
}