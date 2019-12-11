package org.testit.pact.provider.http.clients

import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.Request
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.testit.pact.provider.http.Target
import utils.WireMockExtension
import java.util.*


internal class ApacheHttpClientIntegrationTest {

    companion object {

        @RegisterExtension
        @JvmField val wireMock = WireMockExtension()

        const val CONTENT_TYPE = "Content-Type"
        const val CORELATION_ID = "X-Correlation-Id"

        val BOOK_REQUEST_JSON = """
            {
              "title": "Hello World",
              "isbn": "1234567890123"
            }
            """.trimIndent()
        val BOOK_REQUEST_XML = """
            <book>
              <title>Hello World</title>
              <isbn>1234567890123</isbn>
            </book>
            """.trimIndent()

        val BOOK_REQUEST_TEXT = """
              title = Hello World
              isbn = 1234567890123
            """.trimIndent()

        val BOOK_RESPONSE_JSON = """
            {
              "bookId": "46d287e5-5d6b-42bf-83be-f7085ea132ce",
              "title": "Hello World",
              "isbn": "1234567890123"
            }
            """.trimIndent()
        val BOOKS_RESPONSE_JSON = """
            [{
              "bookId": "46d287e5-5d6b-42bf-83be-f7085ea132ce",
              "title": "Hello World",
              "isbn": "1234567890123"
            }]
            """.trimIndent()

    }

    val target = Target(port = wireMock.port())
    val cut = ApacheHttpClient()

    val correlationId = UUID.randomUUID().toString()
    val correlationIdHeaderValue = listOf(correlationId)

    val jsonContentType = "application/json"
    val jsonContentTypeHeaderValue = listOf(jsonContentType)

    @ValueSource(strings = ["get", "GET"])
    @ParameterizedTest fun `simple GET request is handled correctly`(httpMethod: String) {
        val pactRequest = pactRequest {
            method = "GET"
            path = "/books/46d287e5-5d6b-42bf-83be-f7085ea132ce"
            headers = mapOf(CORELATION_ID to correlationIdHeaderValue)
            body = OptionalBody.missing()
        }

        wireMock.givenThat(get(urlEqualTo("/books/46d287e5-5d6b-42bf-83be-f7085ea132ce"))
                .withHeader(CORELATION_ID, equalTo(correlationId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, jsonContentType)
                        .withHeader(CORELATION_ID, correlationId)
                        .withBody(BOOK_RESPONSE_JSON)))

        with(cut.send(pactRequest, target)) {
            assertThat(status).isEqualTo(200)
            assertThat(headers)
                    .containsEntry(CORELATION_ID, correlationIdHeaderValue)
                    .containsEntry(CONTENT_TYPE, jsonContentTypeHeaderValue)
            assertThat(body).isEqualTo(BOOK_RESPONSE_JSON)
        }
    }

    @ValueSource(strings = ["post", "POST"])
    @ParameterizedTest fun `simple POST request is handled correctly`(httpMethod: String) {
        val pactRequest = pactRequest {
            method = httpMethod
            path = "/books"
            headers = mapOf(
                    CORELATION_ID to correlationIdHeaderValue,
                    CONTENT_TYPE to jsonContentTypeHeaderValue
            )
            body = OptionalBody.body(BOOK_REQUEST_JSON.toByteArray())
        }

        wireMock.givenThat(post(urlEqualTo("/books"))
                .withHeader(CORELATION_ID, equalTo(correlationId))
                .withHeader(CONTENT_TYPE, equalTo(jsonContentType))
                .withRequestBody(equalTo(BOOK_REQUEST_JSON))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader(CONTENT_TYPE, jsonContentType)
                        .withHeader(CORELATION_ID, correlationId)
                        .withBody(BOOK_RESPONSE_JSON)))

        with(cut.send(pactRequest, target)) {
            assertThat(status).isEqualTo(201)
            assertThat(headers)
                    .containsEntry(CORELATION_ID, correlationIdHeaderValue)
                    .containsEntry(CONTENT_TYPE, jsonContentTypeHeaderValue)
            assertThat(body).isEqualTo(BOOK_RESPONSE_JSON)
        }
    }

    @ValueSource(strings = ["put", "PUT"])
    @ParameterizedTest fun `simple PUT request is handled correctly`(httpMethod: String) {
        val pactRequest = pactRequest {
            method = httpMethod
            path = "/books/46d287e5-5d6b-42bf-83be-f7085ea132ce"
            headers = mapOf(
                    CORELATION_ID to correlationIdHeaderValue,
                    CONTENT_TYPE to jsonContentTypeHeaderValue
            )
            body = OptionalBody.body(BOOK_REQUEST_JSON.toByteArray())
        }

        wireMock.givenThat(put(urlEqualTo("/books/46d287e5-5d6b-42bf-83be-f7085ea132ce"))
                .withHeader(CORELATION_ID, equalTo(correlationId))
                .withHeader(CONTENT_TYPE, equalTo(jsonContentType))
                .withRequestBody(equalTo(BOOK_REQUEST_JSON))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, jsonContentType)
                        .withHeader(CORELATION_ID, correlationId)
                        .withBody(BOOK_RESPONSE_JSON)))

        with(cut.send(pactRequest, target)) {
            assertThat(status).isEqualTo(200)
            assertThat(headers)
                    .containsEntry(CORELATION_ID, correlationIdHeaderValue)
                    .containsEntry(CONTENT_TYPE, jsonContentTypeHeaderValue)
            assertThat(body).isEqualTo(BOOK_RESPONSE_JSON)
        }
    }

    @ValueSource(strings = ["delete", "DELETE"])
    @ParameterizedTest fun `simple DELETE request is handled correctly`(httpMethod: String) {
        val pactRequest = pactRequest {
            method = httpMethod
            path = "/books/46d287e5-5d6b-42bf-83be-f7085ea132ce"
            headers = mapOf(
                    CORELATION_ID to correlationIdHeaderValue
            )
            body = OptionalBody.missing()
        }

        wireMock.givenThat(delete(urlEqualTo("/books/46d287e5-5d6b-42bf-83be-f7085ea132ce"))
                .withHeader(CORELATION_ID, equalTo(correlationId))
                .willReturn(aResponse()
                        .withStatus(204)
                        .withHeader(CORELATION_ID, correlationId)))

        with(cut.send(pactRequest, target)) {
            assertThat(status).isEqualTo(204)
            assertThat(headers).containsEntry(CORELATION_ID, correlationIdHeaderValue)
            assertThat(body).isNull()
        }
    }

    @Nested inner class `request content type is determined if not set as header` {

        @ValueSource(strings = ["POST", "PUT", "PATCH"])
        @ParameterizedTest fun `application json`(httpMethod: String) {
            val pactRequest = pactRequest {
                method = httpMethod
                path = "/books/46d287e5-5d6b-42bf-83be-f7085ea132ce"
                headers = mapOf(CORELATION_ID to correlationIdHeaderValue)
                body = OptionalBody.body(BOOK_REQUEST_JSON.toByteArray())
            }

            wireMock.givenThat(any(urlEqualTo("/books/46d287e5-5d6b-42bf-83be-f7085ea132ce"))
                    .withHeader(CORELATION_ID, equalTo(correlationId))
                    .withHeader(CONTENT_TYPE, equalTo(jsonContentType))
                    .withRequestBody(equalTo(BOOK_REQUEST_JSON))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader(CONTENT_TYPE, jsonContentType)
                            .withHeader(CORELATION_ID, correlationId)
                            .withBody(BOOK_RESPONSE_JSON)))

            with(cut.send(pactRequest, target)) {
                assertThat(status).isEqualTo(201)
                assertThat(headers)
                        .containsEntry(CORELATION_ID, correlationIdHeaderValue)
                        .containsEntry(CONTENT_TYPE, jsonContentTypeHeaderValue)
                assertThat(body).isEqualTo(BOOK_RESPONSE_JSON)
            }
        }

        @ValueSource(strings = ["POST", "PUT", "PATCH"])
        @ParameterizedTest fun `application xml`(httpMethod: String) {
            val pactRequest = pactRequest {
                method = httpMethod
                path = "/books/46d287e5-5d6b-42bf-83be-f7085ea132ce"
                headers = mapOf(CORELATION_ID to correlationIdHeaderValue)
                body = OptionalBody.body(BOOK_REQUEST_XML.toByteArray())
            }

            wireMock.givenThat(any(urlEqualTo("/books/46d287e5-5d6b-42bf-83be-f7085ea132ce"))
                    .withHeader(CORELATION_ID, equalTo(correlationId))
                    .withHeader(CONTENT_TYPE, equalTo("application/xml"))
                    .withRequestBody(equalTo(BOOK_REQUEST_XML))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader(CONTENT_TYPE, jsonContentType)
                            .withHeader(CORELATION_ID, correlationId)
                            .withBody(BOOK_RESPONSE_JSON)))

            with(cut.send(pactRequest, target)) {
                assertThat(status).isEqualTo(201)
                assertThat(headers)
                        .containsEntry(CORELATION_ID, correlationIdHeaderValue)
                        .containsEntry(CONTENT_TYPE, jsonContentTypeHeaderValue)
                assertThat(body).isEqualTo(BOOK_RESPONSE_JSON)
            }
        }

        @ValueSource(strings = ["POST", "PUT", "PATCH"])
        @ParameterizedTest fun `defaults to text plain`(httpMethod: String) {
            val pactRequest = pactRequest {
                method = httpMethod
                path = "/books/46d287e5-5d6b-42bf-83be-f7085ea132ce"
                headers = mapOf(CORELATION_ID to correlationIdHeaderValue)
                body = OptionalBody.body(BOOK_REQUEST_TEXT.toByteArray())
            }

            wireMock.givenThat(any(urlEqualTo("/books/46d287e5-5d6b-42bf-83be-f7085ea132ce"))
                    .withHeader(CORELATION_ID, equalTo(correlationId))
                    .withHeader(CONTENT_TYPE, equalTo("text/plain"))
                    .withRequestBody(equalTo(BOOK_REQUEST_TEXT))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader(CONTENT_TYPE, jsonContentType)
                            .withHeader(CORELATION_ID, correlationId)
                            .withBody(BOOK_RESPONSE_JSON)))

            with(cut.send(pactRequest, target)) {
                assertThat(status).isEqualTo(201)
                assertThat(headers)
                        .containsEntry(CORELATION_ID, correlationIdHeaderValue)
                        .containsEntry(CONTENT_TYPE, jsonContentTypeHeaderValue)
                assertThat(body).isEqualTo(BOOK_RESPONSE_JSON)
            }
        }

    }

    @Test fun `query parameters are appended to URL`() {
        val pactRequest = pactRequest {
            method = "GET"
            path = "/books"
            query = mapOf(
                    "foo" to listOf("123", "456"),
                    "bar" to listOf("true")
            )
        }

        wireMock.givenThat(get(urlEqualTo("/books?foo=123&foo=456&bar=true"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, jsonContentType)
                        .withBody(BOOKS_RESPONSE_JSON)))

        with(cut.send(pactRequest, target)) {
            assertThat(status).isEqualTo(200)
            assertThat(body).isEqualTo(BOOKS_RESPONSE_JSON)
        }
    }

    fun pactRequest(body: Request.() -> Unit) = Request().apply(body)

}