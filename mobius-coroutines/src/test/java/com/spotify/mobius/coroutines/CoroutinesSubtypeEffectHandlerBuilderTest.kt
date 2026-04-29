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
import com.google.common.truth.Truth.assertWithMessage
import com.spotify.mobius.coroutines.CoroutinesSubtypeEffectHandlerBuilder.ExecutionPolicy
import com.spotify.mobius.coroutines.MobiusCoroutines.Companion.subtypeEffectHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Test
import java.util.Collections
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutinesSubtypeEffectHandlerBuilderTest {

    @Test
    @Requirement(
        given = "A CoroutinesSubtypeEffectHandlerBuilder",
        `when` = "two effect handlers are added for the same effect",
        then = "an exception is thrown"
    )
    fun duplicateEffectHandler() {
        assertThrows(RuntimeException::class.java) {
            subtypeEffectHandler<Effect, Event>()
                .addAction<Effect.Simple> { }
                .addConsumer<Effect.Simple> { }
        }
    }

    @Test
    @Requirement(
        given = "A connectable without any effect handlers",
        `when` = "an effect is produced",
        then = "an exception is thrown"
    )
    fun emptyEffectHandler() {
        assertThrows(RuntimeException::class.java) {
            runTest {
                val effectHandler = subtypeEffectHandler<Effect, Event>()

                effectHandler.build(UnconfinedTestDispatcher(testScheduler))
                    .connect { }
                    .accept(Effect.Simple)
                advanceUntilIdle()
            }
        }
    }

    @Test
    @Requirement(
        given = "An action as effect handler",
        `when` = "a matching effect is produced",
        then = "the effect is consumed successfully"
    )
    fun actionEffectHandler() = runTest {
        var actionCalled = false
        val effectHandler = subtypeEffectHandler<Effect, Event>()
            .addAction<Effect.Simple> { actionCalled = true }


        effectHandler.build(UnconfinedTestDispatcher(testScheduler))
            .connect { }
            .accept(Effect.Simple)
        advanceUntilIdle()

        assertThat(actionCalled).isTrue()
    }

    @Test
    @Requirement(
        given = "An effect handler failing with an exception",
        `when` = "a matching effect is produced",
        then = "the exception is propagated"
    )
    fun actionEffectHandlerError() {
        val effectHandler = subtypeEffectHandler<Effect, Event>()
            .addAction<Effect.Simple> { error("Error in Action") }

        assertThrows(RuntimeException::class.java) {
            runTest {
                effectHandler.build(UnconfinedTestDispatcher(testScheduler))
                    .connect { }
                    .accept(Effect.Simple)
                advanceUntilIdle()
            }
        }
    }

    @Test
    @Requirement(
        given = "A connectable with a consumer as effect handler",
        `when` = "a matching effect is produced",
        then = "the effect is consumed successfully"
    )
    fun consumerEffectHandler() = runTest {
        var effectConsumed: Effect? = null
        val effectHandler = subtypeEffectHandler<Effect, Event>()
            .addConsumer<Effect.SingleValue> { effectConsumed = it }

        effectHandler.build(UnconfinedTestDispatcher(testScheduler))
            .connect { }
            .accept(Effect.SingleValue("Effect to consume"))
        advanceUntilIdle()

        assertThat(effectConsumed).isEqualTo(Effect.SingleValue("Effect to consume"))
    }

    @Test
    @Requirement(
        given = "A connectable with a producer as effect handler",
        `when` = "a matching effect is produced",
        then = "the effect is consumed successfully " +
                "AND the produced event is propagated"
    )
    fun producerEffectHandler() = runTest {
        var producerCalled = false
        var eventProduced: Event? = null
        val effectHandler = subtypeEffectHandler<Effect, Event>()
            .addProducer<Effect.Simple> {
                producerCalled = true
                Event.SingleValue("Produced event")
            }

        effectHandler.build(UnconfinedTestDispatcher(testScheduler))
            .connect { eventProduced = it }
            .accept(Effect.Simple)
        advanceUntilIdle()

        assertThat(producerCalled).isTrue()
        assertThat(eventProduced).isEqualTo(Event.SingleValue("Produced event"))
    }

    @Test
    @Requirement(
        given = "A connectable with a function as effect handler",
        `when` = "a matching effect is produced",
        then = "the effect is consumed successfully " +
                "AND the produced event is propagated"
    )
    fun functionEffectHandler() = runTest {
        var eventProduced: Event? = null
        var effectConsumed: Effect? = null
        val effectHandler = subtypeEffectHandler<Effect, Event>()
            .addFunction<Effect.SingleValue> {
                effectConsumed = it
                Event.SingleValue("Produced event")
            }

        effectHandler.build(UnconfinedTestDispatcher(testScheduler))
            .connect { eventProduced = it }
            .accept(Effect.SingleValue("Effect to produce event"))
        advanceUntilIdle()

        assertThat(effectConsumed).isEqualTo(Effect.SingleValue("Effect to produce event"))
        assertThat(eventProduced).isEqualTo(Event.SingleValue("Produced event"))
    }

    @Test
    @Requirement(
        given = "A connectable with a flow as effect handler",
        `when` = "a matching effect is produced",
        then = "the effect is consumed successfully " +
                "AND all the produced events are propagated"
    )
    fun flowEffectHandler() = runTest {
        val eventsProduced = mutableListOf<Event>()
        var effectConsumed: Effect? = null
        val effectHandler = subtypeEffectHandler<Effect, Event>()
            .addFlow<Effect.ValueList> { effect ->
                effectConsumed = effect
                effect.tokens.forEach { token -> emit(Event.SingleValue(token)) }
            }

        effectHandler.build(UnconfinedTestDispatcher(testScheduler))
            .connect { eventsProduced.add(it) }
            .accept(Effect.ValueList(listOf("token1", "token2", "token3")))
        advanceUntilIdle()

        assertThat(effectConsumed).isEqualTo(Effect.ValueList(listOf("token1", "token2", "token3")))
        assertThat(eventsProduced).containsExactly(
            Event.SingleValue("token1"),
            Event.SingleValue("token2"),
            Event.SingleValue("token3"),
        )
    }

    @Test
    @Requirement(
        given = "A connectable with a flow producer as effect handler",
        `when` = "a matching effect is produced",
        then = "the effect is consumed successfully " +
                "AND all the produced events are propagated"
    )
    fun flowProducerEffectHandler() = runTest {
        val eventsProduced = mutableListOf<Event>()
        var effectConsumed: Effect? = null
        val effectHandler = subtypeEffectHandler<Effect, Event>()
            .addFlowProducer<Effect.ValueList> { effect ->
                effectConsumed = effect
                flow {
                    effect.tokens.forEach { token -> emit(Event.SingleValue(token)) }
                }
            }

        effectHandler.build(UnconfinedTestDispatcher(testScheduler))
            .connect { eventsProduced.add(it) }
            .accept(Effect.ValueList(listOf("token1", "token2", "token3")))
        advanceUntilIdle()

        assertThat(effectConsumed).isEqualTo(Effect.ValueList(listOf("token1", "token2", "token3")))
        assertThat(eventsProduced).containsExactly(
            Event.SingleValue("token1"),
            Event.SingleValue("token2"),
            Event.SingleValue("token3"),
        )
    }

    @Test
    @Requirement(
        given = "An effect handler",
        `when` = "the effect handler is disposed",
        then = "all running task are cancelled"
    )
    fun disposeEffectHandler() = runTest {
        var effectStarted = false
        var effectFinished = false
        val effectHandler = subtypeEffectHandler<Effect, Event>()
            .addAction<Effect.Simple> {
                effectStarted = true
                delay(1000)
                effectFinished = true
            }

        val connection = effectHandler.build(StandardTestDispatcher(testScheduler))
            .connect { }

        connection.accept(Effect.Simple)
        advanceTimeBy(500)
        connection.dispose()
        advanceUntilIdle()

        assertThat(effectStarted).isTrue()
        assertThat(effectFinished).isFalse()
    }

    @Test
    @Requirement(
        given = "A disposed connection",
        `when` = "an effect is accepted",
        then = "the registered handler is not invoked"
    )
    fun acceptAfterDisposeIsANoOp() = runTest {
        var handlerCalled = false
        val connection = subtypeEffectHandler<Effect, Event>()
            .addAction<Effect.Simple> { handlerCalled = true }
            .build(UnconfinedTestDispatcher(testScheduler))
            .connect { }

        connection.dispose()
        connection.accept(Effect.Simple)
        advanceUntilIdle()

        assertThat(handlerCalled).isFalse()
    }

    @Test
    @Requirement(
        given = "A connection with a handler in flight",
        `when` = "dispose is called multiple times",
        then = "no exception is thrown"
    )
    fun multipleDisposeIsIdempotent() {
        val uncaughtExceptions = Collections.synchronizedList(mutableListOf<Throwable>())
        val internalExceptionHandler = CoroutineExceptionHandler { _, t -> uncaughtExceptions.add(t) }

        val connection = subtypeEffectHandler<Effect, Event>()
            .addAction<Effect.Simple> { delay(50) }
            .build(internalExceptionHandler)
            .connect { }

        connection.accept(Effect.Simple)
        Thread.sleep(10) // let the handler start

        repeat(5) { connection.dispose() }

        Thread.sleep(50) // let cancellations settle

        assertThat(uncaughtExceptions).isEmpty()
    }

    @Test
    @Requirement(
        given = "An effect handler using RunSequentially cancellation policy",
        `when` = "several matching effects are produced",
        then = "all the effect are started successfully" +
                "AND the effects are consumed sequentially"
    )
    fun processEffectHandlerSequentially() = runTest {
        val effectsConsumed = mutableListOf<Effect.DelayAction>()
        val effectHandler = subtypeEffectHandler<Effect, Event>()
            .addConsumer<Effect.DelayAction>(executionPolicy = ExecutionPolicy.RunSequentially()) { effect ->
                delay(effect.delayMillis)
                effectsConsumed.add(effect)
            }


        val connection = effectHandler.build(StandardTestDispatcher(testScheduler))
            .connect { }
        connection.accept(Effect.DelayAction(300))
        connection.accept(Effect.DelayAction(200))
        connection.accept(Effect.DelayAction(100))
        advanceUntilIdle()

        assertThat(effectsConsumed).containsExactly(
            Effect.DelayAction(300),
            Effect.DelayAction(200),
            Effect.DelayAction(100)
        ).inOrder()
    }

    @Test
    @Requirement(
        given = "An effect handler using RunConcurrently cancellation policy",
        `when` = "several matching effects are produced",
        then = "all the effect are started successfully" +
                "AND the effects are consumed concurrently"
    )
    fun processEffectHandlerConcurrently() = runTest {
        val effectsConsumed = mutableListOf<Effect.DelayAction>()
        val effectHandler = subtypeEffectHandler<Effect, Event>()
            .addConsumer<Effect.DelayAction>(executionPolicy = ExecutionPolicy.RunConcurrently()) { effect ->
                delay(effect.delayMillis)
                effectsConsumed.add(effect)
            }

        val connection = effectHandler.build(StandardTestDispatcher(testScheduler))
            .connect { }
        connection.accept(Effect.DelayAction(300))
        connection.accept(Effect.DelayAction(200))
        connection.accept(Effect.DelayAction(100))
        advanceUntilIdle()

        assertThat(effectsConsumed).containsExactly(
            Effect.DelayAction(100),
            Effect.DelayAction(200),
            Effect.DelayAction(300)
        ).inOrder()
    }

    @Test
    @Requirement(
        given = "An effect handler using CancelPrevious cancellation policy",
        `when` = "several matching effects are produced",
        then = "all the effect are started successfully" +
                "AND new effects cancel previous effects while running"
    )
    fun processEffectHandlerCancelPrevious() = runTest {
        val effectsStarted = mutableListOf<Effect.DelayAction>()
        val effectsFinished = mutableListOf<Effect.DelayAction>()
        val effectHandler = subtypeEffectHandler<Effect, Event>()
            .addConsumer<Effect.DelayAction>(executionPolicy = ExecutionPolicy.CancelPrevious()) { effect ->
                effectsStarted.add(effect)
                delay(effect.delayMillis)
                effectsFinished.add(effect)
            }

        val connection = effectHandler.build(StandardTestDispatcher(testScheduler))
            .connect { }

        connection.accept(Effect.DelayAction(300))
        advanceTimeBy(50)
        assertThat(effectsStarted).containsExactly(
            Effect.DelayAction(300),
        )
        assertThat(effectsFinished).isEmpty()

        connection.accept(Effect.DelayAction(200))
        advanceTimeBy(210)
        assertThat(effectsStarted).containsExactly(
            Effect.DelayAction(300),
            Effect.DelayAction(200),
        ).inOrder()
        assertThat(effectsFinished).containsExactly(
            Effect.DelayAction(200),
        )

        connection.accept(Effect.DelayAction(100))
        advanceUntilIdle()
        assertThat(effectsStarted).containsExactly(
            Effect.DelayAction(300),
            Effect.DelayAction(200),
            Effect.DelayAction(100)
        ).inOrder()
        assertThat(effectsFinished).containsExactly(
            Effect.DelayAction(200),
            Effect.DelayAction(100),
        ).inOrder()
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun toTransformerNoEventsAreGeneratedAfterDispose() {

        val executor = Executors.newSingleThreadExecutor()
        val scope = CoroutineScope(executor.asCoroutineDispatcher())

        // Capture exceptions thrown inside the connectable's internal coroutines. Without this
        // handler they propagate to the JVM uncaught-exception handler, which doesn't fail the
        // test — masking races like ClosedSendChannelException between accept() and dispose().
        val uncaughtExceptions = Collections.synchronizedList(mutableListOf<Throwable>())
        val internalExceptionHandler = CoroutineExceptionHandler { _, t -> uncaughtExceptions.add(t) }

        // given a handler that responds with events. Note: handler is registered for the concrete
        // subtype actually being dispatched — lookup uses exact KClass match. The delay holds the
        // sub-effect channel busy so accept-coroutines suspend on send, widening the race window
        // when dispose() closes the sub-channel concurrently (the production crash signature).
        val connectable = subtypeEffectHandler<Effect, Event>()
            .addFunction<Effect.Simple> {
                delay(1)
                Event.SingleValue("value")
            }
            .build(internalExceptionHandler)

        // when a connectable is subscribed to (many times to make this non-flaky/less flaky)
        for (i in 1..999) {

            var disposed = false
            var calledAfterDispose = false
            val connection = connectable.connect {
                if (disposed) {
                    calledAfterDispose = true
                }
            }

            // given several producers that emit a burst of effects (bounded so suspended
            // coroutines don't pile up faster than they can drain — with a delayed handler the
            // unbounded variant exhausts native resources before the test loop completes)
            val job = scope.launch {
                repeat(200) {
                    if (!isActive) return@launch
                    connection.accept(Effect.Simple)
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

            job.cancel()

            // Fail fast on the first race
            if (uncaughtExceptions.isNotEmpty()) break
        }

        scope.cancel()

        assertWithMessage("Uncaught exceptions in connectable's internal coroutines: %s", uncaughtExceptions)
            .that(uncaughtExceptions)
            .isEmpty()
    }

    @Test
    @Requirement(
        given = "An effect is being handled and another is suspended on the sub-effect channel",
        `when` = "the connection is disposed",
        then = "no uncaught exception is thrown by the suspended sender"
    )
    fun disposeWhileEffectSuspendedOnSubChannelDoesNotCrash() {
        val uncaughtExceptions = Collections.synchronizedList(mutableListOf<Throwable>())
        val internalExceptionHandler = CoroutineExceptionHandler { _, t -> uncaughtExceptions.add(t) }

        // Sets up the precondition for the production crash: an accept-coroutine suspended on
        // subEffectChannel.send when dispose() closes the channel. With the bug present, the
        // close races against scope.cancel and can wake the suspended sender with
        // ClosedSendChannelException instead of CancellationException. Repeated to raise the
        // chance the race resolves in the bad direction (kotlinx-coroutines tends to win
        // cancellation propagation, but production has shown it can lose).
        repeat(200) { iteration ->
            val handlerCanProceed = CompletableDeferred<Unit>()
            val handlerStarted = CompletableDeferred<Unit>()

            val connectable = subtypeEffectHandler<Effect, Event>()
                .addAction<Effect.Simple> {
                    handlerStarted.complete(Unit)
                    handlerCanProceed.await()
                }
                .build(internalExceptionHandler)

            val connection = connectable.connect { }

            runBlocking {
                // First effect — handler starts and suspends on handlerCanProceed.
                connection.accept(Effect.Simple)
                handlerStarted.await()

                // Second effect — accept-coroutine acquires mutex, finds existing channel,
                // releases mutex, calls subEffectChannel.send(effect). send suspends because
                // the receiver is busy awaiting handlerCanProceed.
                connection.accept(Effect.Simple)

                // Give the second accept-coroutine time to actually reach the suspended send.
                delay(20)

                // Dispose. This closes the sub-effect channel, which can wake the suspended
                // sender with ClosedSendChannelException if cancellation hasn't propagated yet.
                connection.dispose()

                // Unblock the handler so it can exit (scope is cancelled so its await throws
                // CancellationException, which is the expected, handled outcome).
                handlerCanProceed.complete(Unit)
                delay(20)
            }

            if (uncaughtExceptions.isNotEmpty()) {
                fail("Uncaught exception during dispose race on iteration $iteration: $uncaughtExceptions")
            }
        }
    }

    private sealed interface Effect {
        data object Simple : Effect
        data class SingleValue(val id: String) : Effect
        data class ValueList(val tokens: List<String>) : Effect
        data class DelayAction(val delayMillis: Long) : Effect
    }

    private sealed interface Event {
        data class SingleValue(val id: String) : Event
    }
}
