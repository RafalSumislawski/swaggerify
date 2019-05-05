package swaggerify

import scala.reflect.runtime.universe._

case class Id(fullId: String, shortId: String) {
  def withSuffix(suffix: String) = Id(fullId + suffix, shortId + suffix)
}

object Id {

  def apply[T](tt: TypeTag[T]): Id = Id(fullId(tt.tpe), shortId(tt.tpe))

  private def fullId[T](t: Type): String = t.typeSymbol.fullName + typeArgsString(t.typeArgs.map(fullId))

  private def shortId[T](t: Type): String = t.typeSymbol.name.decodedName.toString + typeArgsString(t.typeArgs.map(shortId))

  def apply(tn: magnolia.TypeName): Id = Id(fullId(tn), shortId(tn))

  private def fullId(tn: magnolia.TypeName): String = tn.full + typeArgsString(tn.typeArguments.map(fullId))

  private def shortId(tn: magnolia.TypeName): String = tn.short + typeArgsString(tn.typeArguments.map(shortId))

  private def typeArgsString(args: Seq[String]): String =
    if (args.isEmpty) ""
    else args.mkString(genericStart, genericSep, genericEnd)

  private val genericStart = "["
  private val genericSep = ","
  private val genericEnd = "]"
}
