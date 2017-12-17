name := "masonjar"

version := "0.1"

scalaVersion := "2.12.4"

cancelable in Global := true

val Http4sVersion = "0.17.5"
val Specs2Version = "4.0.0"
val LogbackVersion = "1.2.3"

libraryDependencies ++= Seq(

  // Server
  "org.http4s"     %% "http4s-blaze-server" % Http4sVersion,
  "org.http4s"     %% "http4s-server"       % Http4sVersion,
  "org.http4s"     %% "http4s-circe"        % Http4sVersion,
  "org.http4s"     %% "http4s-dsl"          % Http4sVersion,
  "org.specs2"     %% "specs2-core"         % Specs2Version % "test",
  "ch.qos.logback" %  "logback-classic"     % LogbackVersion,

  // JSON support
  "org.http4s" %% "http4s-circe" % Http4sVersion,
  // Optional for auto-derivation of JSON codecs
  "io.circe" %% "circe-generic" % "0.8.0",
  // Optional for string interpolation to JSON model
  "io.circe" %% "circe-literal" % "0.8.0",

  // testing
  "org.scalactic" %% "scalactic" % "3.0.4",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",

)

