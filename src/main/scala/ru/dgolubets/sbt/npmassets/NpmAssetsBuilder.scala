package ru.dgolubets.sbt.npmassets

import java.io.File
import java.nio.file.Files

import scala.collection.JavaConverters._

/**
  * NPM build manager
  */
class NpmAssetsBuilder(settings: NpmAssetsBuilderSettings) {

  private val npm: Npm = new Npm()
  private var isDevMode: Boolean = false
  private var npmInstallExecuted: Boolean = false

  def setDevMode(enable: Boolean): Unit = {
    isDevMode = enable
  }

  def run(): Seq[File] = {
    if (npm.isRunning) {
      // async process is still running
      Nil
    }
    else {
      val isAsyncMode = isDevMode && settings.asyncDev

      if (settings.autoInstall && (!isDevMode || !npmInstallExecuted)) {
        npm.install(settings.cwd)
        npmInstallExecuted = true
      }

      // in async mode we usually want to write directly to folder where Play is serving assets from
      val targetDir =
        if (isAsyncMode) settings.targetInAsync
        else settings.target

      npm.run(settings.scriptName, settings.cwd, environmentVariables(settings.source, targetDir, isDevMode))

      if (!isAsyncMode) {
        val exitCode = npm.waitFor()
        if (exitCode != 0) {
          sys.error(s"NPM exit with code: $exitCode")
        }
        collectFiles(targetDir)
      }
      else Nil
    }
  }

  def stop(): Unit = {
    npm.stop()
  }

  private def environmentVariables(source: File, target: File, isDevMode: Boolean) = {
    Map(
      "SOURCE_DIR" -> source.getAbsolutePath,
      "TARGET_DIR" -> target.getAbsolutePath,
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