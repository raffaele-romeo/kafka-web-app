
val commonSettings = Def.settings(
  scalaVersion := "3.6.3",
  version := "0.1.0-SNAPSHOT",
  javacOptions ++= Seq("-source", "21", "-target", "21"),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % "2.13.0",
    "org.typelevel" %% "cats-effect" % "3.5.7",
    "io.circe" %% "circe-core" % "0.14.10",
    "io.circe" %% "circe-generic" % "0.14.10",
    "io.circe" %% "circe-parser" % "0.14.10",
    "org.apache.kafka" % "kafka-clients" % "3.9.0",
    "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
    "org.typelevel" %% "log4cats-core" % "2.7.0",
    "ch.qos.logback" % "logback-classic" % "1.5.16"
  )
)

lazy val root = (project in file("."))
  .settings(name := "kafka-web-app")
  .aggregate(kafkaWebApp, kafkaDataLoader)

lazy val kafkaDataLoader = (project in file("modules/kafka-data-loader"))
  .settings(commonSettings)
  .settings(
    name := "kafka-data-loader",
    Compile / run / mainClass := Some("com.kafka.application.Main")
  )

lazy val kafkaWebApp = (project in file("modules/kafka-web-app"))
  .settings(commonSettings)
  .settings(
    name := "kafka-web-app",
    Compile / run / mainClass := Some("com.kafka.application.App"),
    libraryDependencies ++= Seq( 
      "org.http4s" %% "http4s-ember-server" % "1.0.0-M44",
      "org.http4s" %% "http4s-circe" % "1.0.0-M44",
      "org.http4s" %% "http4s-dsl" % "1.0.0-M44",
      "com.disneystreaming" %% "weaver-cats" % "0.8.4" % Test
    )
  )