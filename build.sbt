name := "akka-http-streaming-client-example"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= {
  val akka = "com.typesafe.akka"
  Seq(
    akka  %% "akka-stream"  % "2.5.0",
    akka  %% "akka-http"    % "10.0.5"
  )
}