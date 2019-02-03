package swaggerify
import scala.reflect.runtime.universe._

object TypeExtensions {

  implicit class RichType(t: Type) {

    val genericStart = "["
    val genericSep = ","
    val genericEnd = "]"

    def simpleName: String =
      t.typeSymbol.name.decodedName.toString +
        (if(t.typeArgs.isEmpty) "" else t.typeArgs.map(_.simpleName).mkString(genericStart, genericSep, genericEnd))

    def fullName: String =
      t.typeSymbol.fullName +
        (if(t.typeArgs.isEmpty) "" else t.typeArgs.map(_.fullName).mkString(genericStart, genericSep, genericEnd))
  }
}
