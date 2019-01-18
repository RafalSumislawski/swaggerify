package swaggerify

import swaggerify.SwaggerBuilder._
import swaggerify.models._

import scala.language.implicitConversions

case class SwaggerBuilder(routes: Seq[Route] = Vector.empty) {
  def add(r: Route): SwaggerBuilder = SwaggerBuilder(routes :+ r)

  def build(info: Info): Swagger = {
    val paths = makePaths(routes)
    val definitions = makeModels(routes)

    Swagger(info = info, paths = paths, definitions = definitions)
  }

  private def makeModels(routes: Seq[Route]): Map[String, Model] = {
    routes.foldLeft(SwaggerDefinitionsBuilder()) { (builder, route) =>
      route.responses.foldLeft(builder)((builder, response) => builder.addModelType(response.swaggerify))
    }.build()
  }

  private def makePaths(routes: Seq[Route]): Map[String, models.Path] = {
    routes.groupBy(r => r.path).map { case (path, routes) =>
      val pathString = makePathString(path)
      val parameters = makePathParameters(path)
      val operations = makeOperations(routes)

      val modelPath = models.Path(parameters = parameters.toList)
      val modelPath2 = operations.foldLeft(modelPath) { (p, mo) =>
        val (method, operation) = mo
        method match {
          case "get" => p.copy(get = Some(operation))
          case "put" => p.copy(put = Some(operation))
          case "post" => p.copy(post = Some(operation))
          case "delete" => p.copy(delete = Some(operation))
          case "patch" => p.copy(patch = Some(operation))
          case "options" => p.copy(options = Some(operation))
          case "head" => p.copy(head = Some(operation))
        }
      }
      pathString -> modelPath2
    }
  }

  private def makePathString(path: SwaggerBuilder.Path): String = {
    path.segments.map {
      case PathString(s) => s
      case PathVar(name, _) => s"{$name}"
    }.mkString("/", "/", "")
  }

  private def makePathParameters(path: SwaggerBuilder.Path): Seq[Parameter] =
    path.segments.collect { case p: PathVar[_] => p.swaggerify.asPathParameter(p.name, p.description) }

  private def makeOperations(routes: Seq[Route]): Seq[(String, Operation)] = {
    routes.map { route =>
      val method = route.method.toLowerCase
      val responses = route.responses.map { resp =>
        resp.code.toString -> models.Response(description = resp.description, schema = resp.swaggerify.asModel)
      }.toMap
      val operation = Operation(operationId = Some(route.name), description = route.description, responses = responses)

      (method, operation)
    }
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
