package org.testit.pact.provider.message

import au.com.dius.pact.model.v3.messaging.Message
import au.com.dius.pact.model.v3.messaging.MessagePact
import mu.KotlinLogging.logger
import org.testit.pact.provider.ExecutablePact
import org.testit.pact.provider.sources.PactSource

class MessagePacts(
        private val pactSource: PactSource,
        private val provider: String
) {

    private val log = logger {}
    private val matcher = MessageMatcher()

    fun createExecutablePacts(consumerFilter: String? = null, callbackHandler: Any): List<ExecutablePact> {
        val messageProducerHandler = MessageProducerHandler(callbackHandler)
        return loadMessagePacts(provider, consumerFilter)
                .flatMap { pact ->
                    pact.messages.map { message ->
                        ExecutablePact(
                                name = "${pact.consumer.name}: ${message.description}",
                                executable = { executeMessageTest(message, messageProducerHandler) }
                        )
                    }
                }
    }

    private fun loadMessagePacts(providerFilter: String, consumerFilter: String?): List<MessagePact> {
        val pacts  = pactSource.loadPacts(providerFilter, consumerFilter).filterIsInstance<MessagePact>()
        log.debug { "loaded ${pacts.size} message pacts from [$pactSource] for providerFilter=$providerFilter and consumerFilter=$consumerFilter" }
        if (pacts.isEmpty()) {
            throw NoMessagePactsFoundException(pactSource, provider, consumerFilter)
        }
        return pacts
    }

    private fun executeMessageTest(expectedMessage: Message, messageProducerHandler: MessageProducerHandler) {
        val actualMessage = messageProducerHandler.produce(expectedMessage)

        val result = matcher.match(expectedMessage, actualMessage)
        if (result.hasErrors) {
            throw MessageMismatchException(result)
        }

        log.info { "Message interaction [${expectedMessage.description}] matched expectations." }
    }

}

class NoMessagePactsFoundException(pactSource: PactSource, provider: String, consumerFilter: String?)
    : RuntimeException("Did not find any message pacts in source [$pactSource] for the provider [$provider] and a consumer filter of [$consumerFilter]")

class MessageMismatchException(val result: MessageMatcher.Result)
    : AssertionError("Message expectation(s) were not met:\n\n$result")
