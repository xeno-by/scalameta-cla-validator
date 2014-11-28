import sbt._
import Keys._
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._

object MyBuild extends Build {
  lazy val scalametaClaValidator = Project(
    id = "scalameta-cla-validator",
    base = file("."),
    settings = packageArchetype.java_application ++ Seq(
      name := "scalameta-cla-validator",
      version := "1.0",
      scalaVersion := "2.10.4",
      libraryDependencies ++= Seq(
        "com.twitter" % "finagle-http_2.10" % "6.18.0"
      )
    )
  )
}

