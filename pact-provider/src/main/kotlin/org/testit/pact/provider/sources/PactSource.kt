package org.testit.pact.provider.sources

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact

interface PactSource {
    fun  loadPacts(providerFilter: String, consumerFilter: String?): List<Pact<out Interaction>>
}
