package ru.dgolubets.sbt.npmassets

import java.io.{ByteArrayOutputStream, DataOutputStream, FileInputStream, FileOutputStream}
import java.nio.file.Files
import java.util

import scala.collection.JavaConverters._

import com.typesafe.sbt.packager.universal.UniversalPlugin
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.{PathMapping, SbtWeb}
import sbt.Keys._
import sbt._

object NpmAssetsPlugin extends AutoPlugin {

  override def requires = SbtWeb && UniversalPlugin

  import SbtWeb.autoImport.WebKeys._
  import SbtWeb.autoImport._
  import UniversalPlugin.autoImport.{stage, _}

  trait Configurations {
    val NpmAssets = config("npm-assets")
  }

  trait Keys {
    val scriptName = taskKey[String]("NPM script to start on assets task")
    val optimize = settingKey[Boolean]("Production build flag")
    val filter = taskKey[Pipeline.Stage]("Filter sources")
    val skipOnNoChanges = settingKey[Boolean]("Check if source files have changed since previous build (DEV build only).")
  }

  object Configurations extends Configurations

  object Keys extends Keys

  object autoImport extends Keys with Configurations

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = defaultSettings ++ hackSettings

  private def defaultSettings: Seq[Def.Setting[_]] = inConfig(NpmAssets)(Seq(
    target := webTarget.value / "npm_assets",
    scriptName := "assets",
    sourceDirectory := (sourceDirectory in Assets).value,
    envVars := environmentVariables.value,
    optimize := false,
    sources in NpmAssets := generateSources.value,
    filter in NpmAssets := filterSources.value,
    skipOnNoChanges := true
  ))

  // global var is the only way I found to alter assets compilation depending on run or dist being called
  // not pretty but it works
  private var shouldOptimize = false

  private def hackSettings: Seq[Def.Setting[_]] = Seq(
    exportedMappings in Assets := Seq(), // https://github.com/playframework/playframework/issues/5242
    sourceGenerators in Assets += (sources in NpmAssets).taskValue,
    pipelineStages in Assets += (filter in NpmAssets),
    stage in Universal := wrapTask(stage in Universal, {
      shouldOptimize = true
    }, {
      shouldOptimize = false
    }).value,
    packageBin in Universal := wrapTask(packageBin in Universal, {
      shouldOptimize = true
    }, {
      shouldOptimize = false
    }).value
  )

  private def environmentVariables = Def.task[Map[String, String]] {
    Map(
      "SOURCE_DIR" -> (sourceDirectory in NpmAssets).value.getAbsolutePath,
      "TARGET_DIR" -> (target in NpmAssets).value.getAbsolutePath,
      "OPTIMIZE" -> shouldOptimize.toString
    )
  }

  private def generateSources = Def.task {
    NpmAssetsImpl.run(
      (scriptName in NpmAssets).value,
      (baseDirectory in NpmAssets).value,
      (sourceDirectory in NpmAssets).value,
      (target in NpmAssets).value,
      (envVars in NpmAssets).value,
      (skipOnNoChanges in NpmAssets).value,
      shouldOptimize
    )
  }

  private def filterSources = Def.task { mappings: Seq[PathMapping] =>
    val sourceDir = (sourceDirectory in NpmAssets).value
    val targetDir = (target in NpmAssets).value

    mappings
      .filter { case (file, _) => file.relativeTo(sourceDir).isEmpty } // exclude sources
      .map {
      case m@(file, _) =>
        file
          .relativeTo(targetDir)
          .map(f => (file, f.toString))
          .getOrElse(m) // fallback to original mapping if it comes from somewhere else
    }
  }

  private def wrapTask[T](taskKey: TaskKey[T], before: => Unit, after: => Unit, mapper: T => T = (a: T) => a): Def.Initialize[Task[T]] = Def.task[T] {
    taskKey.dependsOn(Def.task {
      before
    }).map(mapper).andFinally({
      after
    }).value
  }
}

private object NpmAssetsImpl {

  import scala.sys.process._

  def run(scriptName: String, cwd: File, source: File, target: File, envVars: Map[String, String], skipOnNoChanges: Boolean, optimize: Boolean): Seq[File] = {
    val hashFile = new File(target, ".hash")
    lazy val dirHash = directoryHash(source)

    if (optimize || !hashFile.exists() || !util.Arrays.equals(readFile(hashFile), dirHash)) {
      build(scriptName, cwd, envVars)
      writeFile(hashFile, dirHash)
    }

    collectFiles(target).filter(_ != hashFile)
  }

  private def build(scriptName: String, cwd: File, envVars: Map[String, String]): Unit = {
    val os = sys.props("os.name").toLowerCase
    var command = "npm run" :: scriptName :: Nil
    command = os match {
      case x if x contains "windows" => List("cmd", "/C") ++ command
      case _ => command
    }

    Process(command, cwd, envVars.toSeq: _*) #> sys.process.stdout !!
  }

  private def collectFiles(dir: File): Seq[File] = {
    val targetFiles = (dir ** (-DirectoryFilter)).get
    targetFiles.map(_.getAbsoluteFile)
  }

  private def directoryHash(dir: File): Array[Byte] = {
    val files = Files
      .walk(dir.toPath, Int.MaxValue)
      .iterator()
      .asScala
      .map(_.toFile)

    val (count, timestamp) = files.foldLeft((0, 0L)) {
      case ((c, t), f) => (c + 1, t max f.lastModified)
    }

    val hashStream = new ByteArrayOutputStream()
    val dataStream = new DataOutputStream(hashStream)
    dataStream.writeInt(count)
    dataStream.writeLong(timestamp)
    dataStream.close()
    hashStream.toByteArray
  }

  private def readFile(file: File): Array[Byte] = {
    val stream = new FileInputStream(file)
    val buffer = new Array[Byte](file.length.toInt)
    try {
      stream.read(buffer)
      buffer
    }
    finally {
      stream.close()
    }
  }

  private def writeFile(file: File, bytes: Array[Byte]): Unit = {
    val stream = new FileOutputStream(file)
    try {
      stream.write(bytes)
    }
    finally {
      stream.close()
    }
  }
}