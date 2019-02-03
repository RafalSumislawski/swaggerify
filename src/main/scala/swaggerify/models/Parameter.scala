package swaggerify.models

import io.swagger.models.parameters.AbstractSerializableParameter
import io.swagger.{models => jm}
import swaggerify.models.JValue._
import swaggerify.models.NonBodyParameter.JModelNonBodyParameter

sealed trait Parameter {
  def in: String
  def name: String
  def access: Option[String]
  def description: Option[String]
  def required: Boolean
  def vendorExtensions: Map[String, Any]

  def toJModel: jm.parameters.Parameter
}

case class BodyParameter
(
    name             : String
  , schema           : Option[Model]    = None
  , description      : Option[String]   = None
  , required         : Boolean          = true
  , access           : Option[String]   = None
  , vendorExtensions : Map[String, Any] = Map.empty
) extends Parameter {

  override val in = "body"

  def toJModel: jm.parameters.Parameter = {
    val bp = new jm.parameters.BodyParameter
    bp.setSchema(fromOption(schema.map(_.toJModel)))
    bp.setName(name)
    bp.setDescription(fromOption(description))
    bp.setRequired(required)
    bp.setAccess(fromOption(access))
    vendorExtensions.foreach { case (key, value) => bp.setVendorExtension(key, value) }
    bp
  }
}

case class NonBodyParameter(in: String,
                            name: String,
                            `type`: Option[String] = None,
                            format: Option[String] = None,
                            collectionFormat: Option[String] = None,
                            items: Option[Property] = None,
                            default: Option[Any] = None,
                            description: Option[String] = None,
                            required: Boolean = true,
                            access: Option[String] = None,
                            vendorExtensions: Map[String, Any] = Map.empty,
                            enums: List[String] = List.empty) extends Parameter {

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

}
