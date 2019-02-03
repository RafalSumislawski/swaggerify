package swaggerify

import magnolia._
import swaggerify.TypeExtensions._
import swaggerify.models.{NonBodyParameter, _}

import scala.language.experimental.macros
import scala.reflect.runtime.universe._

trait Swaggerify[T] {
  // TODO consider modeling these pairs of methods differently
  def asProperty: Property
  /** If asProperty is used, these are the models it refers to */
  def propertyDependencies: Set[Model]

  def genPropertyDeps: Set[Swaggerify[_]] => Set[Model]

  def asModel: Option[Model] // TODO should this be an Option?
  /** If asModel is used, these are the models it refers to */
  def modelDependencies: Set[Model]

  def genModelDeps: Set[Swaggerify[_]] => Set[Model]

  def asBodyParameter(name: String = "body", description: Option[String] = None): BodyParameter
  /** If asBodyParameter is used, these are the models it refers to */
  def bodyParameterDependencies: Set[Model]

  def asCookieParameter(name: String, description: Option[String] = None): NonBodyParameter
  def asFormParameter(name: String, description: Option[String] = None): NonBodyParameter
  def asHeaderParameter(name: String, description: Option[String] = None, hasDefault: Boolean = false): NonBodyParameter
  def asPathParameter(name: String, description: Option[String] = None): NonBodyParameter
  def asQueryParameter(name: String, description: Option[String] = None, default: Option[T] = None): NonBodyParameter
  // as*Parameter should not refer to any models. so there's no *Dependencies method for them

  def usingRefModel(): Swaggerify[T]
}

// TODO consider distinguishing simple types to put a limitation on what can be used as non-body parameters
class Swg[T](asProp: => Property, model: => Option[Model], override val genModelDeps: Set[Swaggerify[_]] => Set[Model]) extends Swaggerify[T] {

  override lazy val asProperty: Property = asProp

  override lazy val asModel: Option[Model] = model

  override lazy val modelDependencies: Set[Model] = genModelDeps(Set.empty)

  // TODO consider enforcing ref model to refer to the asModel. This is now an implicit assumption.
  override def propertyDependencies: Set[Model] = genPropertyDeps(Set.empty)

  override def genPropertyDeps: Set[Swaggerify[_]] => Set[Model] = alreadyKnown => {
    val alreadyKnown2 = alreadyKnown + this
    if (asProperty.isInstanceOf[RefProperty] && !asModel.exists(_.isInstanceOf[RefModel])) genModelDeps(alreadyKnown2) ++ asModel
    else genModelDeps(alreadyKnown2)
  }

  override def asBodyParameter(name: String = "body", description: Option[String] = None): BodyParameter =
    BodyParameter(name = name, description = description, schema = asModel)

  override def bodyParameterDependencies: Set[Model] = modelDependencies

  override def asCookieParameter(name: String, description: Option[String] = None): NonBodyParameter =
    asParameter("cookie", name, description = description, default = None)

  override def asFormParameter(name: String, description: Option[String] = None): NonBodyParameter =
    asParameter("formData", name, description = description, default = None).copy(collectionFormat = Some("multi"))

  override def asHeaderParameter(name: String, description: Option[String] = None, hasDefault: Boolean = false): NonBodyParameter =
    asParameter("header", name, description = None, default = None).copy(required = !hasDefault)

  override def asPathParameter(name: String, description: Option[String] = None): NonBodyParameter =
    asParameter("path", name, description, default = None).copy(required = true)

  override def asQueryParameter(name: String, description: Option[String] = None, default: Option[T] = None): NonBodyParameter =
    asParameter("query", name, description, default).copy(collectionFormat = Some("multi"))

  def asParameter(in: String, name: String, description: Option[String], default: Option[T]): NonBodyParameter = {
    val base = NonBodyParameter(in = in, name = name, description = description,
      default = default,
      `type` = Some(asProperty.`type`),
      format = asProperty.format /*rho doesn't do that*/ ,
      required = asProperty.required
    )

    asProperty match {
      // here rho handles more for query params and less for other params
      case _: AbstractProperty => base
      case p: StringProperty => base.copy(enums = p.enums.toList)
      case p: ArrayProperty => base.copy(items = Some(p.items))
      // According to the specs the `type` of a non-body parameter "... MUST be one of "string", "number", "integer", "boolean", "array" or "file"."
      case _ => throw new UnsupportedOperationException("A non-body parameter can only be a of simple type (\"string\", \"number\", \"integer\", \"boolean\", \"array\" or \"file\") and should be represented by an AbstractProperty, a StringProperty or an ArrayProperty")
    }
  }

  def usingRefModel(): Swaggerify[T] = {
    if (asModel.exists(_.isInstanceOf[RefModel])) this
    else Swg(asProperty, asModel.map(m => RefModel(s"${m.id}Ref", s"${m.id2}Ref", ref = m.id2)), _ => asModel.toSet ++ modelDependencies)
  }
}

object Swg{
  @inline def apply[T](asProperty: Property, model: => Option[Model], genModelDeps: Set[Swaggerify[_]] => Set[Model]): Swg[T] = new Swg[T](asProperty, model, genModelDeps)
}

object Swaggerify {

  @inline def apply[T: Swaggerify]: Swaggerify[T] = implicitly[Swaggerify[T]]

  implicit val swaggerifyUnit: Swaggerify[Unit] = swaggerifyAsEmptyObject
  implicit val swaggerifyVoid: Swaggerify[java.lang.Void] = swaggerifyAsEmptyObject
  implicit val swaggerifyNull: Swaggerify[Null] = swaggerifyAsEmptyObject("scala.Null", "Nul") // "Null" can't be used in YAML
  implicit val swaggerifyNothing: Swaggerify[Nothing] = swaggerifyAsEmptyObject

  def swaggerifyAsEmptyObject[T](implicit tt: TypeTag[T]): Swaggerify[T] =
    swaggerifyAsEmptyObject(tt.tpe.fullName, tt.tpe.simpleName)

  def swaggerifyAsEmptyObject[T](fullTypeName: String, simpleTypeName: String): Swaggerify[T] = {
    val model = ModelImpl(id = fullTypeName, id2 = simpleTypeName, description = Some(simpleTypeName), `type` = Some("object"))
    Swg(RefProperty(simpleTypeName), Some(model), _ => Set.empty)
  }

  implicit val swaggerifyString: Swaggerify[String] = swaggerifyAsSimpleType("string")
  implicit val swaggerifyCharSequence: Swaggerify[CharSequence] = swaggerifyAsSimpleType("string")
  implicit val swaggerifyChar: Swaggerify[Char] = swaggerifyAsSimpleType("string", None, Some("char"))
  implicit val swaggerifyJChar: Swaggerify[java.lang.Character] = swaggerifyAsSimpleType("string", None, Some("char"))
  implicit val swaggerifyByte: Swaggerify[Byte] = swaggerifyAsSimpleType("integer", Some("int32"), Some("byte"))
  implicit val swaggerifyBJyte: Swaggerify[java.lang.Byte] = swaggerifyAsSimpleType("integer", Some("int32"), Some("byte"))
  implicit val swaggerifyShort: Swaggerify[Short] = swaggerifyAsSimpleType("integer", Some("int32"), Some("short"))
  implicit val swaggerifyJShort: Swaggerify[java.lang.Short] = swaggerifyAsSimpleType("integer", Some("int32"), Some("short"))
  implicit val swaggerifyInt: Swaggerify[Int] = swaggerifyAsSimpleType("integer", Some("int32"))
  implicit val swaggerifyJInt: Swaggerify[java.lang.Integer] = swaggerifyAsSimpleType("integer", Some("int32"))
  implicit val swaggerifyLong: Swaggerify[Long] = swaggerifyAsSimpleType("integer", Some("int64"))
  implicit val swaggerifyJLong: Swaggerify[java.lang.Long] = swaggerifyAsSimpleType("integer", Some("int64"))
  implicit val swaggerifyBigInt: Swaggerify[BigInt] = swaggerifyAsSimpleType("integer")
  implicit val swaggerifyBigInteger: Swaggerify[java.math.BigInteger] = swaggerifyAsSimpleType("integer")
  implicit val swaggerifyFloat: Swaggerify[Float] = swaggerifyAsSimpleType("number", Some("float"))
  implicit val swaggerifyJFloat: Swaggerify[java.lang.Float] = swaggerifyAsSimpleType("number", Some("float"))
  implicit val swaggerifyDouble: Swaggerify[Double] = swaggerifyAsSimpleType("number", Some("double"))
  implicit val swaggerifyJDouble: Swaggerify[java.lang.Double] = swaggerifyAsSimpleType("number", Some("double"))
  implicit val swaggerifyBigDecimal: Swaggerify[BigDecimal] = swaggerifyAsSimpleType("number")
  implicit val swaggerifyJBigDecimal: Swaggerify[java.math.BigDecimal] = swaggerifyAsSimpleType("number")
  implicit val swaggerifyBoolean: Swaggerify[Boolean] = swaggerifyAsSimpleType("boolean")
  implicit val swaggerifyJBoolean: Swaggerify[java.lang.Boolean] = swaggerifyAsSimpleType("boolean")

  // LocalDate and OffsetDateTime are the java types that match the swagger definitions. see:
  // https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md
  // https://xml2rfc.tools.ietf.org/public/rfc/html/rfc3339.html#anchor14
  // Other java.time types can be added by users as needed.
  implicit val swaggerifyLocalDate: Swaggerify[java.time.LocalDate] = swaggerifyAsSimpleType("string", Some("date"), Some("java.time.LocalDate"))
  implicit val swaggerifyOffsetDateTime: Swaggerify[java.time.OffsetDateTime] = swaggerifyAsSimpleType("string", Some("date-time"), Some("java.time.OffsetDateTime"))

  //  implicit def swaggerifyFileResponse[T]: Swaggerify[SwaggerFileResponse[T]] = swaggerifyAsSimpleType("file")

  def swaggerifyAsSimpleType[T](`type`: String, format: Option[String] = None, description: Option[String] = None): Swaggerify[T] =
    Swg(
      AbstractProperty(`type` = `type`, format = format),
      Some(ModelImpl(id = `type`, id2 = `type`, `type` = Some(`type`), format = format, description = description, isSimple = true)),
      _ => Set.empty
    )

  implicit def swaggerifyOption[T: Swaggerify]: Swaggerify[Option[T]] =
    Swg(Swaggerify[T].asProperty.withRequired(false), Swaggerify[T].asModel, Swaggerify[T].genModelDeps)

  implicit def swaggerifyOptional[T: Swaggerify]: Swaggerify[java.util.Optional[T]] =
    Swg(Swaggerify[T].asProperty.withRequired(false), Swaggerify[T].asModel, Swaggerify[T].genModelDeps)

  implicit def swaggerifySet[I: Swaggerify]: Swaggerify[Set[I]] = swaggerifyAsArray[Set[I], I](uniqueItems = true)

  implicit def swaggerifyJSet[I: Swaggerify]: Swaggerify[java.util.Set[I]] = swaggerifyAsArray[java.util.Set[I], I](uniqueItems = true)

  implicit def swaggerifyArray[I: Swaggerify]: Swaggerify[Array[I]] = swaggerifyAsArray[Array[I], I]()

  implicit def swaggerifySeq[I: Swaggerify]: Swaggerify[Seq[I]] = swaggerifyAsArray[Seq[I], I]()

  implicit def swaggerifyIndexedSeq[I: Swaggerify]: Swaggerify[IndexedSeq[I]] = swaggerifyAsArray[IndexedSeq[I], I]()

  implicit def swaggerifyList[I: Swaggerify]: Swaggerify[List[I]] = swaggerifyAsArray[List[I], I]()

  implicit def swaggerifyVector[I: Swaggerify]: Swaggerify[Vector[I]] = swaggerifyAsArray[Vector[I], I]()

  implicit def swaggerifyJList[I: Swaggerify]: Swaggerify[java.util.List[I]] = swaggerifyAsArray[java.util.List[I], I]()

  implicit def swaggerifyJArrayList[I: Swaggerify]: Swaggerify[java.util.ArrayList[I]] = swaggerifyAsArray[java.util.ArrayList[I], I]()

  implicit def swaggerifyJLinkedList[I: Swaggerify]: Swaggerify[java.util.LinkedList[I]] = swaggerifyAsArray[java.util.LinkedList[I], I]()

  def swaggerifyAsArray[T, I: Swaggerify](uniqueItems: Boolean = false): Swaggerify[T] =
    Swg(
      ArrayProperty(Swaggerify[I].asProperty, uniqueItems = uniqueItems),
      Some(ArrayModel(id = null, id2 = null, `type` = Some("array"), items = Some(Swaggerify[I].asProperty))), // TODO I don't care about these nulls as this model shouldn't end up in modelsSets, but a cleaner solution would be nice.
      Swaggerify[I].genPropertyDeps
    )

  implicit def swaggerifyStringMap[I: Swaggerify]: Swaggerify[Map[String, I]] = swaggerifyAsMap[Map[String, I], I]

  implicit def swaggerifyStringJMap[I: Swaggerify]: Swaggerify[java.util.Map[String, I]] = swaggerifyAsMap[java.util.Map[String, I], I]

  implicit def swaggerifyStringToAnyRefMap: Swaggerify[Map[String, AnyRef]] = swaggerifyAsMap[Map[String, AnyRef], AnyRef](swaggerifyAnyRef)

  implicit def swaggerifyStringToAnyRefJMap: Swaggerify[java.util.Map[String, AnyRef]] = swaggerifyAsMap[java.util.Map[String, AnyRef], AnyRef](swaggerifyAnyRef)

  val swaggerifyAnyRef: Swaggerify[AnyRef] = Swg(AbstractProperty("object"), None, _ => Set.empty)

  def swaggerifyAsMap[T, I: Swaggerify]: Swaggerify[T] =
    Swg(
      MapProperty(Swaggerify[I].asProperty),
      Some(ModelImpl(id = null, id2 = null, `type` = Some("object"), additionalProperties = Some(Swaggerify[I].asProperty))), // TODO I don't care about these nulls as this model shouldn't end up in modelsSets, but a cleaner solution would be nice.
      Swaggerify[I].genPropertyDeps
    )

  // consider excluding it from the default implicits as there are many reasonable ways to encode an either.
  implicit def swaggerifyEither[L: Swaggerify, R: Swaggerify]: Swaggerify[Either[L, R]] = {
    // I don't like what's happening here with the ids.
    val id2 = s"Either[${Swaggerify[L].asProperty.`type`}, ${Swaggerify[R].asProperty.`type`}]"
    val model = ModelImpl(
      id = s"scala.$id2",
      id2 = id2,
      description = Some(id2),
      `type` = Some("object"),
      properties = Map("left" -> Swaggerify[L].asProperty.withRequired(false), "right" -> Swaggerify[R].asProperty.withRequired(false)) // FIXME strange things will happen with Either[Option[L], R] etc.
    )
    Swg(RefProperty(model.id2), Some(model), alreadyKnown => Swaggerify[L].genPropertyDeps(alreadyKnown) ++ Swaggerify[R].genPropertyDeps(alreadyKnown))
  }

  // TODO consider:
  // Effect
  // Stream
  // Either, Try

  type Typeclass[T] = Swaggerify[T]

  def combine[T](ctx: CaseClass[Swaggerify, T]): Swaggerify[T] = {
    if (ctx.isValueClass) {
      ctx.parameters.head.typeclass.asInstanceOf[Swaggerify[T]]
    } else if (ctx.isObject) {
      swaggerifyAsEmptyObject[T](ctx.typeName.full, ctx.typeName.short)
    } else {
      def model = ModelImpl(id = ctx.typeName.full, id2 = ctx.typeName.short,
        description = Some(ctx.typeName.short),
        `type` = Some("object"),
        properties = ctx.parameters.map(param => param.label -> param.typeclass.asProperty).toMap
      )

      def modelSet(alreadyKnown: Set[Swaggerify[_]]) = {
        ctx.parameters.foldLeft(Set.empty[Model])((acc, param) =>
          if(alreadyKnown.contains(param.typeclass)) acc
          else acc ++ param.typeclass.genPropertyDeps(alreadyKnown)
        )
      }

      Swg(RefProperty(ctx.typeName.short), Some(model), modelSet)
    }
  }

  def dispatch[T](ctx: SealedTrait[Swaggerify, T]): Swaggerify[T] = {
    if (ctx.subtypes.forall(_.typeName.short.endsWith("$"))) { // a sealed trait of object is handled as an enum
      // Currently the model for it is a None. This will be a problem in case of two layer of traits.
      // It could be modeled as a simple string or as inheritance with empty objects as values.
      Swg(StringProperty(enums = ctx.subtypes.map(_.typeName.short).toSet), None, _ => Set.empty)
    } else { // a sealed trait with at least one case class is handled as an inheritance hierarchy
      val discriminatorName = "type" // TODO support for @DiscriminatorField
      val discriminatorValues = ctx.subtypes.map(_.typeclass.asModel.get.id2).toSet
      val ownModel = ModelImpl(id = ctx.typeName.full, id2 = ctx.typeName.short,
        description = Some(ctx.typeName.short),
        `type` = Some("object"),
        discriminator = Some(discriminatorName),
        properties = Map(discriminatorName -> StringProperty(enums = discriminatorValues))
      )

      val ownModelRef = RefModel(ownModel.id, ownModel.id2, ownModel.id2)

      def subTypesModels = ctx.subtypes.map { sub =>
        val subModel = sub.typeclass.asModel.get
        ComposedModel(id = subModel.id, id2 = subModel.id2,
          description = subModel.description,
          allOf = List(ownModelRef, subModel),
          parent = Some(ownModelRef)
        )
      }

      def modelSet(alreadyKnown: Set[Swaggerify[_]]) = ctx.subtypes.map(_.typeclass.genModelDeps(alreadyKnown)).fold(Set())(_ ++ _) ++ subTypesModels

      Swg(RefProperty(ownModel.id2), Some(ownModel), modelSet)
    }
  }

  implicit def gen[T]: Swaggerify[T] = macro Magnolia.gen[T]
}
