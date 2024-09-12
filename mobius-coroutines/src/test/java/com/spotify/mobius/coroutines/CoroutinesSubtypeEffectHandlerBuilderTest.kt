package com.spotify.mobius.coroutines

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.spotify.mobius.coroutines.CoroutinesSubtypeEffectHandlerBuilder.ExecutionPolicy
import com.spotify.mobius.coroutines.MobiusCoroutines.Companion.subtypeEffectHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.Assert.assertThrows
import org.junit.Test
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

        // given a handler that responds with events
        val connectable = subtypeEffectHandler<Effect, Event>()
            .addFunction<Effect> { Event.SingleValue("value") }
            .build()

        // when a connectable is subscribed to (many times to make this non-flaky/less flaky)
        for (i in 1..999) {

            var disposed = false
            var calledAfterDispose = false
            val connection = connectable.connect {
                if (disposed) {
                    calledAfterDispose = true
                }
            }

            // given a channel that continuously emits stuff
            val job = scope.launch {
                while (isActive) {
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
        }

        scope.cancel()
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
