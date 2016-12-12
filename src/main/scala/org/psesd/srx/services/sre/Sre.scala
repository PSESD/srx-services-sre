package org.psesd.srx.services.sre

import java.security.SecureRandom

import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.json4s.JValue
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException, ArgumentNullOrEmptyOrWhitespaceException, SrxResourceNotFoundException}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.io.{AmazonS3Client, SftpClient}
import org.psesd.srx.shared.core.sif.SifRequestAction.SifRequestAction
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestAction, SifRequestParameter, _}
import org.psesd.srx.shared.core._
import org.psesd.srx.shared.core.config.{ConfigCache, SftpConfig, ZoneConfig}

import scala.xml.Node

/** Represents a Student Record Exchange entity.
  *
  * @version 1.0
  * @since 1.0
  * @author Margarett Ly (iTrellis, LLC)
  */
class Sre(val id: String, val sre: String) extends SrxResource {
  if (id.isNullOrEmpty) {
    throw new ArgumentNullOrEmptyOrWhitespaceException("id parameter")
  }
  if (sre.isNullOrEmpty) {
    throw new ArgumentNullOrEmptyOrWhitespaceException("sre parameter")
  }

  def toJson: JValue = {
    toXml.toJsonStringNoRoot.toJson
  }

  def toXml: Node = {
    sre.toXml
  }

}

/** Represents a Student Record Exchange method result.
  *
  * @version 1.0
  * @since 1.0
  * @author Margarett Ly (iTrellis, LLC)
  */
class SreResult(
                  requestAction: SifRequestAction,
                  httpStatusCode: Int
                ) extends SrxResourceResult {
  statusCode = httpStatusCode

  def toJson: Option[JValue] = {
    requestAction match {

      case SifRequestAction.Create =>
        Option(SifCreateResponse().addResult("0", statusCode).toXml.toJsonString.toJson)

      case _ =>
        None
    }
  }

  def toXml: Option[Node] = {

    requestAction match {

      case SifRequestAction.Create =>
        Option(SifCreateResponse().addResult("0", statusCode).toXml)

      case _ =>
        None
    }
  }
}

/** Student Record Exchange methods.
  *
  * @version 1.0
  * @since 1.0
  * @author Margarett Ly (iTrellis, LLC)
  */
object Sre extends SrxResourceService {
  def apply(sre: String): Sre = {
    if (sre == null) {
      throw new ArgumentNullException("sre parameter")
    }
    Sre(sre.toXml, None)
  }

  def apply(requestBody: SrxRequestBody, parameters: Option[List[SifRequestParameter]]): Sre = {
    if (requestBody == null) {
      throw new ArgumentNullException("requestBody parameter")
    }

    // TODO: find way to extract from incoming student record
    val id = "0"

    new Sre(
      id,
      requestBody.getValue.orNull
    )
  }

  def apply(sreXml: Node, parameters: Option[List[SifRequestParameter]]): Sre = {
    if (sreXml == null) {
      throw new ArgumentNullException("sreXml parameter")
    }
    val rootElementName = sreXml.label.toLowerCase
    if (rootElementName != "sre") {
      throw new ArgumentInvalidException("root element '%s'".format(rootElementName))
    }
    val id = (sreXml \ "localId").textOption.getOrElse("")
    new Sre(
      id,
      sreXml.toXmlString
    )
  }

  def create(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {

      val sreResource = resource.asInstanceOf[Sre]
      val sre = sreResource.sre
      val zoneId = getZoneIdFromRequestParameters(parameters)

      if (zoneId.isEmpty) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentNullOrEmptyOrWhitespaceException("zoneId parameter"))
      } else {
        val zoneConfig = new ZoneConfig(zoneId.get, SreServer.srxService.service.name)
        val sreXml = (zoneConfig.zoneConfigXml \ "resource").find(r => (r \ "@type").text.toLowerCase() == "sre")
        val sftpXml = (sreXml.get \ "destination" \ "sftp")
        val sftpConfig = new SftpConfig(sftpXml)
        val sftpClient = new SftpClient(sftpConfig)

        sftpClient.write(getXmlFileName, sre.getBytes)

        new SreResult(
          SifRequestAction.Create,
          SifRequestAction.getSuccessStatusCode(SifRequestAction.Create)
        )
      }
    } catch {
      case e: Exception =>
        val test = e
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def delete(parameters: List[SifRequestParameter]): SrxResourceResult = {
    SrxResourceErrorResult(SifHttpStatusCode.MethodNotAllowed, new Exception("SRE DELETE method not allowed."))
  }

  def query(parameters: List[SifRequestParameter]): SrxResourceResult = {
    SrxResourceErrorResult(SifHttpStatusCode.MethodNotAllowed, new Exception("SRE QUERY method not allowed."))
  }

  def update(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    SrxResourceErrorResult(SifHttpStatusCode.MethodNotAllowed, new Exception("SRE UPDATE method not allowed."))
  }

  protected def getKeyIdFromRequestParameters(parameters: List[SifRequestParameter]): Option[String] = {
    var result: Option[String] = None
    try {
      val id = getIdFromRequestParameters(parameters)
      if (id.isDefined) {
        result = Some(id.get)
      }
    } catch {
      case e: Exception =>
        result = Some("-1")
    }
    result
  }

  private def getZoneIdFromRequestParameters(parameters: List[SifRequestParameter]): Option[String] = {
    if (parameters != null && parameters.nonEmpty) {
      val idParameter = parameters.find(p => p.key.toLowerCase == "zoneid").orNull
      if (idParameter != null) {
        Some(idParameter.value)
      } else {
        None
      }
    } else {
      None
    }
  }

  def log(method: String, zoneId: String, studentId: String, parameters: List[SifRequestParameter]): Unit = {
    val generatorId = getRequestParameter(parameters, SifHeader.GeneratorId.toString())
    val requestId = getRequestParameter(parameters, SifHeader.RequestId.toString())
    val uri = getRequestParameter(parameters, "uri")
    val sourceIp = getRequestParameter(parameters, SifHttpHeader.ForwardedFor.toString())
    val userAgent = getRequestParameter(parameters, SifHttpHeader.UserAgent.toString())
    val contextId = getRequestParameter(parameters, "contextId")

    SrxMessageService.createMessage(
      SreServer.srxService.service.name,
      SrxMessage(
        SreServer.srxService,
        SifMessageId(),
        SifTimestamp(),
        Some("SRE"),
        Some(method),
        Some("success"),
        generatorId,
        requestId,
        Some(SifZone(zoneId)),
        { if (contextId.isDefined) Some(SifContext(contextId.get)) else None },
        Some(studentId),
        "%s successful for student '%s' in zone '%s'.".format(method, studentId, zoneId),
        uri,
        userAgent,
        sourceIp,
        None,
        None
      )
    )
  }

  /** Gets a time-based XML file name.
    * */
  private def getXmlFileName: String = {

    val prefix = DateTime.now(DateTimeZone.UTC).toString(DateTimeFormat.forPattern("yyyyMMddHHmmssSSS"))
    val suffix = scala.math.abs(new SecureRandom().nextInt)

    s"$prefix-$suffix.xml"
  }
}
