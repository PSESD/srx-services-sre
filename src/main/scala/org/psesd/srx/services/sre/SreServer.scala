package org.psesd.srx.services.sre

import org.http4s._
import org.http4s.dsl._
import org.psesd.srx.shared.core._
import org.psesd.srx.shared.core.config.{ConfigCache, Environment}
import org.psesd.srx.shared.core.sif._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext

/** SRX SRE server.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  **/
object SreServer extends SrxServer {

  private final val ServerUrlKey = "SERVER_URL"
  private final val sreResource = SrxResourceType.Sres.toString
  private final val configCacheResource = "configcache"

  val sifProvider: SifProvider = new SifProvider(
    SifProviderUrl(Environment.getProperty(ServerUrlKey)),
    SifProviderSessionToken(Environment.getProperty(Environment.SrxSessionTokenKey)),
    SifProviderSharedSecret(Environment.getProperty(Environment.SrxSharedSecretKey)),
    SifAuthenticationMethod.SifHmacSha256
  )

  val srxService: SrxService = new SrxService(
    new SrxServiceComponent(Build.name, Build.version + "." + Build.buildNumber),
    List[SrxServiceComponent](
      new SrxServiceComponent("java", Build.javaVersion),
      new SrxServiceComponent("scala", Build.scalaVersion),
      new SrxServiceComponent("sbt", Build.sbtVersion)
    )
  )

  override def serviceRouter(implicit executionContext: ExecutionContext) = HttpService {

    case req@GET -> Root =>
      Ok()

    case _ -> Root =>
      NotImplemented()

    case req@GET -> Root / _ if services(req, SrxResourceType.Ping.toString) =>
      Ok(true.toString)

    case req@GET -> Root / _ if services(req, SrxResourceType.Info.toString) =>
      respondWithInfo(getDefaultSrxResponse(req))


    /* SRE */

    case req@GET -> Root / _ if services(req, sreResource) =>
      MethodNotAllowed()

    case req@GET -> Root / `sreResource` / _ =>
      MethodNotAllowed()

    case req@POST -> Root / _ if services(req, sreResource) =>
      executeRequest(req, addSreParameters(req), sreResource, Sre, Sre.apply)

    case req@PUT -> Root / _ if services(req, sreResource) =>
      MethodNotAllowed()

    case req@PUT -> Root / `sreResource` / _ =>
      MethodNotAllowed()

    case req@DELETE -> Root / _ if services(req, sreResource) =>
      MethodNotAllowed()

    case req@DELETE -> Root / `sreResource` / _ =>
      MethodNotAllowed()


    /* CONFIG CACHE */

    case req@DELETE -> Root / _ if services(req, configCacheResource) =>
      executeRequest(req, None, configCacheResource, ConfigCache)


    case _ =>
      NotFound()
  }

  def addSreParameters(req: Request): Option[List[SifRequestParameter]] = {
    val params = ArrayBuffer[SifRequestParameter]()
    for (h <- req.headers) {
      val headerName = h.name.value.toLowerCase
      if(headerName == "X-PSESD-IV".toLowerCase) params += SifRequestParameter("X-PSESD-IV", h.value)
      if(headerName == "generatorId".toLowerCase) params += SifRequestParameter("generatorId", h.value)
    }
    Some(params.toList)
  }

}
