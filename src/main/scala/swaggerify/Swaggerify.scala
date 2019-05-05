package swaggerify

import magnolia._
import swaggerify.models.{NonBodyParameter, _}

import scala.language.experimental.macros
import scala.reflect.runtime.universe._

trait Swaggerify[T] {
  // TODO consider modeling these pairs of methods differently
  def asProperty: Property
  /** If asProperty is used, these are the models it refers to */
  def propertyDependencies: Set[Model]

  def genPropertyDeps: Set[Swaggerify[_]] => Set[Model]

  def asModel: Model
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

  def isEmptyObject: Boolean
}

// TODO consider distinguishing simple types to put a limitation on what can be used as non-body parameters
class Swg[T](asProp: => Property,
             model: => Model,
             override val genModelDeps: Set[Swaggerify[_]] => Set[Model],
             override val isEmptyObject: Boolean = false
            ) extends Swaggerify[T] {

  override lazy val asProperty: Property = asProp

  override lazy val asModel: Model = model

  override lazy val modelDependencies: Set[Model] = genModelDeps(Set.empty)

  override def propertyDependencies: Set[Model] = genPropertyDeps(Set.empty)

  // TODO consider enforcing ref model to refer to the asModel. This is now an implicit assumption.
  override def genPropertyDeps: Set[Swaggerify[_]] => Set[Model] = alreadyKnown => {
    val alreadyKnown2 = alreadyKnown + this
    if (asProperty.isInstanceOf[RefProperty] && !asModel.isInstanceOf[RefModel]) genModelDeps(alreadyKnown2) + asModel
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
    if (asModel.isInstanceOf[RefModel]) this
    else Swg(asProperty, RefModel(asModel.id.withSuffix("Ref"), ref = asModel.id.shortId), _ => modelDependencies + asModel)
  }
}

object Swg {
  @inline def apply[T](asProperty: Property,
                       model: => Model,
                       genModelDeps: Set[Swaggerify[_]] => Set[Model],
                       isEmptyObject: Boolean = false
                      ): Swg[T] = new Swg[T](asProperty, model, genModelDeps, isEmptyObject)
}

object Swaggerify {

  @inline def apply[T: Swaggerify]: Swaggerify[T] = implicitly[Swaggerify[T]]

  implicit val swaggerifyUnit: Swaggerify[Unit] = swaggerifyAsEmptyObject
  implicit val swaggerifyVoid: Swaggerify[java.lang.Void] = swaggerifyAsEmptyObject
  implicit val swaggerifyNull: Swaggerify[Null] = swaggerifyAsEmptyObject(Id("scala.Null", "Nul")) // "Null" can't be used in YAML
  implicit val swaggerifyNothing: Swaggerify[Nothing] = swaggerifyAsEmptyObject

  def swaggerifyAsEmptyObject[T](implicit tt: TypeTag[T]): Swaggerify[T] =
    swaggerifyAsEmptyObject(Id(tt))

  def swaggerifyAsEmptyObject[T](id: Id): Swaggerify[T] = {
    val model = ModelImpl(id = id, description = Some(id.shortId), `type` = Some("object"))
    Swg(RefProperty(id.shortId), model, _ => Set.empty, isEmptyObject = true)
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
      AbstractProperty(`type` = `type`, format = format, description = description),
      ModelImpl(id = Id(`type`, `type`), `type` = Some(`type`), format = format, description = description, isSimple = true),
      _ => Set.empty
    )

  implicit val swaggerifyAnyRef: Swaggerify[AnyRef] = Swg(
    AbstractProperty("object"),
    ModelImpl(id = Id("java.lang.Object", "Object"), `type` = Some("object")),
    _ => Set.empty
  )

  implicit def swaggerifyOption[T: Swaggerify]: Swaggerify[Option[T]] =
    Swg(Swaggerify[T].asProperty.withRequired(false), Swaggerify[T].asModel, Swaggerify[T].genModelDeps)

  implicit def swaggerifyOptional[T: Swaggerify]: Swaggerify[java.util.Optional[T]] =
    Swg(Swaggerify[T].asProperty.withRequired(false), Swaggerify[T].asModel, Swaggerify[T].genModelDeps)

  implicit def swaggerifySet[I: Swaggerify](implicit tt: TypeTag[Set[I]]): Swaggerify[Set[I]] =
    swaggerifyAsArray[Set[I], I](uniqueItems = true)

  implicit def swaggerifyJSet[I: Swaggerify](implicit tt: TypeTag[java.util.Set[I]]): Swaggerify[java.util.Set[I]] =
    swaggerifyAsArray[java.util.Set[I], I](uniqueItems = true)

  implicit def swaggerifyArray[I: Swaggerify](implicit tt: TypeTag[Array[I]]): Swaggerify[Array[I]] =
    swaggerifyAsArray[Array[I], I]()

  implicit def swaggerifySeq[I: Swaggerify](implicit tt: TypeTag[Seq[I]]): Swaggerify[Seq[I]] =
    swaggerifyAsArray[Seq[I], I]()

  implicit def swaggerifyIndexedSeq[I: Swaggerify](implicit tt: TypeTag[IndexedSeq[I]]): Swaggerify[IndexedSeq[I]] =
    swaggerifyAsArray[IndexedSeq[I], I]()

  implicit def swaggerifyList[I: Swaggerify](implicit tt: TypeTag[List[I]]): Swaggerify[List[I]] =
    swaggerifyAsArray[List[I], I]()

  implicit def swaggerifyVector[I: Swaggerify](implicit tt: TypeTag[Vector[I]]): Swaggerify[Vector[I]] =
    swaggerifyAsArray[Vector[I], I]()

  implicit def swaggerifyJList[I: Swaggerify](implicit tt: TypeTag[java.util.List[I]]): Swaggerify[java.util.List[I]] =
    swaggerifyAsArray[java.util.List[I], I]()

  implicit def swaggerifyJArrayList[I: Swaggerify](implicit tt: TypeTag[java.util.ArrayList[I]]): Swaggerify[java.util.ArrayList[I]] =
    swaggerifyAsArray[java.util.ArrayList[I], I]()

  implicit def swaggerifyJLinkedList[I: Swaggerify](implicit tt: TypeTag[java.util.LinkedList[I]]): Swaggerify[java.util.LinkedList[I]] =
    swaggerifyAsArray[java.util.LinkedList[I], I]()

  def swaggerifyAsArray[T, I: Swaggerify](uniqueItems: Boolean = false)(implicit tt: TypeTag[T]): Swaggerify[T] = Swg(
      ArrayProperty(Swaggerify[I].asProperty, uniqueItems = uniqueItems),
      ArrayModel(id = Id(tt), `type` = Some("array"), items = Swaggerify[I].asProperty, uniqueItems = uniqueItems),
      Swaggerify[I].genPropertyDeps
    )

  implicit def swaggerifyStringMap[I: Swaggerify](implicit tt: TypeTag[Map[String, I]]): Swaggerify[Map[String, I]] =
    swaggerifyAsMap[Map[String, I], I]

  implicit def swaggerifyStringJMap[I: Swaggerify](implicit tt: TypeTag[java.util.Map[String, I]]): Swaggerify[java.util.Map[String, I]] =
    swaggerifyAsMap[java.util.Map[String, I], I]

  implicit val swaggerifyStringToAnyRefMap: Swaggerify[Map[String, AnyRef]] =
    swaggerifyAsMap[Map[String, AnyRef], AnyRef]

  implicit val swaggerifyStringToAnyRefJMap: Swaggerify[java.util.Map[String, AnyRef]] =
    swaggerifyAsMap[java.util.Map[String, AnyRef], AnyRef]

  def swaggerifyAsMap[T, I: Swaggerify](implicit tt: TypeTag[T]): Swaggerify[T] = Swg(
    MapProperty(Swaggerify[I].asProperty),
    ModelImpl(id = Id(tt), `type` = Some("object"), additionalProperties = Some(Swaggerify[I].asProperty)),
    Swaggerify[I].genPropertyDeps
  )

  // consider excluding it from the default implicits as there are many reasonable ways to encode an either.
  implicit def swaggerifyEither[L: Swaggerify, R: Swaggerify]: Swaggerify[Either[L, R]] = {
    // I don't like what's happening here with the ids.
    val id2 = s"Either[${Swaggerify[L].asProperty.`type`}, ${Swaggerify[R].asProperty.`type`}]"
    val model = ModelImpl(
      id = Id(s"scala.$id2", id2),
      description = Some(id2),
      `type` = Some("object"),
      // FIXME strange things will happen with Either[Option[L], R] etc.
      properties = Map("left" -> Swaggerify[L].asProperty.withRequired(false), "right" -> Swaggerify[R].asProperty.withRequired(false))
    )
    def modelSet(alreadyKnown: Set[Swaggerify[_]]) = Swaggerify[L].genPropertyDeps(alreadyKnown) ++ Swaggerify[R].genPropertyDeps(alreadyKnown)
    Swg(RefProperty(model.id.shortId), model, modelSet)
  }

  // TODO consider:
  // Effect
  // Stream
  // Either, Try

  type Typeclass[T] = Swaggerify[T]

  def combine[T](ctx: CaseClass[Swaggerify, T]): Swaggerify[T] =
    if (ctx.isValueClass) ctx.parameters.head.typeclass.asInstanceOf[Swaggerify[T]]
    else if (ctx.isObject) swaggerifyAsEmptyObject[T](Id(ctx.typeName))
    else swaggerifyAsObject(ctx)

  private def swaggerifyAsObject[T](ctx: CaseClass[Swaggerify, T]): Swaggerify[T] = {
    val id = Id(ctx.typeName)
    def model = ModelImpl(id = id,
      description = Some(ctx.typeName.short),
      `type` = Some("object"),
      properties = ctx.parameters.map(param => param.label -> param.typeclass.asProperty).toMap
    )

    def modelSet(alreadyKnown: Set[Swaggerify[_]]) =
      ctx.parameters
        .collect { case param if !alreadyKnown.contains(param.typeclass) => param.typeclass.genPropertyDeps(alreadyKnown) }
        .toSet.flatten

    Swg(RefProperty(id.shortId), model, modelSet)
  }

  def dispatch[T](ctx: SealedTrait[Swaggerify, T]): Swaggerify[T] =
    if (ctx.subtypes.forall(_.typeclass.isEmptyObject)) swaggerifyAsEnum(ctx)
    else swaggerifyAsInheritanceHierarchy(ctx)

  private def swaggerifyAsEnum[T](ctx: SealedTrait[Swaggerify, T]): Swaggerify[T] = Swg(
    StringProperty(enums = ctx.subtypes.map(_.typeName.short).toSet, description = Some(ctx.typeName.short)),
    ModelImpl(id = Id(ctx.typeName), `type` = Some("string"), isSimple = true),
    _ => Set.empty
  )

  private def swaggerifyAsInheritanceHierarchy[T](ctx: SealedTrait[Swaggerify, T]): Swaggerify[T] = {
    val discriminatorName = "type" // TODO support for @DiscriminatorField
    val discriminatorValues = ctx.subtypes.map(_.typeclass.asModel.id.shortId).toSet
    val ownModel = ModelImpl(id = Id(ctx.typeName),
      description = Some(ctx.typeName.short),
      `type` = Some("object"),
      discriminator = Some(discriminatorName),
      properties = Map(discriminatorName -> StringProperty(enums = discriminatorValues))
    )

    val ownModelRef = RefModel(ownModel.id, ownModel.id.shortId)

    def subTypesModels = ctx.subtypes.map { sub =>
      val subModel = sub.typeclass.asModel
      ComposedModel(id = subModel.id,
        description = subModel.description,
        allOf = List(ownModelRef, subModel),
        parent = Some(ownModelRef)
      )
    }

    def modelSet(alreadyKnown: Set[Swaggerify[_]]) =
      ctx.subtypes.map(_.typeclass.genModelDeps(alreadyKnown)).fold(Set())(_ ++ _) ++ subTypesModels

    Swg(RefProperty(ownModel.id.shortId), ownModel, modelSet)
  }

  implicit def gen[T]: Swaggerify[T] = macro Magnolia.gen[T]
}
