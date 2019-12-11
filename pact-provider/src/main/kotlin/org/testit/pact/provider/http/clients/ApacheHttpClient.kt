package org.testit.pact.provider.http.clients

import au.com.dius.pact.model.Request
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.HttpResponse
import org.apache.http.client.methods.*
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.testit.pact.provider.http.MatchableResponse
import org.testit.pact.provider.http.Target
import java.net.URI

class ApacheHttpClient(
        private val client: CloseableHttpClient = HttpClients.createDefault()
) : HttpClient {

    override fun send(pactRequest: Request, target: Target): MatchableResponse {
        val targetUri = UriBuilder.build(target, pactRequest)
        val httpRequest = RequestBuilder.build(targetUri, pactRequest)
        return client.execute(httpRequest).use(ResponseExtractor::extract)
    }

    internal object RequestBuilder {

        fun build(uri: URI, request: Request): HttpUriRequest {
            return buildRequestBase(uri, request).apply {
                addHeaders(request)
                addBody(request)
            }
        }

        private fun buildRequestBase(uri: URI, request: Request) = with(request.method) {
            when {
                equals("get", true) -> HttpGet(uri)
                equals("post", true) -> HttpPost(uri)
                equals("put", true) -> HttpPut(uri)
                equals("delete", true) -> HttpDelete(uri)
                equals("options", true) -> HttpOptions(uri)
                equals("head", true) -> HttpHead(uri)
                equals("trace", true) -> HttpTrace(uri)
                equals("patch", true) -> HttpPatch(uri)
                else -> error("unknown HTTP method: $this")
            }
        }

        private fun HttpRequestBase.addHeaders(request: Request) {
            request.headers?.forEach { (name, value) ->
                this.addHeader(name, value.joinToString(separator = ","))
            }
        }

        private fun HttpRequestBase.addBody(request: Request) {
            val hasBody = request.body?.isPresent() == true
            val canHaveBody = this is HttpEntityEnclosingRequest

            if (hasBody && canHaveBody) {
                this as HttpEntityEnclosingRequest

                val contentType: ContentType = determineContentType(request)
                val body = request.body!!.value!!
                this.entity = StringEntity(String(body), contentType)
                this.setHeader(entity.contentType)
            }

            if (hasBody && !canHaveBody) {
                error("request definition contains body, but [${request.method}] requests can't have bodies!")
            }
        }

        private fun determineContentType(request: Request): ContentType {
            // NOTE: 'request.contentTypeHeader()' determines content type if header not set and defaults to text/plain
            return request.contentTypeHeader().let { ContentType.parse(it) }
        }

    }

    internal object ResponseExtractor {

        fun extract(response: HttpResponse): MatchableResponse {
            val status: Int = response.statusLine.statusCode
            val headers: Map<String, List<String>> = response.allHeaders.associate { header -> header.name to listOf(header.value) }
            val body: String? = response.entity?.let {
                val charset = it.contentType
                        ?.let { ContentType.parse(it.value)?.charset }
                        ?: Charsets.ISO_8859_1
                EntityUtils.toString(it, charset)
            }
            return MatchableResponse(status, headers, body)
        }

    }

}