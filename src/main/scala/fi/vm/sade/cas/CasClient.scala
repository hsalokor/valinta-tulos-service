package fi.vm.sade.cas

import fi.vm.sade.valintatulosservice.Logging
import fi.vm.sade.valintatulosservice.http.DefaultHttpClient

import scala.xml.{Elem, XML}

class CasClient(casRoot: String) extends Logging {
  def validateServiceTicket(ticket: CasTicket): CasResponse = {
    def failure(error: String) = CasResponse(false, Some(error))
    def parseCasResponse(response: String) = {
      val responseXml: Elem = XML.loadString(response)
      val success = !(responseXml \\ ("authenticationSuccess")).isEmpty
      CasResponse(success, None)
    }

    val casUrl: String = casRoot + "/serviceValidate"
    val (responseCode, headers, resultString) = DefaultHttpClient.httpGet(casUrl).param("service", ticket.service).param("ticket", ticket.ticket)
      .responseWithHeaders

    responseCode match {
      case 200 => parseCasResponse(resultString)
      case _ => failure("CAS server at " + casUrl + " responded with status "+ responseCode)
    }
  }

  def getServiceTicket(service: CasTicketRequest): Option[String] = {
    val casTicketUrl = casRoot + "/v1/tickets"

    def getTicketGrantingTicket(username: String, password: String): Option[String] = {
      val (responseCode, headersMap, resultString) = DefaultHttpClient.httpPost(casTicketUrl, None)
        .param("username", username)
        .param("password", password)
        .responseWithHeaders

      responseCode match {
        case 201 => {
          val ticketPattern = """.*/([^/]+)""".r
          val headerValue = headersMap.getOrElse("Location",List("no location header")).head
          ticketPattern.findFirstMatchIn(headerValue) match {
            case Some(matched) => Some(matched.group(1))
            case None => {
              logger.warn("Successful ticket granting request, but no ticket found! Location header: " + headerValue)
              None
            }
          }
        }
        case _ => {
          logger.error("Invalid response code (" + responseCode + ") from CAS server. Response body: " + resultString)
          None
        }
      }
    }

    getTicketGrantingTicket(service.username, service.password).flatMap { ticket =>
      DefaultHttpClient.httpPost(casTicketUrl + "/" + ticket, None)
        .param("service", service.service)
        .response
    }
  }
}