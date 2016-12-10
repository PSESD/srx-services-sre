name := "srx-services-sre"

version := "1.0"

scalaVersion := "2.11.8"

lazy val apacheHttpClientVersion = "4.5.2"
lazy val http4sVersion = "0.14.1"
lazy val jcraftVersion = "0.1.54"
lazy val jodaConvertVersion = "1.8.1"
lazy val jodaTimeVersion = "2.9.4"
lazy val json4sVersion = "3.4.0"
lazy val scalaTestVersion = "2.2.6"
lazy val apacheCommonsVersion = "2.1"
lazy val apachePoiVersion = "3.14"

// Date/time
libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % jodaTimeVersion,
  "org.joda" % "joda-convert" % jodaConvertVersion
)

// Test
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
)

// JSON
libraryDependencies ++= Seq(
  "org.json4s" % "json4s-native_2.11" % json4sVersion,
  "org.json4s" % "json4s-jackson_2.11" % json4sVersion
)

// HTTP Client
libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "httpclient" % apacheHttpClientVersion
)

// HTTP Server
libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion
)

// SFTP Client
libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-vfs2" % apacheCommonsVersion,
  "org.apache.poi" % "poi" % apachePoiVersion,
  "com.jcraft" % "jsch" % jcraftVersion
)

// Build info
lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  dependsOn(srxCore).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, BuildInfoKey.map(buildInfoBuildNumber) { case (k, v) =>
      "buildNumber" -> v
    }),
    buildInfoPackage := "org.psesd.srx.services.sre"
  )

lazy val srxCore = RootProject(uri("https://github.com/PSESD/srx-shared-core.git"))

enablePlugins(JavaServerAppPackaging)