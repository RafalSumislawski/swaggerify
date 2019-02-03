package swaggerify.validation

import cats._
import cats.data._

import scala.collection.JavaConverters._

object SwaggerValidator {

  def validate(swagger: String): Validated[NonEmptyList[String], Unit] = {
    val validationResponse = validationService.debugByContent(swagger)

    val messages = Option(validationResponse.getMessages).toList.flatMap(_.asScala)
    val fromMessages: Validated[NonEmptyList[String], Unit] = Validated.fromOption(NonEmptyList.fromList(messages), ()).swap

    val validationMessages = Option(validationResponse.getSchemaValidationMessages).toList.flatMap(_.asScala)
      .map(e => s"${e.getInstance.getPointer}: ${e.getMessage}")
    val fromValidationMessages: Validated[NonEmptyList[String], Unit] = Validated.fromOption(NonEmptyList.fromList(validationMessages), ()).swap

    Applicative[Validated[NonEmptyList[String], ?]].map2(fromMessages, fromValidationMessages)((_: Unit, _: Unit) => ())
  }

  private lazy val validationService: ValidatorService = new ValidatorService()
}
