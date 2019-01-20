package swaggerify

import swaggerify.{models => m}
import swaggerify.SwaggerBuilder._

import scala.language.implicitConversions

case class SwaggerBuilder(routes: Seq[Route] = Vector.empty) {
  def add(r: Route): SwaggerBuilder = SwaggerBuilder(routes :+ r)

  def build(info: m.Info): m.Swagger = {
    val paths = makePaths(routes)
    val definitions = makeModels(routes)

    m.Swagger(
      info = Some(info), // it shouldn't be optional. It's not according to the specs and the validator.
      paths = paths,
      definitions = definitions
    )
  }

  private def makeModels(routes: Seq[Route]): Map[String, m.Model] = {
    routes.foldLeft(SwaggerDefinitionsBuilder()) { (builder, route) =>
      route.responses.foldLeft(builder)((builder, response) => builder.addModelType(response.swaggerify))
    }.build()
  }

  private def makePaths(routes: Seq[Route]): Map[String, m.Path] = {
    routes.groupBy(r => r.path).map { case (path, routes) =>
      val pathString = makePathString(path)
      val parameters = makePathParameters(path)
      val operations = makeOperations(routes)

      pathString -> m.Path(
        parameters = parameters,
        get = operations.get("get"),
        put = operations.get("put"),
        delete = operations.get("delete"),
        post = operations.get("post"),
        patch = operations.get("patch"),
        options = operations.get("options"),
        head = operations.get("head")
        )
    }
  }

  private def makePathString(path: Path): String = {
    path.segments.map {
      case PathString(s) => s
      case PathVar(name, _) => s"{$name}"
    }.mkString("/", "/", "")
  }

  private def makePathParameters(path: Path): List[m.Parameter] =
    path.segments.collect { case p: PathVar[_] => p.swaggerify.asPathParameter(p.name, p.description) }.toList

  private def makeOperations(routes: Seq[Route]): Map[String, m.Operation] = {
    routes.map { route =>
      val method = route.method.toLowerCase
      val responses = makeResponses(route)

      val operation = m.Operation(
        operationId = Some(route.name),
        description = route.description,
        responses = responses
      )

      (method, operation)
    }
  }.toMap

  private def makeResponses(route: Route): Map[String, m.Response] = {
    route.responses.map{ resp =>
      resp.code.toString -> m.Response(
        description = resp.description,
        schema = resp.swaggerify.asModel
      )
    }.toMap
  }
}

object SwaggerBuilder {
  case class Route(name: String, description: Option[String], method: String, path: Path, responses: Response[_]*)

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
