package org.psesd.srx.services.sre

import org.scalatest.FunSuite

class BuildTests extends FunSuite {

  test("package") {
    assert(Build.name == "srx-services-sre")
    assert(Build.version == "1.0")
    assert(Build.buildNumber > 0)
  }

  test("framework") {
    assert(!Build.javaVersion.isEmpty)
    assert(Build.scalaVersion == "2.11.8")
    assert(!Build.sbtVersion.isEmpty)
  }

}
