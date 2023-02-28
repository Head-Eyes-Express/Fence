import sbt._

object Dependencies {

  val akkaOrg = "com.typesafe.akka"
  val scullxOrg = "com.github.scullxbones"

  val akkaV = "2.6.20"
  val mongoAkkaV = "3.0.8"
  val calibanV = "2.0.2"
  val akkaHttpV = "10.2.10"
  val scalaMockV = "5.1.0"
  val scalaTestV = "3.1.0"
  val scalaCheckV = "1.14.1"
  val circeV = "1.39.2"
  val circeVersion = "0.14.4"

  val caliban = "com.github.ghostdogpr"  %% "caliban" % calibanV
  val akkaPersistenceTyped = akkaOrg %% "akka-persistence-typed" % akkaV
  val akkaPersistenceMongoJournal = scullxOrg %% "akka-persistence-mongo-scala" % mongoAkkaV
  val akkaTypedTestKit = akkaOrg %% "akka-persistence-testkit" % akkaV % Test
  val akkaHttp = akkaOrg %% "akka-http" % akkaHttpV
  val circe = "de.heikoseeberger" %% "akka-http-circe" % circeV
  val circeCore = "io.circe" %% "circe-core" % circeVersion
  val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  val circeParser = "io.circe" %% "circe-parser" % circeVersion
  val logback = "ch.qos.logback" % "logback-classic" % "1.4.5"

  val akkaLibs: Seq[ModuleID] = Seq(akkaPersistenceTyped, akkaHttp, akkaTypedTestKit, akkaPersistenceMongoJournal)
  val testLibs: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % scalaTestV % Test,
    "org.scalamock" %% "scalamock" % scalaMockV % Test,
    "org.scalacheck" %% "scalacheck" % scalaCheckV % Test,
  )

}
