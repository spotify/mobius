package com.spotify.mobius.coroutines

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.spotify.mobius.EventSource
import com.spotify.mobius.coroutines.FlowEventSources.Companion.asFlow
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

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

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun fromFlowNoEventsAreGeneratedAfterDispose() {

        val executor = Executors.newSingleThreadExecutor()
        val scope = CoroutineScope(executor.asCoroutineDispatcher())
        val flow = MutableStateFlow(0)


        // given a channel that constantly emits events
        scope.launch {
            var i = 0
            while (isActive) {
                flow.value = i++
            }
        }

        val source = FlowEventSources.fromFlows(Dispatchers.Default, flow)

        // when a connectable is subscribed to (many times to make this non-flaky/less flaky)
        for (i in 1..999) {

            var disposed = false
            var calledAfterDispose = false
            val connection = source.subscribe {
                if (disposed) {
                    calledAfterDispose = true
                }
            }

            // the sleep here and below is not strictly necessary, but it helps provoke errors more
            // frequently (on my laptop at least..). YMMV in case there is another issue like this one
            // in the future.
            runBlocking {
                delay(1)
            }

            // then, the event observer doesn't receive events after it has been disposed.
            connection.dispose()
            disposed = true

            runBlocking {
                delay(3)
            }

            assertWithMessage("accept called after dispose on attempt %s", i)
                .that(calledAfterDispose)
                .isFalse()
        }

        scope.cancel()
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun toFlowNoEventsAreGeneratedAfterDispose() {

        val executor = Executors.newSingleThreadExecutor()
        val scope = CoroutineScope(executor.asCoroutineDispatcher())
        val flow = MutableStateFlow(0)


        // given a channel that constantly emits events
        scope.launch {
            var i = 0
            while (isActive) {
                flow.value = i++
            }
        }

        val source = object : EventSource<Int> {
            override fun subscribe(eventConsumer: Consumer<Int>): Disposable {
                val job = scope.launch {
                    flow.collect { eventConsumer.accept(it) }
                }
                return Disposable {
                    job.cancel()
                }
            }
        }

        // when a connectable is subscribed to (many times to make this non-flaky/less flaky)
        for (i in 1..999) {

            var disposed = false
            var calledAfterDispose = false
            val job = scope.launch {
                source.asFlow().collect {
                    if (disposed) {
                        calledAfterDispose = true
                    }
                }
            }

            // the sleep here and below is not strictly necessary, but it helps provoke errors more
            // frequently (on my laptop at least..). YMMV in case there is another issue like this one
            // in the future.
            runBlocking {
                delay(1)
            }

            // then, the event observer doesn't receive events after it has been disposed.
            job.cancel()
            disposed = true

            runBlocking {
                delay(3)
            }

            assertWithMessage("accept called after dispose on attempt %s", i)
                .that(calledAfterDispose)
                .isFalse()
        }

        scope.cancel()
    }

    private data class Event(val id: String)
}
