
name := "swaggerify"
organization := "swaggerify"

version := "0.2.0"
val sVersion = "2.12.8"
scalaVersion := sVersion

val scalaReflect = "org.scala-lang" % "scala-reflect" % sVersion

val magnolia = ("com.propensive" %% "magnolia" % "0.10.0").excludeAll(ExclusionRule("org.scala-lang", "scala-compiler"))

val swaggerVersion = "1.5.22"
val swaggerModels = "io.swagger" % "swagger-models" % swaggerVersion
val swaggerCore = "io.swagger" % "swagger-core" % swaggerVersion

val swaggerValidator = ("io.swagger" % "swagger-validator" % "1.0.6").excludeAll(ExclusionRule("ch.qos.logback"),
  ExclusionRule("org.eclipse.jetty"), ExclusionRule("org.glassfish"), ExclusionRule("org.glassfish.hk2"),
  ExclusionRule("org.glassfish.hk2.external"), ExclusionRule("org.glassfish.jersey.bundlers"),
  ExclusionRule("org.glassfish.jersey.containers"), ExclusionRule("org.glassfish.jersey.core"), ExclusionRule("org.glassfish.jersey.media"))

val commonsIo = "commons-io" % "commons-io" % "2.6"

val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
val log4jApi = "org.apache.logging.log4j" % "log4j-api" % "2.11.2"
val log4jSlf4j = "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.11.2"
val slf4j = "org.slf4j" % "slf4j-api" % "1.7.26"
val lmaxDisruptor = "com.lmax" % "disruptor" % "3.4.2"
val allLogging = Seq(slf4j, log4jApi, log4jSlf4j, lmaxDisruptor, scalaLogging)

val circeVersion = "0.11.1"
val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
val circeParser = "io.circe" %% "circe-parser" % circeVersion
val circeJava8 = "io.circe" %% "circe-java8" % circeVersion
val circeYaml = "io.circe" %% "circe-yaml" % "0.9.0"
val allCirce = Seq(circeGeneric, circeParser, circeJava8, circeYaml)

val http4sCore = "org.http4s" %% "http4s-core" % "0.20.0-M5"
val rhoSwagger = "org.http4s" %% "rho-swagger" % "0.19.0-M5"

val specs2Version = "4.5.1"
val specsCore = "org.specs2" %% "specs2-core" % specs2Version
val specsScalaCheck = "org.specs2" %% "specs2-scalacheck" % specs2Version
val allSpecs = Seq(specsCore, specsScalaCheck)

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(magnolia, swaggerModels, swaggerCore) ++
  (Seq(swaggerValidator, commonsIo) ++ allLogging ++ allCirce ++ allSpecs ).map(_ % Test)

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-Xfuture",
  "-Ywarn-unused",
  "-Xfatal-warnings"
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")

cancelable in Global := true
crossPaths := false
aggregate in update := false
