package swaggerify

import io.swagger.models.parameters.AbstractSerializableParameter
import io.swagger.{models => jm}
import org.http4s.rho.swagger.models.Property
import swaggerify.NonBodyParameter.JModelNonBodyParameter
import swaggerify.NonBodyParameter.JsValue._

import scala.collection.JavaConverters._

case class NonBodyParameter(in: String,
                            name: String,
                            `type`: Option[String] = None,
                            format: Option[String] = None,
                            collectionFormat: Option[String] = None,
                            items: Option[Property] = None,
                            default: Option[Any] = None,
                            description: Option[String] = None,
                            required: Boolean = false,
                            access: Option[String] = None,
                            vendorExtensions: Map[String, Any] = Map.empty,
                            enums: List[String] = List.empty) {

  def toJModel: jm.parameters.Parameter = {
    val qp = new JModelNonBodyParameter
    qp.setIn(in)
    qp.setName(name)
    qp.setType(fromOption(`type`))
    qp.setFormat(fromOption(format))
    if (`type`.contains("array")) qp.setCollectionFormat(fromOption(collectionFormat))
    qp.setItems(fromOption(items.map(_.toJModel)))
    qp.setDefault(fromOption(default)) // using default instead of default value like rho currently does. This allows defaults for arrays.
    qp.setDescription(fromOption(description))
    qp.setRequired(required)
    qp.setAccess(fromOption(access))
    qp.setEnumValue(fromList(enums))
    vendorExtensions.foreach { case (key, value) => qp.setVendorExtension(key, value) }
    qp
  }
}

object NonBodyParameter {

  class JModelNonBodyParameter extends AbstractSerializableParameter[JModelNonBodyParameter]

  object JsValue {
    def fromOption[A](oa: Option[A]): A =
      oa.getOrElse(null.asInstanceOf[A])

    def fromList[A](xs: List[A]): java.util.List[A] =
      if (xs.isEmpty) null else xs.asJava

    def fromMap[A, B](m: Map[A, B]): java.util.Map[A, B] =
      if (m.isEmpty) null else m.asJava
  }
}
