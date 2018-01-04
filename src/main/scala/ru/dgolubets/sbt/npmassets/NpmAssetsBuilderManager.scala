package ru.dgolubets.sbt.npmassets

/**
  * Cache of a single instance of NpmAssetsBuilder.
  */
class NpmAssetsBuilderManager {

  private var cache: Map[NpmAssetsBuilderSettings, NpmAssetsBuilder] = Map.empty

  /**
    * Get an instance of NpmAssetsBuilder with specified settings,
    * recreating it if settings have changed.
    * @param settings
    * @return
    */
  def get(settings: => NpmAssetsBuilderSettings): NpmAssetsBuilder = {
    val newSettings = settings
    cache.get(newSettings) match {
      case Some(builder) =>
        builder
      case _ =>
        cache.foreach { case (_, b) => b.stop() }
        val builder = new NpmAssetsBuilder(newSettings)
        cache = Map(newSettings -> builder)
        builder
    }
  }
}
