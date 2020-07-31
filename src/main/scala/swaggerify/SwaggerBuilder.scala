package swaggerify

import swaggerify.SwaggerBuilder._
import swaggerify.{models => m}

import scala.collection.immutable.ListMap
import scala.language.implicitConversions

case class SwaggerBuilder(renderSimpleTypesAsRefModels: Boolean = false,
                          renderArraysAsRefModels: Boolean = false,
                          renderMapsAsRefModels: Boolean = false,
                          renderComplexTypesAsRefModels: Boolean = true,
                          paths: Map[String, m.Path] = ListMap.empty,
                          definitions: Map[String, m.Model] = ListMap.empty,
                          tags: Map[String, m.Tag] = ListMap.empty,
                         ) {

  def add(route: Route): SwaggerBuilder = {
    val path = route.path
    val pathString = makePathString(path)
    val parameters = makePathParameters(path)
    val operationName = route.method.toLowerCase
    val (operation, models) = makeOperation(route)

    val basePathModel = paths.getOrElse(pathString, m.Path(parameters = parameters))
    val pathModel = addOperation(basePathModel, operationName, operation)

    val newDefinitions = models.toVector.map(m => m.id.shortId -> m).toMap

    val tags = route.tags.map(t => t.name -> t).toMap

    copy(
      paths = this.paths + (pathString -> pathModel),
      definitions = this.definitions ++ newDefinitions,
      tags = this.tags ++ tags,
    )
  }

  private def addOperation(basePathModel: m.Path, operationName: String, operation: m.Operation): m.Path = {
    operationName match {
      case "get" => basePathModel.copy(get = Some(operation))
      case "put" => basePathModel.copy(put = Some(operation))
      case "delete" => basePathModel.copy(delete = Some(operation))
      case "post" => basePathModel.copy(post = Some(operation))
      case "patch" => basePathModel.copy(patch = Some(operation))
      case "options" => basePathModel.copy(options = Some(operation))
      case "head" => basePathModel.copy(head = Some(operation))
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

  private def makeOperation(route: Route): (m.Operation, Set[m.Model]) = {
    val (responses, models) = makeResponses(route)
    val bodySwaggerify = route.bodyParameter.map(p => useRefModelAccordingToConfiguration(p.swaggerify))
    val bodyModels = bodySwaggerify.map(_.bodyParameterDependencies).toSet.flatten
    val operation = m.Operation(
      operationId = Some(route.name),
      description = route.description,
      responses = responses,
      parameters = bodySwaggerify.map(_.asBodyParameter()).toList ++
        route.queryParameters.map(p => p.swaggerify.asQueryParameter(p.name)) ++
        route.headerParameters.map(p => p.swaggerify.asHeaderParameter(p.name)) ++
        route.cookieParameters.map(p => p.swaggerify.asCookieParameter(p.name)) ++
        route.formParameters.map(p => p.swaggerify.asFormParameter(p.name))
    )

    (operation, models ++ bodyModels)
  }

  private def makeResponses(route: Route): (Map[String, m.Response], Set[m.Model]) = {
    val responses = route.responses.map { resp =>
      val (responseModel, models) = makeResponse(resp)
      (resp.code.toString, responseModel, models)
    }
    (
      responses.map { case (respCode, responseModel, _) => respCode -> responseModel }.toMap,
      responses.flatMap { case (_, _, models) => models }.toSet
    )
  }

  private def makeResponse(resp: Response[_]): (m.Response, Set[m.Model]) = {
    val swg = useRefModelAccordingToConfiguration(resp.swaggerify)
    (m.Response(description = resp.description, schema = Some(swg.asModel)), swg.modelDependencies)
  }

  private def useRefModelAccordingToConfiguration(swaggerify: Swaggerify[_]): Swaggerify[_] = {
    val shouldUseRefModel = swaggerify.asProperty match {
      case _: m.AbstractProperty => renderSimpleTypesAsRefModels
      case _: m.StringProperty => renderSimpleTypesAsRefModels
      case _: m.ArrayProperty => renderArraysAsRefModels
      case _: m.MapProperty => renderMapsAsRefModels
      case _: m.ObjectProperty => renderComplexTypesAsRefModels
      case _: m.RefProperty => renderComplexTypesAsRefModels
    }
    if (shouldUseRefModel) swaggerify.usingRefModel() else swaggerify
  }

  def build(info: m.Info): m.Swagger = {
    m.Swagger(
      info = info,
      paths = paths,
      definitions = definitions,
      tags = tags.values.toList,
    )
  }
}

object SwaggerBuilder {
  case class Route(name: String, description: Option[String], method: String, path: Path, responses: Seq[Response[_]],
                   bodyParameter: Option[BodyParameter[_]] = None, queryParameters: Seq[NonBodyParameter[_]] = Seq.empty,
                   headerParameters: Seq[NonBodyParameter[_]] = Seq.empty, cookieParameters: Seq[NonBodyParameter[_]] = Seq.empty,
                   formParameters: Seq[NonBodyParameter[_]] = Seq.empty, tags: Seq[m.Tag] = Seq.empty)

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

  case class BodyParameter[T](name: String, description: Option[String] = None)(implicit val swaggerify: Swaggerify[T])
  case class NonBodyParameter[T](name: String, description: Option[String] = None)(implicit val swaggerify: Swaggerify[T])

  implicit def pathFromString(s: String): Path = Path(Vector(PathString(s)))

}
