name := "my-akka-patterns"

version := "1.0"

scalaVersion := "2.12.1"

scalacOptions ++= Seq("-feature")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.16"
)