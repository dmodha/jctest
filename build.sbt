
name := "jobCoin"

version := "1.0"

scalaVersion := "2.11.8"

organization := "test"


resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/", "Spray Repository"  at "http://repo.spray.io")

libraryDependencies ++= {
  val akkaVersion = "2.4.10"
  val sprayVersion = "1.3.3"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-client" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "io.spray" %% "spray-json" % "1.3.2",
    "io.spray" %% "spray-http" % sprayVersion,
    "io.spray" %% "spray-httpx" % sprayVersion,
    "io.spray" %% "spray-util" % sprayVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.fasterxml.jackson.core" % "jackson-core" % "2.8.3",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.3",
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "io.spray" %% "spray-testkit" % sprayVersion % "test",
    "org.specs2" %% "specs2" % "2.3.13" % "test",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
  )


}


