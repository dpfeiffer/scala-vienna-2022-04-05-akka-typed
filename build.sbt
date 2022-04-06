lazy val commonSettings = Seq(
  organization := "com.firstbird"
)

lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .settings(
    name         := "akka-typed-demo",
    scalaVersion := "3.1.1",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % "2.6.19",
      "org.slf4j"          % "slf4j-simple"     % "1.7.36"
    )
  )
