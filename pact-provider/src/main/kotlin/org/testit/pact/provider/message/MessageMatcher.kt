package org.testit.pact.provider.message

import au.com.dius.pact.matchers.BodyMismatch
import au.com.dius.pact.matchers.MatchingConfig
import au.com.dius.pact.matchers.Mismatch
import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.Response
import au.com.dius.pact.model.v3.messaging.Message

class MessageMatcher {

    fun match(expectedMessage: Message, actualMessage: ActualMessage): Result {
        val actualBody = String(actualMessage.body)
        val bodyMismatches = executeComparison(expectedMessage, actualBody)
        return Result(
                bodyErrors = findBodyErrors(bodyMismatches)
        )
    }

    private fun findBodyErrors(mismatches: List<Mismatch>): List<String> = bodyMismatches(mismatches)
            .map { "[${it.path}] was expected to be [${it.expected}] but was actually [${it.actual}]" }

    private fun bodyMismatches(mismatches: List<Mismatch>) = mismatches.filterIsInstance<BodyMismatch>()
            .groupBy { it.path }
            .map { it.value.first() }
            .sortedBy { it.path }

    data class Result(
            val bodyErrors: List<String>
    ) {

        val hasErrors: Boolean
            get() = bodyErrors.isNotEmpty()

        override fun toString(): String {
            val sections = mutableListOf<String>()
            if (bodyErrors.isNotEmpty()) {
                sections.add("-- Body --\n" + bodyErrors.joinToString("\n"))
            }
            return sections.joinToString("\n\n")
        }

    }

    private fun executeComparison(message: Message, actualBody: String): List<Mismatch> {
        val contentType = message.contentType.substringBefore(";")
        val matcher = MatchingConfig.lookupBodyMatcher(contentType)
                ?: error("Content-Type [$contentType] is currently not supported!")
        val expected = message.asPactRequest()
        val actual = Response(200, emptyMap(), OptionalBody.body(actualBody.toByteArray()))
        return matcher.matchBody(expected, actual, true)
    }

}
