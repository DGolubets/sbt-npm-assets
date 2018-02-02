package ru.dgolubets.sbt.npmassets

import com.typesafe.sbt.packager.universal.UniversalPlugin
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.{PathMapping, SbtWeb}
import play.sbt.Play
import sbt.Keys._
import sbt.{Def, _}

object NpmAssetsPlugin extends AutoPlugin {

  override def requires = SbtWeb && UniversalPlugin && Play

  import SbtWeb.autoImport.WebKeys._
  import SbtWeb.autoImport._

  trait Configurations {
    val NpmAssets = config("npm-assets")
  }

  trait Keys {
    val scriptName = settingKey[String]("NPM script to start on assets task")
    val asyncDev = settingKey[Boolean]("Run NPM script asynchronously in development mode.")
    val filter = taskKey[Pipeline.Stage]("Filter sources")
    val autoInstall = settingKey[Boolean]("Run NPM install automatically.")
  }

  object Configurations extends Configurations

  object Keys extends Keys

  object autoImport extends Keys with Configurations

  import play.sbt.PlayImport.PlayKeys._

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = defaultSettings ++ hackSettings

  private def defaultSettings: Seq[Def.Setting[_]] = inConfig(NpmAssets)(Seq(
    scriptName := "assets",
    sourceDirectory := (sourceDirectory in Assets).value,
    envVars := Map.empty,
    asyncDev := false,
    autoInstall:= true,
    sources in NpmAssets := generateSources.value,
    filter in NpmAssets := filterSources.value
  ))

  private def hackSettings: Seq[Def.Setting[_]] = Seq(
    playRunHooks += playHook.value,
    exportedMappings in Assets := Seq(), // https://github.com/playframework/playframework/issues/5242
    sourceGenerators in Assets += (sources in NpmAssets).taskValue,
    pipelineStages in Assets += (filter in NpmAssets),
    playMonitoredFiles := monitoredFiles.value
  )

  private def generateSources = Def.task {
    npmAssetsBuilder.value.run()
    Nil
  }

  private def filterSources = Def.task { mappings: Seq[PathMapping] =>
    val sourceDir = (sourceDirectory in NpmAssets).value
    mappings.filter { case (file, _) => file.relativeTo(sourceDir).isEmpty } // exclude sources
  }

  private def monitoredFiles = Def.task {
    val isAsync = (asyncDev in NpmAssets).value
    val sourceDir = (sourceDirectory in NpmAssets).value
    val files = playMonitoredFiles.value
    if(isAsync){
      // async dev assumes NPM is watching over sources itself
      files.filterNot(_.relativeTo(sourceDir).nonEmpty)
    } else {
      files
    }
  }

  private lazy val playHook = Def.task {
    new NpmAssetsPlayRunHook(npmAssetsBuilder.value)
  }

  private lazy val npmAssetsBuilder = Def.task {
    npmAssetsBuilderManager.get(
      NpmAssetsBuilderSettings(
        (scriptName in NpmAssets).value,
        (baseDirectory in NpmAssets).value,
        (sourceDirectory in NpmAssets).value,
        (public in Assets).value,
        (envVars in NpmAssets).value,
        (asyncDev in NpmAssets).value,
        (autoInstall in NpmAssets).value)
    )
  }

  private val npmAssetsBuilderManager = new NpmAssetsBuilderManager
}

