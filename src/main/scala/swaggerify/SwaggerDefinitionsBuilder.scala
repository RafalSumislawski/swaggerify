package swaggerify

import io.swagger.{models => jm}
import org.http4s.rho.swagger.models.{ComposedModel, Model}

case class SwaggerDefinitionsBuilder(models: Set[Model] = Set.empty) {

  def addModelType[T: Swaggerify]: SwaggerDefinitionsBuilder = copy(models ++ Swaggerify[T].modelDependencies)

  def addPropertyType[T: Swaggerify]: SwaggerDefinitionsBuilder = copy(models ++ Swaggerify[T].propertyDependencies)

  def build(): Map[String, jm.Model] = {
    val modelsByNameWithoutDuplicates = dropDuplicatesPreferingComposedModels(models)
    modelsByNameWithoutDuplicates.map { case (id2, model) => id2 -> model.toJModel }
  }

  private def dropDuplicatesPreferingComposedModels(models: Set[Model]): Map[String, Model] = {
    models.toVector.groupBy(_.id2).map { case (id2, models) =>
      val selectedModel = models match {
        case Seq(theOnlyModel) => theOnlyModel
        case models => models.collectFirst { case cm: ComposedModel => cm }.getOrElse(models.head)
      }
      id2 -> selectedModel
    }
  }
}
