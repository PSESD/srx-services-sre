package org.psesd.srx.services.sre

/** Provides information about current build.
  *
  * @version 1.0
  * @since 1.0
  * @author Margarett Ly (iTrellis, LLC)
  */
object Build {
  val name: String = org.psesd.srx.services.sre.BuildInfo.name

  val version: String = org.psesd.srx.services.sre.BuildInfo.version

  val scalaVersion: String = org.psesd.srx.services.sre.BuildInfo.scalaVersion

  val sbtVersion: String = org.psesd.srx.services.sre.BuildInfo.sbtVersion

  val buildNumber: Int = org.psesd.srx.services.sre.BuildInfo.buildNumber

  val javaVersion: String = scala.util.Properties.javaVersion
}