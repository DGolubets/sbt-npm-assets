name := "sbt-npm-assets"
organization := "ru.dgolubets"

sbtPlugin := true
scalaVersion := "2.12.4"
crossSbtVersions := Seq("0.13.16", "1.0.4")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.7")

bintrayRepository := "sbt-plugins"
bintrayOrganization in bintray := None
bintrayPackageLabels := Seq("sbt", "npm", "assetsreload")

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))
publishMavenStyle := false
publishArtifact in Test := false

libraryDependencies ++= Seq(
  "net.java.dev.jna" % "jna" % "4.5.1",
  "net.java.dev.jna" % "jna-platform" % "4.5.1"
)

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("^ test"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("^ publish"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)