[![License](https://img.shields.io/badge/License-Apache%20License%202.0-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Build Status](https://circleci.com/gh/nt-ca-aqe/pact/tree/master.svg?style=svg)][1]

# testIT | Pact Libraries

This repository hosts libraries related to working with Pact (https://docs.pact.io).

## Pact Provider JUnit 5

The `pact-provider-junit5` module contains a basic provider side test factory
implementations for running `RequestResponsePact` and `MessagePact` pacts
using JUnit 5's `@TestFactory` mechanism.

**Request / Response Pact provider side tests:**

```kotlin
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [Application::class], webEnvironment = RANDOM_PORT)
class HttpContractTest {

    @MockBean lateinit var dataStore: BookDataStore
    @MockBean lateinit var eventDispatcher: EventDispatcher<BookEvent>

    val pacts = RequestResponsePacts(LocalFiles("src/test/pacts/http"), "library-service")

    @LocalServerPort
    fun init(port: Int) {
        pacts.target.bindPort { port }
    }

    @TestFactory fun `library enrichment contract tests`() =
            PactTestFactory.createTests(pacts, "library-enrichment", this)

    @ProviderState("A book with the ID {bookId} exists")
    fun `book with fixed ID exists`(params: Map<String, String>) {
        val bookId = BookId.from(params["bookId"]!!)
        val bookRecord = BookRecord(bookId, Books.THE_MARTIAN)
        given { dataStore.findById(bookId) }.willReturn { bookRecord }
        given { dataStore.createOrUpdate(any()) }.willAnswer { it.arguments[0] as BookRecord }
    }

}
```

**Message Pact provider side tests:**

```kotlin
class MessageContractTest {

    val configuration = MessagingConfiguration()
    val objectMapper = ObjectMapper().apply { findAndRegisterModules() }
    val messageConverter = configuration.messageConverter(objectMapper)

    val pacts = MessagePacts(LocalFiles("src/test/pacts/message"), "library-service")

    @TestFactory fun `library-enrichment consumer contract tests`() =
            PactTestFactory.createTests(pacts, "library-enrichment", this)

    @MessageProducer("'The Martian' was added event")
    fun `verify The Martian was added event`(): ActualMessage {
        val event = BookAdded(
                id = UUID.randomUUID(),
                bookId = BookId.generate(),
                isbn = Books.THE_MARTIAN.isbn,
                timestamp = OffsetDateTime.now()
        )
        val message = messageConverter.toMessage(event, MessageProperties())
        return ActualMessage(message.body)
    }

}
```

## Licensing
TestUtils is licensed under [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

## Sponsoring
The contents of this repository is mainly developed by
[NovaTec Consulting GmbH](http://www.novatec-gmbh.de/),
a German consultancy firm that drives quality in software development projects.

[1]:https://circleci.com/gh/nt-ca-aqe/pact/tree/master
