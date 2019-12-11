package org.testit.pact.provider.http.clients

import au.com.dius.pact.model.Request
import org.apache.http.ProtocolVersion
import org.apache.http.StatusLine
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicHttpResponse
import org.apache.http.message.BasicStatusLine
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ApacheResponseExtractorTest {

    val http11 = ProtocolVersion("HTTP", 1, 1)

    @ValueSource(ints = [200, 201, 400, 401, 403, 404, 500, 503, 504])
    @ParameterizedTest fun `status code is extracted correctly`(statusCode: Int) {
        val response = BasicHttpResponse(statusLine(statusCode))
        val result = ApacheHttpClient.ResponseExtractor.extract(response)
        Assertions.assertThat(result.status).isEqualTo(statusCode)
    }

    @Test fun `headers are extracted correctly`() {
        val response = BasicHttpResponse(statusLine(200)).apply {
            setHeader("Content-Type", "application/json")
            setHeader("X-Correlation-Id", "foo-bar-123")
        }
        val result = ApacheHttpClient.ResponseExtractor.extract(response)
        Assertions.assertThat(result.headers).isEqualTo(mapOf(
                "Content-Type" to listOf("application/json"),
                "X-Correlation-Id" to listOf("foo-bar-123")
        ))
    }

    @Test fun `body is extracted correctly`() {
        val response = BasicHttpResponse(statusLine(200)).apply {
            entity = StringEntity("""{ "msg": "hello world" }""")
        }
        val result = ApacheHttpClient.ResponseExtractor.extract(response)
        Assertions.assertThat(result.body).isEqualTo("""{ "msg": "hello world" }""")
    }

    fun statusLine(statusCode: Int, reasonPhrase: String = ""): StatusLine {
        return BasicStatusLine(http11, statusCode, reasonPhrase)
    }

}