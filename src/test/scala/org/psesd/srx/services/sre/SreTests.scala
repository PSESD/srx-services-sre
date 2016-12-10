package org.psesd.srx.services.sre

import org.psesd.srx.shared.core.{SrxResourceErrorResult, SrxResourceResult}
import org.psesd.srx.shared.core.exceptions.ArgumentInvalidException
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.FunSuite

class SreTests extends FunSuite {

  test("constructor") {
    val id = "999"
    val sreXml = <sre><localId>{id}</localId></sre>
    val sre = new Sre(id, sreXml.toXmlString)
    assert(sre.id.equals(id))
    assert(sre.sre.contains("<sre>"))
  }

  test("factory") {
    val id = "999"
    val sreXml = <sre><localId>{id}</localId></sre>
    val sre = Sre(sreXml.toXmlString)
    assert(sre.id.equals(id))
    assert(sre.sre.contains("<sre>"))
  }

  test("factory invalid") {
    val thrown = intercept[ArgumentInvalidException] {
      val sre = Sre(<foo></foo>, None)
    }

    assert(thrown.getMessage.equals("The root element 'foo' is invalid."))
  }

  test("node") {
    val id = "999"
    val sreXml = <sre><localId>{id}</localId></sre>
    val sre = Sre(sreXml, None)
    assert(sre.id.equals(id))
    assert(sre.sre.contains("<sre>"))
  }

  test("delete not allowed") {
    val result = Sre.delete(TestValues.sreParameters)
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.MethodNotAllowed)
    assert(result.exceptions.head.getMessage == "SRE DELETE method not allowed.")
  }

  test("update not allowed") {
    val result = Sre.update(TestValues.sreUnencrypted, TestValues.sreParameters)
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.MethodNotAllowed)
    assert(result.exceptions.head.getMessage == "SRE UPDATE method not allowed.")
  }

  test("query not allowed") {
    val result = Sre.query(TestValues.sreParameters)
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.MethodNotAllowed)
    assert(result.exceptions.head.getMessage == "SRE QUERY method not allowed.")
  }

  test("create no zoneId") {
    val result = Sre.create(TestValues.sreUnencrypted, List[SifRequestParameter]())
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.exceptions.head.getMessage == "The zoneId parameter cannot be null, empty, or whitespace.")
    assert(result.toXml.isEmpty)
  }

  ignore("create invalid") {
    val result = Sre.create(TestValues.sreInvalid, TestValues.sreParameters).asInstanceOf[SrxResourceErrorResult]
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.exceptions.head.getMessage.contains("The request body is invalid."))
  }

  ignore("create valid unencrypted") {
    val result = Sre.create(TestValues.sreUnencrypted, TestValues.sreParameters).asInstanceOf[SreResult]
    assert(result.success)
    assert(result.exceptions.isEmpty)
    assert(result.toXml.get.toXmlString.contains("id=\"%s\"".format("0")))
  }

  ignore("create valid encrypted") {
    val result = Sre.create(TestValues.sreEncrypted, TestValues.sreParameters).asInstanceOf[SreResult]
    assert(result.success)
    assert(result.exceptions.isEmpty)
    assert(result.toXml.get.toXmlString.contains("id=\"%s\"".format("0")))
  }


}
