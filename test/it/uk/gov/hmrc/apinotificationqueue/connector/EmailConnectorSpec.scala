/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.apinotificationqueue.connector

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.ACCEPTED
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.apinotificationqueue.model.{Email, SendEmailRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec

class EmailConnectorSpec extends UnitSpec
  with GuiceOneAppPerSuite
  with MockitoSugar {

  private val emailPort = sys.env.getOrElse("WIREMOCK", "11111").toInt
  private val emailHost = "localhost"
  private val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(emailPort))

  override implicit lazy val app: Application = GuiceApplicationBuilder().configure(Map(
      "microservice.services.email.host" -> emailHost,
      "microservice.services.email.port" -> emailPort,
      "microservice.services.email.context" -> "/hmrc/email"
    )).build()

  trait Setup {
    val mockHttpClient: HttpClient = mock[HttpClient]

    val sendEmailRequest = SendEmailRequest(List(Email("some-email@address.com")), "some-template-id",
      Map("parameters" -> "some-parameter"), force = false)

    implicit val hc = HeaderCarrier()
    lazy val connector: EmailConnector = app.injector.instanceOf[EmailConnector]
  }

  "EmailConnector" should {
    "successfully email" in new Setup {
      wireMockServer.start()
      WireMock.configureFor(emailHost, emailPort)
      stubFor(post(urlEqualTo("/hmrc/email")).willReturn(aResponse().withStatus(ACCEPTED)))

      await(connector.send(sendEmailRequest))

      verify(1, postRequestedFor(urlEqualTo("/hmrc/email")))
    }
  }
}
