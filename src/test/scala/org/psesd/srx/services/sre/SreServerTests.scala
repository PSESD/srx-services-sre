package org.psesd.srx.services.sre

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.util.EntityUtils
import org.http4s.dsl._
import org.http4s.{Method, Request}
import org.psesd.srx.shared.core.CoreResource
import org.psesd.srx.shared.core.config.Environment
import org.psesd.srx.shared.core.extensions.HttpTypeExtensions._
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif._
import org.scalatest.FunSuite

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

class SreServerTests extends FunSuite {

  private final val ServerDuration = 8000
  private lazy val tempServer = Future {
    delayedInterrupt(ServerDuration)
    intercept[InterruptedException] {
      startServer()
    }
  }
  private val pendingInterrupts = new ThreadLocal[List[Thread]] {
    override def initialValue = Nil
  }

  test("service") {
    assert(SreServer.srxService.service.name.equals(Build.name))
    assert(SreServer.srxService.service.version.equals(Build.version + "." + Build.buildNumber))
    assert(SreServer.srxService.buildComponents(0).name.equals("java"))
    assert(SreServer.srxService.buildComponents(0).version.equals(Build.javaVersion))
    assert(SreServer.srxService.buildComponents(1).name.equals("scala"))
    assert(SreServer.srxService.buildComponents(1).version.equals(Build.scalaVersion))
    assert(SreServer.srxService.buildComponents(2).name.equals("sbt"))
    assert(SreServer.srxService.buildComponents(2).version.equals(Build.sbtVersion))
  }

  test("ping (localhost)") {
    if (Environment.isLocal) {
      val expected = "true"
      var actual = ""
      tempServer onComplete {
        case Success(x) =>
          assert(actual.equals(expected))
        case _ =>
      }

      // wait for server to init
      Thread.sleep(2000)

      // ping server and collect response
      val httpclient: CloseableHttpClient = HttpClients.custom().disableCookieManagement().build()
      val httpGet = new HttpGet("http://localhost:%s/ping".format(Environment.getPropertyOrElse("SERVER_PORT", "80")))
      val response = httpclient.execute(httpGet)
      actual = EntityUtils.toString(response.getEntity)
    }
  }

  test("root") {
    val getRoot = Request(Method.GET, uri("/"))
    val task = SreServer.service.run(getRoot)
    val response = task.run
    assert(response.status.code.equals(SifHttpStatusCode.Ok))
  }

  test("ping") {
    if (Environment.isLocal) {
      val getPing = Request(Method.GET, uri("/ping"))
      val task = SreServer.service.run(getPing)
      val response = task.run
      val body = response.body.value
      assert(response.status.code.equals(SifHttpStatusCode.Ok))
      assert(body.equals(true.toString))
    }
  }

  test("info (localhost)") {
    if (Environment.isLocal) {
      val sifRequest = new SifRequest(TestValues.sifProvider, CoreResource.Info.toString)
      val response = new SifConsumer().query(sifRequest)
      val responseBody = response.body.getOrElse("")
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
      assert(response.contentType.get.equals(SifContentType.Xml))
      assert(responseBody.contains("<service>"))
    }
  }


  /* SRE ROUTES */

  test("update SRE not allowed") {
    if (Environment.isLocal) {
      val resource = "sres/999"
      val sifRequest = new SifRequest(TestValues.sifProvider, resource, SifZone("test"), SifContext())
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(TestValues.sreXml.toXmlString)
      val response = new SifConsumer().update(sifRequest)
      assert(response.statusCode.equals(SifHttpStatusCode.MethodNotAllowed))
    }
  }

  test("create all SREs not allowed") {
    if (Environment.isLocal) {
      val resource = "sres"
      val sifRequest = new SifRequest(TestValues.sifProvider, resource, SifZone("test"), SifContext())
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(TestValues.sreXml.toXmlString)
      val response = new SifConsumer().create(sifRequest)
      assert(response.statusCode.equals(SifHttpStatusCode.MethodNotAllowed))
    }
  }

  test("create SRE valid") {
    if (Environment.isLocal) {
      val resource = "sres"
      val sifRequest = new SifRequest(TestValues.sifProvider, resource, SifZone("test"), SifContext())
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(TestValues.sreXml.toXmlString)
      val response = new SifConsumer().create(sifRequest)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("query all SREs not allowed") {
    if (Environment.isLocal) {
      val resource = "sres"
      val sifRequest = new SifRequest(TestValues.sifProvider, resource, SifZone("test"), SifContext())
      sifRequest.generatorId = Some(TestValues.generatorId)
      val response = new SifConsumer().query(sifRequest)
      assert(response.statusCode.equals(SifHttpStatusCode.MethodNotAllowed))
    }
  }

  test("delete all SREs not allowed") {
    if (Environment.isLocal) {
      val resource = "sres"
      val sifRequest = new SifRequest(TestValues.sifProvider, resource, SifZone("test"), SifContext())
      sifRequest.generatorId = Some(TestValues.generatorId)
      val response = new SifConsumer().delete(sifRequest)
      assert(response.statusCode.equals(SifHttpStatusCode.MethodNotAllowed))
    }
  }

  test("delete configCache valid") {
    if (Environment.isLocal) {
      val resource = "configcache"
      val sifRequest = new SifRequest(TestValues.sifProvider, resource, SifZone("test"), SifContext())
      sifRequest.generatorId = Some(TestValues.generatorId)
      val response = new SifConsumer().delete(sifRequest)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }


  private def delayedInterrupt(delay: Long) {
    delayedInterrupt(Thread.currentThread, delay)
  }

  private def delayedInterrupt(target: Thread, delay: Long) {
    val t = new Thread {
      override def run() {
        Thread.sleep(delay)
        target.interrupt()
      }
    }
    pendingInterrupts.set(t :: pendingInterrupts.get)
    t.start()
  }

  private def startServer(): Unit = {
    if (Environment.isLocal) {
      SreServer.main(Array[String]())
    }
  }

  private def printlnResponse(response: SifResponse): Unit = {
    println("STATUS CODE: " + response.statusCode.toString)
    for (header <- response.getHeaders) {
      println("%s=%s".format(header._1, header._2))
    }
    println(response.getBody(SifContentType.Xml))
    /*
    if(response.bodyXml.isDefined) {
      println(response.getBody(SifContentType.Xml))
    } else {
      println(response.body.getOrElse(""))
    }
    */
  }

}
