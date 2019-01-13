package swaggerify

import io.swagger.models.parameters.Parameter
import io.swagger.models.{HttpMethod, Model, Operation, Swagger}
import io.swagger.{models => jm}
import swaggerify.SwaggerBuilder._

import scala.collection.JavaConverters._
import scala.language.implicitConversions

case class SwaggerBuilder(routes: Seq[Route] = Vector.empty) {
  def add(r: Route): SwaggerBuilder = SwaggerBuilder(routes :+ r)

  def build(): Swagger = {
    val paths = makePaths(routes)
    val definitions = makeModels(routes)

    val swagger = new jm.Swagger
    swagger.setPaths(paths.asJava)
    swagger.setDefinitions((collection.mutable.Map() ++ definitions).asJava)
    swagger
  }

  private def makeModels(routes: Seq[Route]): Map[String, Model] = {
    routes.foldLeft(SwaggerDefinitionsBuilder()) { (builder, route) =>
      route.responses.foldLeft(builder)((builder, response) => builder.addModelType(response.swaggerify))
    }.build()
  }

  private def makePaths(routes: Seq[Route]): Map[String, jm.Path] = {
    routes.groupBy(r => r.path).map { case (path, routes) =>
      val pathString = makePathString(path)
      val parameters = makePathParameters(path)
      val operations = makeOperations(routes)

      val jPath = new jm.Path()
      jPath.setParameters(parameters.toBuffer.asJava)
      operations.foreach { case (method, op) => jPath.set(method, op) }

      pathString -> jPath
    }
  }

  private def makePathString(path: Path): String = {
    path.segments.map {
      case PathString(s) => s
      case PathVar(name, _) => s"{$name}"
    }.mkString("/", "/", "")
  }

  private def makePathParameters(path: Path): Seq[Parameter] =
    path.segments.collect { case p: PathVar[_] => p.swaggerify.asPathParameter(p.name, p.description).toJModel }

  private def makeOperations(routes: Seq[Route]): Seq[(String, Operation)] = {
    routes.map { route =>
      val method = route.method.name().toLowerCase
      val operation = new jm.Operation()
      operation.setOperationId(route.name)
      operation.setDescription(route.description.orNull)
      route.responses.foreach { resp =>
        val response = new jm.Response()
        response.setDescription(resp.description)
        response.setResponseSchema(resp.swaggerify.asModel.get.toJModel)
        operation.addResponse(resp.code.toString, response)
      }

      (method, operation)
    }
  }
}

object SwaggerBuilder {
  case class Route(name: String, description: Option[String], method: HttpMethod, path: Path, responses: Response[_]*)

  case class Response[R](code: Int, description: String)(implicit val swaggerify: Swaggerify[R])

  def ok[R: Swaggerify] = Response(200, "ok")
  def ok[R: Swaggerify](description: String) = Response(200, description)
  def notFound[R: Swaggerify] = Response(404, "not found")
  def notFound[R: Swaggerify](description: String) = Response(404, description)

  case class Path(segments: Seq[PathSegment]) {
    def /(pathString: String): Path = Path(segments :+ PathString(pathString))
    def /[T](pathVar: PathVar[T]): Path = Path(segments :+ pathVar)
  }

  sealed trait PathSegment
  case class PathVar[T](name: String, description: Option[String] = None)(implicit val swaggerify: Swaggerify[T]) extends PathSegment
  case class PathString(s: String) extends PathSegment

  implicit def pathFromString(s: String): Path = Path(Vector(PathString(s)))
}
