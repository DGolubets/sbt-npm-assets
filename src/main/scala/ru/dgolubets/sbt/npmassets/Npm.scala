package ru.dgolubets.sbt.npmassets

import java.io.File
import scala.collection.JavaConverters._

/**
  * NPM process manager
  */
class Npm() {

  private var process: Option[Process] = None

  def start(scriptName: String, cwd: File, envVars: Map[String, String]): Unit = {
    val os = sys.props("os.name").toLowerCase
    var command = "npm run" :: scriptName :: Nil
    command = os match {
      case x if x contains "windows" => List("cmd", "/C") ++ command
      case _ => command
    }

    val processBuilder = new ProcessBuilder(command.asJava)
    processBuilder.directory(cwd)
    processBuilder.environment().putAll(envVars.asJava)
    processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
    processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
    val p = processBuilder.start()

    process = Some(p)
  }

  def waitFor(): Int = {
    process.map(_.waitFor()).getOrElse(0)
  }

  def isRunning: Boolean = process.exists(_.isAlive())

  def stop(): Unit = {
    process.foreach(_.destroyForcibly())
    process = None
  }
}
