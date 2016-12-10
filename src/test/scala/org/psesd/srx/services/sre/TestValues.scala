package org.psesd.srx.services.sre

import org.http4s.Header.Raw
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Headers, Method, Request, Uri}
import org.psesd.srx.shared.core.config.Environment
import org.psesd.srx.shared.core.{SrxRequest, SrxRequestBody}
import org.psesd.srx.shared.core.extensions.HttpTypeExtensions._
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif._

/**
  * Created by margarettly on 12/7/16.
  */
object TestValues {
  lazy val authorization = new SifAuthorization(sifProvider, timestamp)
  val id = "999"

  val sreParameters = List[SifRequestParameter](SifRequestParameter("iv", "X-PSESD-IV"), SifRequestParameter("zoneId", "test"))
  val sreXml = <sre><localId>{id}</localId></sre>
  lazy val testIv = "e675f725e675f725"

  lazy val sreRequest = getRequest(sreXml.toXmlString, testIv, null)
  lazy val requestBodyUnencrypted = new SrxRequestBody(sreRequest)

  lazy val testPassword = Environment.getProperty("AES_PASSWORD").toCharArray
  lazy val testSalt = Environment.getProperty("AES_SALT").getBytes
  lazy val sreXmlEncrypted = SifEncryptor.encryptString(testPassword, testSalt, sreXml.toXmlString, testIv.getBytes)
  lazy val sreRequestEncrypted = getRequest(sreXmlEncrypted, testIv, null)
  lazy val requestBodyEncrypted = new SrxRequestBody(sreRequestEncrypted)

  val sreUnencrypted = Sre(requestBodyUnencrypted, Some(sreParameters))
  val sreEncrypted = Sre(requestBodyEncrypted, Some(sreParameters))

  lazy val sreXmlInvalid = "<foo></food>"
  lazy val sreRequestInvalid = getRequest(sreXmlInvalid, testIv, null)
  lazy val requestBodyInvalid = new SrxRequestBody(sreRequestInvalid)
  lazy val sreInvalid = Sre(requestBodyInvalid, Some(List[SifRequestParameter]()))

  lazy val sifAuthenticationMethod = SifAuthenticationMethod.SifHmacSha256
  lazy val sessionToken = SifProviderSessionToken("ad53dbf6-e0a0-469f-8428-c17738eba43e")
  lazy val sharedSecret = SifProviderSharedSecret("pHkAuxdGGMWS")
  lazy val sifUrl: SifProviderUrl = SifProviderUrl("http://localhost:%s".format(Environment.getPropertyOrElse("SERVER_PORT", "80")))
  lazy val sifProvider = new SifProvider(sifUrl, sessionToken, sharedSecret, sifAuthenticationMethod)

  lazy val timestamp: SifTimestamp = SifTimestamp("2015-02-24T20:51:59.878Z")
  lazy val testSrxUri = new SifUri(TestValues.sifProvider.url + "/test_service/test_resource/test_resource_id;zoneId=test;contextId=test")

  lazy val generatorId = "srx-services-sre-test"

  private def getRequest(requestBody: String, iv: String, contentType: String): SrxRequest = {
    SrxRequest(
      sifProvider,
      new Request(
        method = Method.GET,
        new Uri(None, None, testSrxUri.toString),
        headers = Headers(
          Raw(CaseInsensitiveString(SifHeader.Authorization.toString), TestValues.authorization.toString),
          Raw(CaseInsensitiveString(SifHeader.Timestamp.toString), TestValues.timestamp.toString),
          Raw(CaseInsensitiveString(SifHeader.Iv.toString), iv),
          Raw(CaseInsensitiveString(SifHttpHeader.ContentType.toString), contentType)
        ),
        body = requestBody.toEntityBody
      )
    )
  }
}
