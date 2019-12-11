package org.testit.pact.provider.http

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.RequestResponseInteraction
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


internal class ProviderStateHandlerTest {

    @Test fun `if there are no provider states defined for an interaction a callback handler is not needed`() {
        val cut = ProviderStateHandler(null)
        val interaction = interaction("no states", emptyList())
        cut.prepare(interaction)
    }

    @Test fun `if there are provider states defined for an interaction a callback handler is needed`() {
        val cut = ProviderStateHandler(null)
        val interaction = interaction("one state", listOf(
                providerState("some state description")
        ))
        assertThrows<ProviderStateHandlerNotSetException> {
            cut.prepare(interaction)
        }
    }

    @Test fun `provider state methods are called in order`() {
        val delegate = mock<CallbackDelegate>()
        val cut = ProviderStateHandler(CallbackHandler(delegate))
        val interaction = interaction("various states", listOf(
                providerState("some state description"),
                providerState("some other state description", LinkedHashMap<String, String>().apply { put("foo", "bar") })
        ))

        cut.prepare(interaction)

        with(inOrder(delegate)) {
            verify(delegate).someState()
            verify(delegate).someOtherState(mapOf("foo" to "bar"))
            verifyNoMoreInteractions()
        }
    }

    @Test fun `provider state methods dont have to match case`() {
        val delegate = mock<CallbackDelegate>()
        val cut = ProviderStateHandler(CallbackHandler(delegate))
        val interaction = interaction("various states", listOf(
                providerState("SoMe StAtE DeScRiPtIoN")
        ))
        cut.prepare(interaction)
        verify(delegate).someState()
    }

    @Test fun `unresolved provider state methods throw exception`() {
        val delegate = mock<CallbackDelegate>()
        val cut = ProviderStateHandler(CallbackHandler(delegate))
        val interaction = interaction("unknown state", listOf(
                providerState("unknown state description")
        ))
        assertThrows<ProviderStateMethodNotFoundException> {
            cut.prepare(interaction)
        }
    }

    @Test fun `provider state method exceptions are throw wrapped in custom exception type`() {
        val delegate = mock<CallbackDelegate> {
            on { someState() } doThrow RuntimeException()
        }
        val cut = ProviderStateHandler(CallbackHandler(delegate))
        val interaction = interaction("one state with exception", listOf(
                providerState("some state description")
        ))
        assertThrows<ProviderStateInvocationException> {
            cut.prepare(interaction)
        }
    }

    @Test fun `malformed state methods throw exception - wrong argument type`() {
        val cut = ProviderStateHandler(MalformedCallbackHandler())
        val interaction = interaction("malformed state", listOf(
                providerState("non map method argument")
        ))
        assertThrows<MalformedProviderStateMethodException> {
            cut.prepare(interaction)
        }
    }

    @Test fun `malformed state methods throw exception - number of arguments`() {
        val cut = ProviderStateHandler(MalformedCallbackHandler())
        val interaction = interaction("malformed state", listOf(
                providerState("more than one method argument")
        ))
        assertThrows<MalformedProviderStateMethodException> {
            cut.prepare(interaction)
        }
    }

    class CallbackHandler(
            val delegate: CallbackDelegate
    ) {

        @ProviderState("some state description")
        fun someState() = delegate.someState()

        @ProviderState("some other state description")
        fun someOtherState(parameters: Map<String, Any>) = delegate.someOtherState(parameters)

    }

    class MalformedCallbackHandler {

        @ProviderState("non map method argument")
        fun someState(arg0: String) {
        }

        @ProviderState("more than one method argument")
        fun someOtherState(arg0: Map<String, Any>, arg1: String) {
        }

    }

    interface CallbackDelegate {
        fun someState()
        fun someOtherState(parameters: Map<String, Any>)
    }

    fun providerState(name: String, parameters: Map<String, Any> = mapOf()) =
            au.com.dius.pact.model.ProviderState(name, parameters)

    fun interaction(description: String, providerStates: List<au.com.dius.pact.model.ProviderState>): Interaction {
        return RequestResponseInteraction(description, providerStates)
    }

}