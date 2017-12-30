name := "sbt-npm-assets"
organization := "ru.dgolubets"

sbtPlugin := true
scalaVersion := "2.12.4"
crossSbtVersions := Seq("0.13.16", "1.0.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.4.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.2")

bintrayRepository := "sbt-plugins"
bintrayOrganization in bintray := None
bintrayPackageLabels := Seq("sbt", "npm", "assetsreload")

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))
publishMavenStyle := false
publishArtifact in Test := false

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