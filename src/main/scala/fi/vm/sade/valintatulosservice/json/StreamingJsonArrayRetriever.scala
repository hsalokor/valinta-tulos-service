package fi.vm.sade.valintatulosservice.json

import com.fasterxml.jackson.core.{JsonFactory, JsonParser, JsonToken}
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.net.HttpHeaders
import fi.vm.sade.utils.cas.CasClient.JSessionId
import fi.vm.sade.utils.cas.CasParams
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig

import scalaj.http.{Http, HttpRequest, HttpResponse}
import scalaz.concurrent.Task
import scalaz.stream._


class StreamingJsonArrayRetriever(appConfig: AppConfig) extends Logging {
  private val jsonFactory = new JsonFactory()
  private val mapper = new ObjectMapper()

  def requestStreaming[T](targetService: String, url: String, targetClass: Class[T]): Iterator[T] = {
    val casParams = createCasParams(appConfig, targetService)
    val jsessionId = authenticate(casParams)

    val httpRequest: HttpRequest = Http(url).header(HttpHeaders.COOKIE, s"JSESSIONID=$jsessionId").compress(false)
    val response: HttpResponse[Iterator[T]] = httpRequest.execute(inputStream => {
      val jsonParser = jsonFactory.createParser(inputStream)
      var currentToken: JsonToken = null

      var arrayStartedOrDocumentFinished = false
      while (!arrayStartedOrDocumentFinished) {
        currentToken = jsonParser.nextToken()
        arrayStartedOrDocumentFinished = JsonToken.START_ARRAY == currentToken || currentToken == null
      }

      currentToken = jsonParser.nextToken() // advance to beginning of first object

      new Iterator[T] {
        override def hasNext = currentToken == JsonToken.START_OBJECT

        override def next(): T = {
          val parsed = parseObject(mapper, jsonParser, targetClass).getOrElse(throw new RuntimeException(s"Could not parse $targetClass object"))
          currentToken = jsonParser.nextToken()
          parsed
        }
      }.asInstanceOf[Iterator[T]]
    })
    response.body
  }

  private def parseObject[T](mapper: ObjectMapper, jsonParser: JsonParser, targetClass: Class[T]): Option[T] = try {
    Some(mapper.readValue(jsonParser, targetClass))
  } catch {
    case e: Exception =>
      logger.error(s"Could not parse $targetClass", e)
      None
  }

  private def authenticate(casParams: CasParams): String = {
    val sessions: Process[Task, JSessionId] = Process(casParams).toSource through appConfig.securityContext.casClient.casSessionChannel

    var jSessionId = "<not fetched>"
    sessions.map(sessionIdFromCas => jSessionId = sessionIdFromCas).run.run
    jSessionId
  }

  private def createCasParams(appConfig: AppConfig, targetService: String): CasParams = {
    CasParams(targetService, appConfig.settings.securitySettings.casUsername, appConfig.settings.securitySettings.casPassword)
  }
}
