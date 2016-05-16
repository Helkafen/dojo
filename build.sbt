name := "dojo_collections"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.scalacheck" %% "scalacheck" % "1.12.2" % "test",
  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3" % "test"
)