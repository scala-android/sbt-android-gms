import bintray.Keys._

name := "sbt-android-gms"
organization := "org.scala-android"
version := "0.1"

scalacOptions ++= Seq("-deprecation","-Xlint","-feature", "-unchecked")
sbtPlugin := true

libraryDependencies += "com.hanhuy.sbt" %% "bintray-update-checker" % "0.1"
libraryDependencies += "io.argonaut" %% "argonaut" % "6.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test"
addSbtPlugin("org.scala-android" % "sbt-android" % "1.6.12")

bintrayPublishSettings
repository in bintray := "sbt-plugins"
publishMavenStyle := false
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
bintrayOrganization in bintray := None

scriptedSettings
scriptedLaunchOpts ++= Seq("-Xmx1024m", "-Dplugin.version=" + version.value)
