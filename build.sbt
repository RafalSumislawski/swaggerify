
name := "swaggerify"
organization := "swaggerify"
version := "0.4.0-SNAPSHOT"

val scala212 = "2.12.12"
val scala213 = "2.13.3"
val sVersion = scala213
val supportedScalaVersions = List(scala212, scala213)

scalaVersion := sVersion
crossScalaVersions := supportedScalaVersions

val magnolia = "com.propensive" %% "magnolia" % "0.17.0"

val swaggerVersion = "1.6.2"
val swaggerModels = "io.swagger" % "swagger-models" % swaggerVersion
val swaggerCore = "io.swagger" % "swagger-core" % swaggerVersion

val swaggerValidator = ("io.swagger" % "swagger-validator" % "1.0.7").excludeAll(ExclusionRule("ch.qos.logback"),
  ExclusionRule("org.eclipse.jetty"), ExclusionRule("org.glassfish"), ExclusionRule("org.glassfish.hk2"),
  ExclusionRule("org.glassfish.hk2.external"), ExclusionRule("org.glassfish.jersey.bundlers"),
  ExclusionRule("org.glassfish.jersey.containers"), ExclusionRule("org.glassfish.jersey.core"), ExclusionRule("org.glassfish.jersey.media"))

val commonsIo = "commons-io" % "commons-io" % "2.8.0"

val collectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.2.0"

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

val specs2Version = "4.10.5"
val specsCore = "org.specs2" %% "specs2-core" % specs2Version
val specsScalaCheck = "org.specs2" %% "specs2-scalacheck" % specs2Version
val allSpecs = Seq(specsCore, specsScalaCheck)

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(magnolia, swaggerModels, swaggerCore, collectionCompat) ++
  Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value) ++
  (Seq(swaggerValidator, commonsIo) ++ allLogging ++ allCirce ++ allSpecs).map(_ % Test)


scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-Ywarn-unused",
  "-Xfatal-warnings",
  "-language:higherKinds"
) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 12)) => Seq("-Ypartial-unification", "-Xfuture")
    case Some((2, 13)) => Seq.empty
    case _ => Seq.empty
  })


addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")

cancelable in Global := true
aggregate in update := false
