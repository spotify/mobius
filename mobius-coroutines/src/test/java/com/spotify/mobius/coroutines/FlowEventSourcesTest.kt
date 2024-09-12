/*
 * -\-\-
 * Mobius
 * --
 * Copyright (c) 2017-2020 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */
package com.spotify.mobius.coroutines

import com.google.common.truth.Truth.assertThat
import com.spotify.mobius.EventSource
import com.spotify.mobius.coroutines.FlowEventSources.Companion.asFlow
import com.spotify.mobius.disposables.Disposable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@ExperimentalCoroutinesApi
class FlowEventSourcesTest {

    @Test
    @Requirement(
        given = "Event source from flows",
        `when` = "a flow produces an event",
        then = "the event is notified"
    )
    fun eventIsNotified() = runTest {
        val eventsReceived = mutableListOf<Event>()
        val eventSource = FlowEventSources.fromFlows(
            coroutineContext,
            flowOf(Event("event1"))
        )

        eventSource.subscribe { eventsReceived.add(it) }
        advanceUntilIdle()

        assertThat(eventsReceived).containsExactly(Event("event1"))
    }

    @Test
    @Requirement(
        given = "Event source from flows",
        `when` = "a flow produces an event after subscribe",
        then = "the event is notified"
    )
    fun eventIsNotifiedAfterSubscribe() = runTest {
        val eventsReceived = mutableListOf<Event>()
        val eventSource = FlowEventSources.fromFlows<Event>(
            coroutineContext,
            flow {
                delay(2000)
                emit(Event("delayed"))
            }
        )

        eventSource.subscribe { eventsReceived.add(it) }
        advanceTimeBy(1500)
        assertThat(eventsReceived).isEmpty()

        advanceUntilIdle()
        assertThat(eventsReceived).containsExactly(Event("delayed"))
    }

    @Test
    @Requirement(
        given = "Event source from 2 flows",
        `when` = "any flow produces an event",
        then = "the event is notified immediately"
    )
    fun multipleFlows() = runTest {
        val eventsReceived = mutableListOf<Event>()
        val eventSource = FlowEventSources.fromFlows<Event>(
            coroutineContext,
            flow {
                delay(500)
                emit(Event("delayed 500"))
            },
            flow {
                delay(2000)
                emit(Event("delayed 2000"))
            }
        )

        eventSource.subscribe { eventsReceived.add(it) }

        advanceTimeBy(501)
        assertThat(eventsReceived).containsExactly(Event("delayed 500"))
        advanceUntilIdle()
        assertThat(eventsReceived).containsExactly(Event("delayed 500"), Event("delayed 2000"))
    }

    @Test
    @Requirement(
        given = "Event source from flows",
        `when` = "a flow produces an event in a different context",
        then = "the event is notified"
    )
    fun flowInDifferentContexts() = runTest {
        val eventsReceived = mutableListOf<Event>()
        val flowDispatcher = StandardTestDispatcher(testScheduler)
        val eventSource = FlowEventSources.fromFlows(
            coroutineContext,
            flowOf(Event("event1"))
                .flowOn(flowDispatcher),
        )

        eventSource.subscribe { eventsReceived.add(it) }
        advanceUntilIdle()

        assertThat(eventsReceived).containsExactly(Event("event1"))
    }

    @Test
    @Requirement(
        given = "Event source from flows",
        `when` = "event source is disposed",
        then = "no more events are processed"
    )
    fun eventSourceDisposed() = runTest {
        val eventsReceived = mutableListOf<Event>()
        val eventSource = FlowEventSources.fromFlows<Event>(
            StandardTestDispatcher(testScheduler),
            flow {
                for (i in 1..20) {
                    emit(Event("event $i"))
                    delay(100)
                }
            }
        )

        val disposable = eventSource.subscribe { eventsReceived.add(it) }
        advanceTimeBy(300)
        disposable.dispose()
        advanceUntilIdle()

        assertThat(eventsReceived).containsExactly(
            Event("event 1"),
            Event("event 2"),
            Event("event 3"),
        )
    }

    @Test
    @Requirement(
        given = "Event source from flows containing a child event source transformed with asFlow()",
        `when` = "the child event source produces an event",
        then = "the event from the child event source are notified correctly"
    )
    fun eventSourceEventsAreNotified() = runTest {
        val eventsReceived = mutableListOf<Event>()
        val eventSource = FlowEventSources.fromFlows<Event>(
            StandardTestDispatcher(testScheduler),
            EventSource<Event> { eventConsumer ->
                eventConsumer.accept(Event("From Event Source"))
                Disposable { }
            }.asFlow()
        )

        val disposable = eventSource.subscribe { eventsReceived.add(it) }
        advanceUntilIdle()
        disposable.dispose()

        assertThat(eventsReceived).containsExactly(Event("From Event Source"))
    }

    @Test
    @Requirement(
        given = "Event source from flows containing a child event source transformed with asFlow()",
        `when` = "the parent event source is disposed",
        then = "the child event source is also disposed"
    )
    fun eventSourceIsDisposedCorrectly() = runTest {
        var disposeCalled = false
        val eventSource = FlowEventSources.fromFlows(
            StandardTestDispatcher(testScheduler),
            EventSource<Event> {
                Disposable { disposeCalled = true }
            }.asFlow()
        )

        val disposable = eventSource.subscribe { }
        advanceUntilIdle()
        disposable.dispose()
        advanceUntilIdle()

        assertThat(disposeCalled).isTrue()
    }

    private data class Event(val id: String)
}
