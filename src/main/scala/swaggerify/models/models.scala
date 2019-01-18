package swaggerify

import io.circe.{Encoder, Json}
import io.circe.generic.auto._
import io.circe.generic.semiauto

package object models {

  case class Swagger
    (
      swagger             : String                                = "2.0"
    , info                : Option[Info]                          = None
    , host                : Option[String]                        = None
    , basePath            : Option[String]                        = None
    , schemes             : List[Scheme]                          = Nil
    , consumes            : List[String]                          = Nil
    , produces            : List[String]                          = Nil
    , paths               : Map[String, Path]                     = Map.empty
    , securityDefinitions : Map[String, SecuritySchemeDefinition] = Map.empty
    , definitions         : Map[String, Model]                    = Map.empty
    , parameters          : Map[String, Parameter]                = Map.empty
    , externalDocs        : Option[ExternalDocs]                  = None
    , security            : Map[String, List[String]]             = Map.empty
    , vendorExtensions    : Map[String, Json]                      = Map.empty
    )

  implicit val swaggerEncoder: Encoder[Swagger] = semiauto.deriveEncoder

  case class Info
    (
      title            : String
    , version          : String
    , description      : Option[String]   = None
    , termsOfService   : Option[String]   = None
    , contact          : Option[Contact]  = None
    , license          : Option[License]  = None
    , vendorExtensions : Map[String, Json] = Map.empty
    )

  implicit val infoEncoder: Encoder[Info] = semiauto.deriveEncoder

  case class Contact
    (
      name  : String
    , url   : Option[String] = None
    , email : Option[String] = None
    )

  implicit val contactEncoder: Encoder[Contact] = semiauto.deriveEncoder

  case class License
    (
      name  : String
    , url   : String
    )

  implicit val licenseEncoder: Encoder[License] = semiauto.deriveEncoder

  sealed trait Scheme
  object Scheme {
    case object HTTP  extends Scheme
    case object HTTPS extends Scheme
    case object WS    extends Scheme
    case object WSS   extends Scheme
  }

  implicit val schemeEncoder: Encoder[Scheme] = implicitly[Encoder[String]].contramap(_.toString)

  sealed trait SecuritySchemeDefinition {
    def `type`: String
  }

  implicit val securitySchemeDefinitionEncoder: Encoder[SecuritySchemeDefinition] = {
    case m: OAuth2Definition => implicitly[Encoder[OAuth2Definition]].apply(m)
    case m: OAuth2VendorExtensionsDefinition => implicitly[Encoder[OAuth2VendorExtensionsDefinition]].apply(m)
    case m: ApiKeyAuthDefinition => implicitly[Encoder[ApiKeyAuthDefinition]].apply(m)
    case m: BasicAuthDefinition => implicitly[Encoder[BasicAuthDefinition]].apply(m)
  }

  case class OAuth2Definition
    (
      authorizationUrl : String
    , tokenUrl         : String
    , flow             : String
    , scopes           : Map[String, String]
    , `type`           : String = "oauth2"
    ) extends SecuritySchemeDefinition

  case class OAuth2VendorExtensionsDefinition
  (
      authorizationUrl : String
    , vendorExtensions : Map[String, Json]
    , flow             : String
    , scopes           : Map[String, String]
    , tokenUrl         : Option[String] = None
    , `type`           : String = "oauth2"
  ) extends SecuritySchemeDefinition

  case class ApiKeyAuthDefinition
  (
    name : String
    , in   : In
    , description: Option[String] = None
    , `type` : String = "apiKey"
  ) extends SecuritySchemeDefinition

  case class BasicAuthDefinition(`type`: String = "basic") extends SecuritySchemeDefinition

  sealed trait In
  object In {
    case object HEADER extends In
    case object QUERY  extends In
  }

  implicit val inEncoder: Encoder[In] = implicitly[Encoder[String]].contramap(_.toString)

  case class SecurityScope
    (
      name        : String
    , description : String
    )

  implicit val securityScopeEncoder: Encoder[SecurityScope] = semiauto.deriveEncoder

  case class Path
    (
      get              : Option[Operation] = None
    , put              : Option[Operation] = None
    , post             : Option[Operation] = None
    , delete           : Option[Operation] = None
    , patch            : Option[Operation] = None
    , options          : Option[Operation] = None
    , head             : Option[Operation] = None
    , parameters       : List[Parameter]   = Nil
    , vendorExtensions : Map[String, Json]  = Map.empty
    )

  implicit val pathEncoder: Encoder[Path] = semiauto.deriveEncoder

  case class Operation
    (
      tags             : List[String]                    = Nil
    , summary          : Option[String]                  = None
    , description      : Option[String]                  = None
    , operationId      : Option[String]                  = None
    , schemes          : List[Scheme]                    = Nil
    , consumes         : List[String]                    = Nil
    , produces         : List[String]                    = Nil
    , parameters       : List[Parameter]                 = Nil
    , responses        : Map[String, Response]           = Map.empty
    , security         : List[Map[String, List[String]]] = Nil
    , externalDocs     : Option[ExternalDocs]            = None
    , deprecated       : Boolean                         = false
    , vendorExtensions : Map[String, Json]                = Map.empty
    )

  implicit val operationEncoder: Encoder[Operation] = semiauto.deriveEncoder

  case class Response
    (
      description : String
    , schema      : Option[Model]      = None
    , examples    : Map[String, String]   = Map.empty
    , headers     : Map[String, Property] = Map.empty
    )

  implicit val responseEncoder: Encoder[Response] = semiauto.deriveEncoder

  sealed trait Model {
    def id: String
    def id2: String
    def description: Option[String]
    def properties: Map[String, Property]
    def example: Option[String]
    def externalDocs : Option[ExternalDocs]
  }

  implicit val modelEncoder: Encoder[Model] = {
    case m: ModelImpl => implicitly[Encoder[ModelImpl]].apply(m)
    case m: ArrayModel => implicitly[Encoder[ArrayModel]].apply(m)
    case m: ComposedModel => implicitly[Encoder[ComposedModel]].apply(m)
    case m: RefModel => implicitly[Encoder[RefModel]].apply(m)
  }

  case class ModelImpl
    (
      id                   : String
    , id2                  : String
    , `type`               : String
    , description          : Option[String]        = None
    , name                 : Option[String]        = None
    , required             : List[String]          = Nil
    , properties           : Map[String, Property] = Map.empty
    , example              : Option[String]        = None
    , additionalProperties : Option[Property]      = None
    , discriminator        : Option[String]        = None
    , externalDocs         : Option[ExternalDocs]  = None
    ) extends Model

  case class ArrayModel
    (
      id           : String
    , id2          : String
    , description  : Option[String]        = None
    ,`type`        : String                = "array"
    , properties   : Map[String, Property] = Map.empty
    , items        : Option[Property]      = None
    , example      : Option[String]        = None
    , externalDocs : Option[ExternalDocs]  = None
    ) extends Model

  case class ComposedModel
    (
      id           : String
    , id2          : String
    , description  : Option[String]        = None
    , allOf        : List[Model]           = Nil
    , parent       : Option[Model]         = None
    , child        : Option[Model]         = None
    , interfaces   : List[RefModel]        = Nil
    , properties   : Map[String, Property] = Map.empty
    , example      : Option[String]        = None
    , externalDocs : Option[ExternalDocs]  = None
    ) extends Model

  case class RefModel
    (
      id           : String
    , id2          : String
    , ref          : String
    , description  : Option[String]        = None
    , properties   : Map[String, Property] = Map.empty
    , example      : Option[String]        = None
    , externalDocs : Option[ExternalDocs]  = None
    ) extends Model

  sealed trait Parameter {
    def in: String
    def name: String
    def access: Option[String]
    def description: Option[String]
    def required: Boolean
    def vendorExtensions: Map[String, Json]

    def withDesc(desc: Option[String]): Parameter
  }

  implicit val parameterEncoder: Encoder[Parameter] = {
    case p: BodyParameter => implicitly[Encoder[BodyParameter]].apply(p)
    case p: NonBodyParameter => implicitly[Encoder[NonBodyParameter]].apply(p)
  }

  case class BodyParameter
    (
      name             : String
    , schema           : Option[Model]    = None
    , description      : Option[String]   = None
    , required         : Boolean          = true
    , access           : Option[String]   = None
    , vendorExtensions : Map[String, Json] = Map.empty
    , in               : String           = "body"
    ) extends Parameter{

      def withDesc(desc: Option[String]): BodyParameter = copy(description = desc)
    }

  implicit val bodyParameterEncoder: Encoder[BodyParameter] = semiauto.deriveEncoder

  case class NonBodyParameter
    (
      in: String
    , name: String
    , `type`: Option[String] = None
    , format: Option[String] = None
    , collectionFormat: Option[String] = None
    , items: Option[Property] = None
    , default: Option[Default[_]] = None
    , description: Option[String] = None
    , required: Boolean = true
    , access: Option[String] = None
    , vendorExtensions: Map[String, Json] = Map.empty
    , enums: List[String] = List.empty
    ) extends Parameter{
      def withDesc(desc: Option[String]): NonBodyParameter = copy(description = desc)
    }

  implicit val nonBodyParameterEncoder: Encoder[NonBodyParameter] = semiauto.deriveEncoder

  case class Default[T](value: T)(implicit encoder: Encoder[T]){
    def encode(): Json = encoder.apply(value)
  }

  implicit val defaultUnderscoreEncoder: Encoder[Default[_]] = d => d.encode()

  implicit def defaultEncoder[T]: Encoder[Default[T]] = d => d.encode()

  sealed trait Property {
    def `type`: String
    def required: Boolean
    def title: Option[String]
    def description: Option[String]
    def format: Option[String]

    def withRequired(required: Boolean): Property
  }

  implicit val propertyEncoder: Encoder[Property] = {
    case p: AbstractProperty => implicitly[Encoder[AbstractProperty]].apply(p)
    case p: ObjectProperty => implicitly[Encoder[ObjectProperty]].apply(p)
    case p: MapProperty => implicitly[Encoder[MapProperty]].apply(p)
    case p: ArrayProperty => implicitly[Encoder[ArrayProperty]].apply(p)
    case p: RefProperty => implicitly[Encoder[RefProperty]].apply(p)
    case p: StringProperty => implicitly[Encoder[StringProperty]].apply(p)
  }


  case class AbstractProperty
  (
    `type`        : String         = null
    , $ref        : Option[String] = None
    , required    : Boolean        = true
    , title       : Option[String] = None
    , description : Option[String] = None
    , format      : Option[String] = None
  ) extends Property {

    def withRequired(required: Boolean): AbstractProperty =
      copy(required = required)
  }

  implicit val abstractPropertyEncoder: Encoder[AbstractProperty] = semiauto.deriveEncoder

  case class ObjectProperty
  (
      required    : Boolean        = true
    , title       : Option[String] = None
    , description : Option[String] = None
    , format      : Option[String] = None
    , properties  : Map[String, Property] = Map.empty
    , `type`      : String         = "object"
  ) extends Property {

    def withRequired(required: Boolean): ObjectProperty =
      copy(required = required)
  }

  implicit val objectPropertyEncoder: Encoder[ObjectProperty] = semiauto.deriveEncoder

  case class MapProperty
  (
      additionalProperties  : Property
    , required              : Boolean        = true
    , title                 : Option[String] = None
    , description           : Option[String] = None
    , format                : Option[String] = None
    , `type`                : String         = "object"
  ) extends Property {

    def withRequired(required: Boolean): MapProperty =
      copy(required = required)
  }

  implicit val mapPropertyEncoder: Encoder[MapProperty] = semiauto.deriveEncoder

  case class ArrayProperty
  (
      items       : Property
    , uniqueItems : Boolean        = false
    , required    : Boolean        = true
    , title       : Option[String] = None
    , description : Option[String] = None
    , format      : Option[String] = None
    , `type`      : String         = "array"
  ) extends Property {

    def withRequired(required: Boolean): ArrayProperty =
      this.copy(required = required)
  }

  implicit val arrayPropertyEncoder: Encoder[ArrayProperty] = semiauto.deriveEncoder

  case class RefProperty
  (
      ref         : String
    , required    : Boolean        = true
    , title       : Option[String] = None
    , description : Option[String] = None
    , format      : Option[String] = None
    , `type`      : String         = "ref"
  ) extends Property {

    def withRequired(required: Boolean): RefProperty =
      copy(required = required)
  }

  implicit val refPropertyEncoder: Encoder[RefProperty] = semiauto.deriveEncoder

  case class StringProperty
    (
      title       : Option[String] = None
    , description : Option[String] = None
    , format      : Option[String] = None
    , required    : Boolean        = true
    , enums       : Set[String]
    , minLength   : Option[Int]    = None
    , maxLength   : Option[Int]    = None
    , pattern     : Option[String] = None
    , default     : Option[String] = None
    , `type`      : String         = "string"
    ) extends Property {

    def withRequired(required: Boolean): StringProperty =
      copy(required = required)
  }

  implicit val stringPropertyEncoder: Encoder[StringProperty] = semiauto.deriveEncoder

  case class ExternalDocs
    (
      description : String
    , url         : String
    )

  implicit val externalDocsEncoder: Encoder[ExternalDocs] = semiauto.deriveEncoder
}
