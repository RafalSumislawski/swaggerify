
name := "swaggerify"
organization := "swaggerify"

version := "0.3.1"
val sVersion = "2.12.12"
scalaVersion := sVersion

val scalaReflect = "org.scala-lang" % "scala-reflect" % sVersion

val magnolia = "com.propensive" %% "magnolia" % "0.16.0"

val swaggerVersion = "1.6.2"
val swaggerModels = "io.swagger" % "swagger-models" % swaggerVersion
val swaggerCore = "io.swagger" % "swagger-core" % swaggerVersion

val swaggerValidator = ("io.swagger" % "swagger-validator" % "1.0.7").excludeAll(ExclusionRule("ch.qos.logback"),
  ExclusionRule("org.eclipse.jetty"), ExclusionRule("org.glassfish"), ExclusionRule("org.glassfish.hk2"),
  ExclusionRule("org.glassfish.hk2.external"), ExclusionRule("org.glassfish.jersey.bundlers"),
  ExclusionRule("org.glassfish.jersey.containers"), ExclusionRule("org.glassfish.jersey.core"), ExclusionRule("org.glassfish.jersey.media"))

val commonsIo = "commons-io" % "commons-io" % "2.7"

val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
val log4jApi = "org.apache.logging.log4j" % "log4j-api" % "2.13.3"
val log4jSlf4j = "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.13.3"
val slf4j = "org.slf4j" % "slf4j-api" % "1.7.30"
val lmaxDisruptor = "com.lmax" % "disruptor" % "3.4.2"
val allLogging = Seq(slf4j, log4jApi, log4jSlf4j, lmaxDisruptor, scalaLogging)

val circeVersion = "0.13.0"
val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
val circeParser = "io.circe" %% "circe-parser" % circeVersion
val circeYaml = "io.circe" %% "circe-yaml" % "0.13.1"
val allCirce = Seq(circeGeneric, circeParser, circeYaml)

val specs2Version = "4.10.1"
val specsCore = "org.specs2" %% "specs2-core" % specs2Version
val specsScalaCheck = "org.specs2" %% "specs2-scalacheck" % specs2Version
val allSpecs = Seq(specsCore, specsScalaCheck)

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(magnolia, swaggerModels, swaggerCore, scalaReflect) ++
  (Seq(swaggerValidator, commonsIo) ++ allLogging ++ allCirce ++ allSpecs).map(_ % Test)

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-Xfuture",
  "-Ywarn-unused",
  "-Xfatal-warnings",
  "-language:higherKinds",
  "-Ypartial-unification"
)

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")

cancelable in Global := true
crossPaths := false
aggregate in update := false
