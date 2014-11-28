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
        "com.twitter" %% "finagle-http" % "6.18.0",
        "net.databinder" %% "dispatch-http" % "0.8.9",
        "net.databinder" %% "dispatch-http-json" % "0.8.9",
        "net.liftweb" %% "lift-json" % "2.5"
      )
    )
  )
}

