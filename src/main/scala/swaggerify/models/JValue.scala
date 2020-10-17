package swaggerify.models
//import scala.collection.JavaConverters._
import scala.jdk.CollectionConverters._

object JValue {
    def fromOption[A](oa: Option[A]): A =
      oa.getOrElse(null.asInstanceOf[A])

    def fromList[A](xs: List[A]): java.util.List[A] =
      if (xs.isEmpty) null else xs.asJava

    def fromMap[A, B](m: Map[A, B]): java.util.Map[A, B] =
      if (m.isEmpty) null else m.asJava
}
