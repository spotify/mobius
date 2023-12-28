package com.spotify.mobius.coroutines

import com.google.common.truth.Truth.assertThat
import com.spotify.mobius.coroutines.MobiusCoroutines.Companion.subtypeEffectHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

@ExperimentalCoroutinesApi
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

                effectHandler.build(coroutineContext)
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

        effectHandler.build(coroutineContext)
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
                effectHandler.build(coroutineContext)
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

        effectHandler.build(coroutineContext)
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

        effectHandler.build(coroutineContext)
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

        effectHandler.build(coroutineContext)
            .connect { eventProduced = it }
            .accept(Effect.SingleValue("Effect to produce event"))
        advanceUntilIdle()

        assertThat(effectConsumed).isEqualTo(Effect.SingleValue("Effect to produce event"))
        assertThat(eventProduced).isEqualTo(Event.SingleValue("Produced event"))
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

        effectHandler.build(coroutineContext)
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

        val connection = effectHandler.build(StandardTestDispatcher(testScheduler) + Job())
            .connect { }
        connection.accept(Effect.Simple)
        advanceTimeBy(500)
        connection.dispose()
        advanceUntilIdle()

        assertThat(effectStarted).isTrue()
        assertThat(effectFinished).isFalse()
    }

    private sealed interface Effect {
        data object Simple : Effect
        data class SingleValue(val id: String) : Effect
        data class ValueList(val tokens: List<String>) : Effect
    }

    private sealed interface Event {
        data class SingleValue(val id: String) : Event
    }
}
