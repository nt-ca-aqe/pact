package org.testit.pact.provider.http

import au.com.dius.pact.matchers.BodyMismatch
import au.com.dius.pact.matchers.HeaderMismatch
import au.com.dius.pact.matchers.Mismatch
import au.com.dius.pact.model.*
import scala.collection.JavaConverters.seqAsJavaListConverter

class ResponseMatcher {

    fun match(expectedResponse: Response, actualResponse: MatchableResponse): Result {
        val actualStatus = actualResponse.status
        val actualHeaders = actualResponse.headers
        val actualBody = actualResponse.body

        val mismatches = executeComparison(expectedResponse, actualStatus, actualHeaders, actualBody)
        return Result(
                statusError = findStatusError(mismatches),
                headerErrors = findHeaderErrors(mismatches),
                bodyErrors = findBodyErrors(mismatches)
        )
    }

    private fun findStatusError(mismatches: List<Mismatch>): String? = statusMismatch(mismatches)
            ?.let { "[status] was expected to be [${it.expected()}] but was actually [${it.actual()}]" }

    private fun findHeaderErrors(mismatches: List<Mismatch>): List<String> = headerMismatches(mismatches)
            .map { "[${it.headerKey}] was expected to be [${it.expected}] but was actually [${it.actual}]" }

    private fun findBodyErrors(mismatches: List<Mismatch>): List<String> {
        val bodyTypeError = bodyTypeMismatch(mismatches)
                ?.let { "[Content-Type] expected it to be [${it.expected()}] but was [${it.actual()}] - no further comparison executed" }
        if (bodyTypeError != null) {
            return listOf(bodyTypeError)
        }
        return bodyMismatches(mismatches)
                .map { "[${it.path}] was expected to be [${it.expected}] but was actually [${it.actual}]" }
    }

    private fun statusMismatch(mismatches: List<Mismatch>) = mismatches.filterIsInstance<StatusMismatch>().singleOrNull()

    private fun headerMismatches(mismatches: List<Mismatch>) = mismatches.filterIsInstance<HeaderMismatch>().sortedBy { it.headerKey }

    private fun bodyTypeMismatch(mismatches: List<Mismatch>) = mismatches.filterIsInstance<BodyTypeMismatch>().singleOrNull()

    private fun bodyMismatches(mismatches: List<Mismatch>) = mismatches.filterIsInstance<BodyMismatch>()
            .groupBy { it.path }
            .map { it.value.first() }
            .sortedBy { it.path }

    data class Result(
            val statusError: String?,
            val headerErrors: List<String>,
            val bodyErrors: List<String>
    ) {

        val hasErrors: Boolean
            get() = statusError != null || headerErrors.isNotEmpty() || bodyErrors.isNotEmpty()

        override fun toString(): String {
            val sections = mutableListOf<String>()
            if (statusError != null) {
                sections.add("-- Status --\n$statusError")
            }
            if (headerErrors.isNotEmpty()) {
                sections.add("-- Headers --\n" + headerErrors.joinToString("\n"))
            }
            if (bodyErrors.isNotEmpty()) {
                sections.add("-- Body --\n" + bodyErrors.joinToString("\n"))
            }
            return sections.joinToString("\n\n")
        }

    }

    private fun executeComparison(response: Response, actualStatus: Int, actualHeaders: Map<String, List<String>>, actualBody: String?):
        List<Mismatch> {
        val matching = `ResponseMatching$`.`MODULE$`
        return seqAsJavaListConverter(
                matching.responseMismatches(
                    response,
                    Response(
                        actualStatus,
                        actualHeaders,
                        OptionalBody.body(actualBody?.toByteArray())
                    )
                )
        ).asJava()
    }

}
