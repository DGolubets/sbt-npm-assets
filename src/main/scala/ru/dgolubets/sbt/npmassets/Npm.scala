package ru.dgolubets.sbt.npmassets

import java.io.File

import ru.dgolubets.sbt.npmassets.util.ProcessUtil

/**
  * NPM process manager
  */
class Npm() {

  private var process: Option[Process] = None

  def start(scriptName: String, cwd: File, envVars: Map[String, String]): Unit = {
    val p = ProcessUtil.exec("npm", "run" :: scriptName :: Nil, envVars, Some(cwd))
    process = Some(p)
  }

  def waitFor(): Int = {
    process.map(_.waitFor()).getOrElse(0)
  }

  def isRunning: Boolean = process.exists(_.isAlive())

  def stop(): Unit = {
    process.foreach { p =>
      ProcessUtil.kill(p)
    }
    process = None
  }
}
