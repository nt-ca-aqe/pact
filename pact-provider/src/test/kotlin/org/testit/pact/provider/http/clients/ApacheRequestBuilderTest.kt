package org.testit.pact.provider.http.clients

import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.Request
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.client.methods.*
import org.apache.http.util.EntityUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.URI

class ApacheRequestBuilderTest {

    val defaultUri = URI.create("http://localhost:8080")

    @Test fun `default request baseline`() {
        val httpRequest = ApacheHttpClient.RequestBuilder.build(defaultUri, Request())

        Assertions.assertThat(httpRequest.method).isEqualTo("GET")
        Assertions.assertThat(httpRequest.uri).isEqualTo(URI.create("http://localhost:8080"))
        Assertions.assertThat(httpRequest.allHeaders).isEmpty()
        Assertions.assertThat(httpRequest).isNotInstanceOf(HttpEntityEnclosingRequest::class.java)
    }

    @Nested inner class `HTTP method handling` {

        @ValueSource(strings = [
            "get", "post", "put", "delete", "options", "head", "trace", "patch",
            "GeT", "pOsT", "PuT", "DeLeTE", "OPtiONs", "HeaD", "TrACe", "pATCH",
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "TRACE", "PATCH"
        ])
        @ParameterizedTest fun `http method case is ignored`(httpMethod: String) {
            val pactRequest = pactRequest { method = httpMethod }
            val httpRequest = ApacheHttpClient.RequestBuilder.build(defaultUri, pactRequest)
            Assertions.assertThat(httpRequest.method).isEqualTo(httpMethod.toUpperCase())
        }

        @Test fun `unknown HTTP method throws exception`() {
            val pactRequest = pactRequest { method = "unknown" }
            val exception = assertThrows<IllegalStateException> {
                ApacheHttpClient.RequestBuilder.build(defaultUri, pactRequest)
            }
            Assertions.assertThat(exception).hasMessage("unknown HTTP method: unknown")
        }

        @TestFactory fun `correct request type is used for each HTTP method`() = mapOf(
                "GET" to HttpGet::class.java,
                "POST" to HttpPost::class.java,
                "PUT" to HttpPut::class.java,
                "DELETE" to HttpDelete::class.java,
                "OPTIONS" to HttpOptions::class.java,
                "HEAD" to HttpHead::class.java,
                "TRACE" to HttpTrace::class.java,
                "PATCH" to HttpPatch::class.java
        ).map { (httpMethod, type) ->
            dynamicTest("$httpMethod uses ${type.simpleName}") {
                val pactRequest = pactRequest { method = httpMethod }
                val httpRequest = ApacheHttpClient.RequestBuilder.build(defaultUri, pactRequest)
                Assertions.assertThat(httpRequest).isExactlyInstanceOf(type)
            }
        }

    }

    @Nested inner class `header handling` {

        @Test fun `headers are correctly set`() {
            val pactRequest = pactRequest {
                headers = mapOf(
                    "Content-Type" to listOf("application/json"),
                    "X-Correlation-Id" to listOf("foo-bar-123")
                )
            }

            with(ApacheHttpClient.RequestBuilder.build(defaultUri, pactRequest)) {
                Assertions.assertThat(getHeaders("Content-Type").map { it.value }).containsOnly("application/json")
                Assertions.assertThat(getHeaders("X-Correlation-Id").map { it.value }).containsOnly("foo-bar-123")
                Assertions.assertThat(allHeaders).hasSize(2)
            }
        }

        @Test fun `null headers map defaults to empty`() {
            val pactRequest = pactRequest { headers = null }
            val httpRequest = ApacheHttpClient.RequestBuilder.build(defaultUri, pactRequest)
            Assertions.assertThat(httpRequest.allHeaders).isEmpty()
        }

    }

    @Nested inner class `body handling` {

        @Test fun `content type of header is used if available even though it might be wrong`() {
            val pactRequest = pactRequest {
                method = "POST"
                headers = mapOf("Content-Type" to listOf("application/json"))
                body = OptionalBody.body("<foo><bar>Hello World</bar></foo>".toByteArray())
            }

            with(ApacheHttpClient.RequestBuilder.build(defaultUri, pactRequest) as HttpPost) {
                Assertions.assertThat(entity.contentType.value).isEqualTo("application/json")
                Assertions.assertThat(EntityUtils.toString(entity)).isEqualTo("<foo><bar>Hello World</bar></foo>")
                Assertions.assertThat(getHeaders("Content-Type").map { it.value }).containsOnly("application/json")
            }
        }

        @Test fun `json content is recognized even without content type header`() {
            val pactRequest = pactRequest {
                method = "POST"
                headers = mapOf()
                body = OptionalBody.body("""{ "foo": "bar" }""".toByteArray())
            }

            with(ApacheHttpClient.RequestBuilder.build(defaultUri, pactRequest) as HttpPost) {
                Assertions.assertThat(entity.contentType.value).isEqualTo("application/json")
                Assertions.assertThat(EntityUtils.toString(entity)).isEqualTo("""{ "foo": "bar" }""")
                Assertions.assertThat(getHeaders("Content-Type").map { it.value }).containsOnly("application/json")
            }
        }

        @Test fun `xml content is recognized even without content type header`() {
            val pactRequest = pactRequest {
                method = "POST"
                headers = mapOf()
                body = OptionalBody.body("<foo><bar>Hello World</bar></foo>".toByteArray())
            }

            with(ApacheHttpClient.RequestBuilder.build(defaultUri, pactRequest) as HttpPost) {
                Assertions.assertThat(entity.contentType.value).isEqualTo("application/xml")
                Assertions.assertThat(EntityUtils.toString(entity)).isEqualTo("<foo><bar>Hello World</bar></foo>")
                Assertions.assertThat(getHeaders("Content-Type").map { it.value }).containsOnly("application/xml")
            }
        }

        @Test fun `content type defaults to text plain if no header was set`() {
            val pactRequest = pactRequest {
                method = "POST"
                headers = mapOf()
                body = OptionalBody.body("foo = bar".toByteArray())
            }

            with(ApacheHttpClient.RequestBuilder.build(defaultUri, pactRequest) as HttpPost) {
                Assertions.assertThat(entity.contentType.value).isEqualTo("text/plain")
                Assertions.assertThat(EntityUtils.toString(entity)).isEqualTo("foo = bar")
                Assertions.assertThat(getHeaders("Content-Type").map { it.value }).containsOnly("text/plain")
            }
        }

        @Test fun `throws exception for requests with body definitions who's methods don't allow a body`() {
            val pactRequest = pactRequest {
                method = "HEAD"
                headers = mapOf()
                body = OptionalBody.body("foo = bar".toByteArray())
            }
            val exception = assertThrows<IllegalStateException> {
                ApacheHttpClient.RequestBuilder.build(defaultUri, pactRequest)
            }
            Assertions.assertThat(exception).hasMessage("request definition contains body, but [HEAD] requests can't have bodies!")
        }

    }

    fun pactRequest(body: Request.() -> Unit) = Request().apply(body)

}