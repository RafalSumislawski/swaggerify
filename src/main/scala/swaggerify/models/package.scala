package swaggerify.models

import java.util.ArrayList

import _root_.swaggerify.models.JValue._
import io.swagger.{models => jm}

import scala.collection.JavaConverters._

object `package` {

  case class Swagger
  (
      info                : Info
    , swagger             : String                                = "2.0"
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
    , security            : List[SecurityRequirement]             = Nil
    , vendorExtensions    : Map[String, Any]                      = Map.empty
  ) {

    def toJModel: jm.Swagger = {
      val s = new jm.Swagger

      s.info(info.toJModel)
      s.host(fromOption(host))
      s.basePath(fromOption(basePath))
      s.setSchemes(fromList(schemes.map(_.toJModel)))
      s.setConsumes(fromList(consumes))
      s.setProduces(fromList(produces))
      s.setPaths(fromMap(paths.mapValues(_.toJModel)))
      s.setSecurity(fromList(security.map(_.toJModel)))
      s.setSecurityDefinitions(fromMap(securityDefinitions.mapValues(_.toJModel)))
      s.setDefinitions(fromMap(definitions.mapValues(_.toJModel)))
      s.setParameters(fromMap(parameters.mapValues(_.toJModel)))
      s.setExternalDocs(fromOption(externalDocs.map(_.toJModel)))
      vendorExtensions.foreach {
        case (key, value:Map[_,_]) => s.setVendorExtension(key, fromMap(value))
        case (key, value:Option[_]) => s.setVendorExtension(key, fromOption(value))
        case (key, value:List[_]) => s.setVendorExtension(key, fromList(value))
        case (key, value) => s.setVendorExtension(key, value)
      }
      s
    }
  }

  case class Info
  (
    title            : String
    , version          : String
    , description      : Option[String]   = None
    , termsOfService   : Option[String]   = None
    , contact          : Option[Contact]  = None
    , license          : Option[License]  = None
    , vendorExtensions : Map[String, Any] = Map.empty
  ) {

    def toJModel: jm.Info = {
      val i = new jm.Info
      i.title(title)
        .version(version)
        .description(fromOption(description))
        .termsOfService(fromOption(termsOfService))
        .contact(fromOption(contact.map(_.toJModel)))
        .license(fromOption(license.map(_.toJModel)))
      vendorExtensions.foreach { case (key, value) => i.setVendorExtension(key, value) }
      i
    }
  }

  case class Contact
  (
    name  : String
    , url   : Option[String] = None
    , email : Option[String] = None
  ) {

    def toJModel: jm.Contact =
      (new jm.Contact).name(name).url(fromOption(url)).email(fromOption(email))
  }

  case class License
  (
    name  : String
    , url   : String
  ) {

    def toJModel: jm.License =
      (new jm.License).name(name).url(url)
  }

  sealed trait Scheme {
    def toJModel: jm.Scheme
  }
  object Scheme {
    object HTTP  extends Scheme { def toJModel: jm.Scheme = jm.Scheme.HTTP }
    object HTTPS extends Scheme { def toJModel: jm.Scheme = jm.Scheme.HTTPS }
    object WS    extends Scheme { def toJModel: jm.Scheme = jm.Scheme.WS }
    object WSS   extends Scheme { def toJModel: jm.Scheme = jm.Scheme.WSS }
  }

  sealed trait SecuritySchemeDefinition {
    def `type`: String

    def toJModel: jm.auth.SecuritySchemeDefinition
  }

  case class OAuth2Definition
  (
    authorizationUrl : String
    , tokenUrl         : String
    , flow             : String
    , scopes           : Map[String, String]
  ) extends SecuritySchemeDefinition {

    override val `type` = "oauth2"

    def toJModel: jm.auth.OAuth2Definition = {
      val oa2d = new jm.auth.OAuth2Definition
      oa2d.setAuthorizationUrl(authorizationUrl)
      oa2d.setTokenUrl(tokenUrl)
      oa2d.setFlow(flow)
      oa2d.setScopes(fromMap(scopes))
      oa2d
    }
  }

  case class OAuth2VendorExtensionsDefinition
  (
    authorizationUrl : String
    , vendorExtensions : Map[String, AnyRef]
    , flow             : String
    , scopes           : Map[String, String]
    , tokenUrl         : Option[String] = None
  ) extends SecuritySchemeDefinition {

    override val `type` = "oauth2"

    def toJModel: jm.auth.OAuth2Definition = {
      val oa2d = new jm.auth.OAuth2Definition
      oa2d.setAuthorizationUrl(authorizationUrl)
      oa2d.setVendorExtensions(fromMap(vendorExtensions))
      oa2d.setFlow(flow)
      oa2d.setScopes(fromMap(scopes))

      if(tokenUrl.isDefined)
        oa2d.setTokenUrl(tokenUrl.get)

      oa2d
    }
  }

  case class ApiKeyAuthDefinition
  (
    name : String
    , in   : In
    , description: Option[String] = None
  ) extends SecuritySchemeDefinition {

    override val `type` = "apiKey"

    def toJModel: jm.auth.ApiKeyAuthDefinition = {
      val akad  = new jm.auth.ApiKeyAuthDefinition
      val definition = akad.name(name).in(in.toJModel)
      description.foreach(definition.setDescription)
      definition
    }
  }

  case object BasicAuthDefinition extends SecuritySchemeDefinition {
    override val `type` = "basic"

    def toJModel: jm.auth.BasicAuthDefinition =
      new jm.auth.BasicAuthDefinition
  }

  sealed trait In {
    def toJModel: jm.auth.In
  }
  object In {
    case object HEADER extends In { def toJModel: jm.auth.In = jm.auth.In.HEADER }
    case object QUERY  extends In { def toJModel: jm.auth.In = jm.auth.In.QUERY  }
  }

  case class SecurityRequirement
  (
    name   : String
    , scopes : List[String]
  ) {

    def toJModel: jm.SecurityRequirement = {
      val sr = new jm.SecurityRequirement
      sr.setRequirements(name, scopes.asJava)
      sr
    }
  }

  case class SecurityScope
  (
    name        : String
    , description : String
  ) {

    def toJModel: jm.SecurityScope =
      new jm.SecurityScope(name, description)
  }

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
    , vendorExtensions : Map[String, Any]  = Map.empty
  ) {

    def operations: Seq[Operation] =
      get.toList ++
        put.toList ++
        post.toList ++
        delete.toList ++
        patch.toList ++
        options.toList ++
        head.toList

    def toJModel: jm.Path = {
      val p = new jm.Path
      p.setGet(fromOption(get.map(_.toJModel)))
      p.setPut(fromOption(put.map(_.toJModel)))
      p.setPost(fromOption(post.map(_.toJModel)))
      p.setDelete(fromOption(delete.map(_.toJModel)))
      p.setPatch(fromOption(patch.map(_.toJModel)))
      p.setOptions(fromOption(options.map(_.toJModel)))
      p.setHead(fromOption(head.map(_.toJModel)))
      p.setParameters(fromList(parameters.map(_.toJModel)))
      vendorExtensions.foreach { case (key, value) => p.setVendorExtension(key, value) }
      p
    }
  }

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
    , deprecated       : Option[Boolean]                 = None
    , vendorExtensions : Map[String, Any]                = Map.empty
  ) {

    def toJModel: jm.Operation = {
      val o = new jm.Operation
      o.setTags(fromList(tags))
      o.setSummary(fromOption(summary))
      o.setDescription(fromOption(description))
      o.setOperationId(fromOption(operationId))
      o.setSchemes(fromList(schemes.map(_.toJModel)))
      o.setConsumes(fromList(consumes))
      o.setProduces(fromList(produces))
      o.setParameters(fromList(parameters.map(_.toJModel)))
      o.setResponses(fromMap(responses.mapValues(_.toJModel)))
      o.setSecurity(fromList(security.map { m =>
        m.mapValues(_.asJava).asJava
      }))
      o.setExternalDocs(fromOption(externalDocs.map(_.toJModel)))
      o.setDeprecated(fromOption(deprecated.map(_.asInstanceOf[java.lang.Boolean])))
      vendorExtensions.foreach { case (key, value) => o.setVendorExtension(key, value) }
      o
    }
  }

  case class Response
  (
    description : String
    , schema      : Option[Model]      = None
    , examples    : Map[String, String]   = Map.empty
    , headers     : Map[String, Property] = Map.empty
  ) {

    def toJModel: jm.Response = {
      val r = new jm.Response
      r.setDescription(description)
      r.setResponseSchema(fromOption(schema.map(_.toJModel)))
      r.setExamples(fromMap(examples))
      r.setHeaders(fromMap(headers.mapValues(_.toJModel)))
      r
    }
  }

  sealed trait Model {
    def id: String
    def id2: String
    def description: Option[String]
    def properties: Map[String, Property]
    def example: Option[String]
    def externalDocs : Option[ExternalDocs]

    def toJModel: jm.Model
  }

  case class ModelImpl
  (
    id                   : String
    , id2                  : String
    , description          : Option[String]        = None
    , `type`               : Option[String]        = None
    , name                 : Option[String]        = None
    , required             : List[String]          = Nil
    , properties           : Map[String, Property] = Map.empty
    , isSimple             : Boolean               = false
    , example              : Option[String]        = None
    , additionalProperties : Option[Property]      = None
    , discriminator        : Option[String]        = None
    , externalDocs         : Option[ExternalDocs]  = None
  ) extends Model {

    def toJModel: jm.Model = {
      val m = new jm.ModelImpl
      m.setType(fromOption(`type`))
      m.setName(fromOption(name))
      m.setDescription(fromOption(description))
      m.setRequired(required.asJava)
      m.setExample(fromOption(example))
      m.setProperties(fromMap(properties.mapValues(_.toJModel)))
      if (additionalProperties.nonEmpty) m.setAdditionalProperties(fromOption(additionalProperties.map(_.toJModel)))
      m.setDiscriminator(fromOption(discriminator))
      m.setExternalDocs(fromOption(externalDocs.map(_.toJModel)))
      m
    }
  }

  case class ArrayModel
  (
    id           : String
    , id2          : String
    , description  : Option[String]        = None
    ,`type`        : Option[String]        = None
    , properties   : Map[String, Property] = Map.empty
    , items        : Option[Property]      = None
    , example      : Option[String]        = None
    , externalDocs : Option[ExternalDocs]  = None
  ) extends Model {

    def toJModel: jm.Model = {
      val am = new jm.ArrayModel
      am.setType(fromOption(`type`))
      am.setDescription(fromOption(description))
      am.setProperties(fromMap(properties.mapValues(_.toJModel)))
      am.setItems(fromOption(items.map(_.toJModel)))
      am.setExample(fromOption(example))
      am.setExternalDocs(fromOption(externalDocs.map(_.toJModel)))
      am
    }
  }

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
  ) extends Model {

    def toJModel: jm.Model = {
      val cm = new jm.ComposedModel
      cm.setDescription(fromOption(description))
      cm.setAllOf(new ArrayList(allOf.map(_.toJModel).asJava))
      parent.map(_.toJModel).foreach(p => cm.setParent(p))
      child.map(_.toJModel).foreach(c => cm.setChild(c))
      cm.setInterfaces(interfaces.map(_.toJModel.asInstanceOf[jm.RefModel]).asJava)
      cm.setProperties(properties.mapValues(_.toJModel).asJava)
      cm.setExample(fromOption(example))
      cm.setExternalDocs(fromOption(externalDocs.map(_.toJModel)))
      cm
    }
  }

  case class RefModel
  (
    id           : String
    , id2          : String
    , ref          : String
    , description  : Option[String]        = None
    , properties   : Map[String, Property] = Map.empty
    , example      : Option[String]        = None
    , externalDocs : Option[ExternalDocs]  = None
  ) extends Model {

    def toJModel: jm.Model = {
      val rm = new jm.RefModel(ref)
      rm.setDescription(fromOption(description))
      rm.setProperties(fromMap(properties.mapValues(_.toJModel)))
      rm.setExample(fromOption(example))
      rm.setExternalDocs(fromOption(externalDocs.map(_.toJModel)))
      rm
    }
  }

  sealed trait Property {
    def `type`: String
    def required: Boolean
    def title: Option[String]
    def description: Option[String]
    def format: Option[String]

    def withRequired(required: Boolean): Property
    def toJModel: jm.properties.Property
  }

  case class AbstractProperty
  (
    `type`      : String         = null
    , $ref        : Option[String] = None
    , required    : Boolean        = false
    , title       : Option[String] = None
    , description : Option[String] = None
    , format      : Option[String] = None
  ) extends Property {

    class RefProperty extends jm.properties.AbstractProperty {
      protected var $ref: String = _

      def $ref($ref: String): RefProperty = {
        this.set$ref($ref)
        this
      }

      def get$ref(): String = $ref
      def set$ref($ref: String): Unit = { this.$ref = $ref }
    }

    def withRequired(required: Boolean): AbstractProperty =
      copy(required = required)

    def toJModel: jm.properties.Property = {
      val ap = new RefProperty
      ap.setType(`type`)
      ap.set$ref(fromOption($ref))
      ap.setRequired(required)
      ap.setTitle(fromOption(title))
      ap.setDescription(fromOption(description))
      ap.setFormat(fromOption(format))
      ap
    }
  }

  case class ObjectProperty
  (
    required    : Boolean        = false
    , title       : Option[String] = None
    , description : Option[String] = None
    , format      : Option[String] = None
    , properties  : Map[String, Property] = Map.empty
  ) extends Property {

    override val `type` = "object"

    def withRequired(required: Boolean): ObjectProperty =
      copy(required = required)

    def toJModel: jm.properties.Property = {
      val ap = new jm.properties.ObjectProperty
      ap.setType(`type`)
      ap.setRequired(required)
      ap.setTitle(fromOption(title))
      ap.setDescription(fromOption(description))
      ap.setFormat(fromOption(format))
      ap.setProperties(fromMap(properties.mapValues(_.toJModel)))
      ap
    }
  }

  case class MapProperty
  (
    additionalProperties  : Property
    , required              : Boolean        = false
    , title                 : Option[String] = None
    , description           : Option[String] = None
    , format                : Option[String] = None
  ) extends Property {

    override val `type` = "object"

    def withRequired(required: Boolean): MapProperty =
      copy(required = required)

    def toJModel: jm.properties.Property = {
      val ap = new jm.properties.MapProperty
      ap.setType(`type`)
      ap.setRequired(required)
      ap.setTitle(fromOption(title))
      ap.setDescription(fromOption(description))
      ap.setFormat(fromOption(format))
      ap.setAdditionalProperties(additionalProperties.toJModel)
      ap
    }
  }

  case class ArrayProperty
  (
    items       : Property
    , uniqueItems : Boolean        = false
    , required    : Boolean        = true
    , title       : Option[String] = None
    , description : Option[String] = None
    , format      : Option[String] = None
  ) extends Property {

    override val `type` = "array"

    def withRequired(required: Boolean): ArrayProperty =
      this.copy(required = required)

    def toJModel: jm.properties.Property = {
      val ap = new jm.properties.ArrayProperty
      ap.setItems(items.toJModel)
      ap.setUniqueItems(uniqueItems)
      ap.setRequired(required)
      ap.setTitle(fromOption(title))
      ap.setDescription(fromOption(description))
      ap.setFormat(fromOption(format))
      ap
    }
  }

  case class RefProperty
  (
    ref         : String
    , required    : Boolean        = false
    , title       : Option[String] = None
    , description : Option[String] = None
    , format      : Option[String] = None
  ) extends Property {

    override val `type` = "ref"

    def withRequired(required: Boolean): RefProperty =
      copy(required = required)

    def toJModel: jm.properties.Property = {
      val rp = new jm.properties.RefProperty(ref)
      rp.setRequired(required)
      rp.setTitle(fromOption(title))
      rp.setDescription(fromOption(description))
      rp.setFormat(fromOption(format))
      rp
    }
  }

  case class StringProperty
  (
    title       : Option[String] = None
    , description : Option[String] = None
    , format      : Option[String] = None
    , required: Boolean = false
    , enums: Set[String]
    , minLength: Option[Int] = None
    , maxLength: Option[Int] = None
    , pattern: Option[String] = None
    , default: Option[String] = None
  ) extends Property {
    override val `type` = "string"

    def withRequired(required: Boolean): StringProperty =
      copy(required = required)

    def toJModel: jm.properties.Property = {
      val sp = new jm.properties.StringProperty()
      sp.setRequired(required)
      sp.setTitle(fromOption(title))
      sp.setDescription(fromOption(description))
      sp.setFormat(fromOption(format))
      sp.setEnum(fromList(enums.toList))
      minLength.foreach(l => sp.setMinLength(Integer.valueOf(l)))
      maxLength.foreach(l => sp.setMaxLength(Integer.valueOf(l)))
      sp.setPattern(fromOption(pattern))
      sp.setDefault(fromOption(default))
      sp
    }
  }

  case class ExternalDocs
  (
    description : String
    , url         : String
  ) {

    def toJModel: jm.ExternalDocs = {
      val ed = new jm.ExternalDocs
      ed.setDescription(description)
      ed.setUrl(url)
      ed
    }
  }
}
