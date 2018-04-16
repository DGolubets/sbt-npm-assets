package ru.dgolubets.sbt.npmassets

import java.io.File
import java.nio.file.Files

import scala.collection.JavaConverters._

/**
  * NPM build manager
  */
class NpmAssetsBuilder(settings: NpmAssetsBuilderSettings) {

  private val npm: Npm = new Npm()

  def run(isDevMode: Boolean = false): Seq[File] = {
    if (npm.isRunning) {
      // async process is still running
      Nil
    }
    else {
      npm.stop()
      if (settings.autoInstall) {
        npm.install(settings.cwd)
      }
      npm.run(settings.scriptName, settings.cwd, environmentVariables(isDevMode))

      if (!isDevMode || !settings.asyncDev) {
        val exitCode = npm.waitFor()
        if (exitCode != 0) {
          sys.error(s"NPM exit with code: $exitCode")
        }
        collectFiles(settings.target)
      }
      else Nil
    }
  }

  def stop(): Unit = {
    npm.stop()
  }

  private def environmentVariables(isDevMode: Boolean) = {
    Map(
      "SOURCE_DIR" -> settings.source.getAbsolutePath,
      "TARGET_DIR" -> settings.target.getAbsolutePath,
      "DEV_MODE" -> isDevMode.toString
    ) ++ settings.envVars
  }

  private def collectFiles(dir: File): Seq[File] = {
    Files
      .walk(dir.toPath)
      .iterator()
      .asScala
      .map(_.toFile)
      .toSeq
  }
}