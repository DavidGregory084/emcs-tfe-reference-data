/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.emcstfereferencedata.connectors


import play.api.http.{HeaderNames, MimeTypes, Status}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.emcstfereferencedata.connector.EMCSStubConnector
import uk.gov.hmrc.emcstfereferencedata.mocks.config.MockAppConfig
import uk.gov.hmrc.emcstfereferencedata.mocks.connectors.MockHttpClient
import uk.gov.hmrc.emcstfereferencedata.models.response.{HelloWorldResponse, OtherDataReference, OtherDataReferenceList, OtherDataReferenceListErrorModel}
import uk.gov.hmrc.emcstfereferencedata.support.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class EMCSStubConnectorSpec extends UnitSpec with Status with MimeTypes with HeaderNames with MockAppConfig with MockHttpClient {

  trait Test {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

    val connector = new EMCSStubConnector(mockHttpClient, mockAppConfig)

    val baseUrl: String = "http://test-BaseUrl"
    MockedAppConfig.referenceDataBaseUrl.returns(baseUrl)
  }

  "getMessage" should {
    "return a Right" when {
      "downstream call is successful" in new Test {
        val response: HttpResponse = HttpResponse(status = Status.OK, json = Json.toJson(HelloWorldResponse("test message")), headers = Map.empty)

        MockHttpClient.get(s"$baseUrl/hello-world").returns(Future.successful(response))

        await(connector.getMessage()) shouldBe Right(HelloWorldResponse("test message"))
      }
    }
    "return a Left" when {
      "downstream call is successful but doesn't match expected JSON" in new Test {

        case class TestModel(field: String)

        object TestModel {
          implicit val format: OFormat[TestModel] = Json.format
        }

        val response: HttpResponse = HttpResponse(status = Status.OK, json = Json.toJson(TestModel("test message")), headers = Map.empty)

        MockHttpClient.get(s"$baseUrl/hello-world").returns(Future.successful(response))

        await(connector.getMessage()) shouldBe Left("JSON validation error")
      }
      "downstream call is unsuccessful" in new Test {
        val response: HttpResponse = HttpResponse(status = Status.INTERNAL_SERVER_ERROR, json = Json.toJson(HelloWorldResponse("test message")), headers = Map.empty)

        MockHttpClient.get(s"$baseUrl/hello-world").returns(Future.successful(response))

        await(connector.getMessage()) shouldBe Left("Unexpected downstream response status")
      }
    }
  }

  "getOtherDataReferenceList" should {
    "return a success model" when {
      "downstream call is successful" in new Test {
        val response: HttpResponse = HttpResponse(
          status = Status.OK,
          json = Json.toJson(
            OtherDataReferenceList(
              List(OtherDataReference("test", "test", "test"))
            )
          ),
          headers = Map.empty
        )

        MockHttpClient.get(s"$baseUrl/otherReferenceDataTransportMode").returns(Future.successful(response))

        await(connector.getOtherDataReferenceList()) shouldBe OtherDataReferenceList(
          List(OtherDataReference("test", "test", "test"))
        )
      }
    }
    "return an error model" when {
      "downstream call is successful but doesn't match expected JSON" in new Test {

        case class TestModel(field: String)

        object TestModel {
          implicit val format: OFormat[TestModel] = Json.format
        }

        val response: HttpResponse = HttpResponse(status = Status.OK, json = Json.toJson(TestModel("test message")), headers = Map.empty)

        MockHttpClient.get(s"$baseUrl/otherReferenceDataTransportMode").returns(Future.successful(response))

        await(connector.getOtherDataReferenceList()) shouldBe OtherDataReferenceListErrorModel(500, "JSON validation error")
      }
      "downstream call is unsuccessful" in new Test {

        MockHttpClient.get(s"$baseUrl/otherReferenceDataTransportMode").returns(Future.successful(HttpResponse(Status.INTERNAL_SERVER_ERROR, responseString = Some("test message"))))

        await(connector.getOtherDataReferenceList()) shouldBe OtherDataReferenceListErrorModel(500, "test message")
      }
    }
  }
}