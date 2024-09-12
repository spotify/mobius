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

import com.google.common.truth.Ordered
import com.google.common.truth.Truth.assertThat
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MobiusCoroutinesTest {

    @Test
    @Requirement(
        given = "an effect handler created from MobiusCoroutines.effectHandler",
        `when` = "an effect is dispatched",
        then = "the effect is processed"
    )
    fun effectHandlerProcessEffects() = runTest {
        val testEventConsumer = TestEventConsumer()

        val connection = createEffectHandler().connect(testEventConsumer)
        connection.accept(Effect.ComputeSuccess("success1", 100))
        advanceUntilIdle()

        testEventConsumer.assertConsumedEvents(
            Event.Loading("success1"),
            Event.Success("success1"),
        ).inOrder()
    }


    @Test
    @Requirement(
        given = "an effect handler created from MobiusCoroutines.effectHandler",
        `when` = "an effect is dispatched AND the effect handler is disposed before the effect processing completes",
        then = "the effect is cancelled"
    )
    fun effectHandlerCancelsEffects() = runTest {
        val testEventConsumer = TestEventConsumer()

        val connection = createEffectHandler().connect(testEventConsumer)
        connection.accept(Effect.ComputeSuccess("success1", 100))

        advanceTimeBy(50)
        testEventConsumer.assertConsumedEvents(
            Event.Loading("success1"),
        )

        connection.dispose()
        advanceUntilIdle()
        testEventConsumer.assertConsumedEvents(
            Event.Loading("success1"),
        )
    }

    @Test
    @Requirement(
        given = "an effect handler created from MobiusCoroutines.effectHandler",
        `when` = "several effects are dispatched",
        then = "the effects are processed concurrently"
    )
    fun effectHandlerProcessEffectsConcurrently() = runTest {
        val testEventConsumer = TestEventConsumer()

        val connection = createEffectHandler().connect(testEventConsumer)
        connection.accept(Effect.ComputeSuccess("success1", 100))
        connection.accept(Effect.ComputeFailure("failure1", 150))

        advanceTimeBy(50)
        testEventConsumer.assertConsumedEvents(
            Event.Loading("success1"),
            Event.Loading("failure1"),
        )

        advanceTimeBy(60)
        testEventConsumer.assertConsumedEvents(
            Event.Loading("success1"),
            Event.Loading("failure1"),
            Event.Success("success1"),
        )

        advanceUntilIdle()
        testEventConsumer.assertConsumedEvents(
            Event.Loading("success1"),
            Event.Loading("failure1"),
            Event.Success("success1"),
            Event.Failure("failure1"),
        )
    }

    private fun TestScope.createEffectHandler() =
        MobiusCoroutines.effectHandler<Effect, Event>(StandardTestDispatcher(testScheduler)) { effect, eventConsumer ->
            when (effect) {
                is Effect.ComputeFailure -> {
                    eventConsumer.accept(Event.Loading(effect.id))
                    delay(effect.delayMillis)
                    eventConsumer.accept(Event.Failure(effect.id))
                }

                is Effect.ComputeSuccess -> {
                    eventConsumer.accept(Event.Loading(effect.id))
                    delay(effect.delayMillis)
                    eventConsumer.accept(Event.Success(effect.id))
                }
            }
        }

    sealed class Event {
        data class Loading(val id: String) : Event()
        data class Success(val id: String) : Event()
        data class Failure(val id: String) : Event()
    }

    sealed class Effect {
        data class ComputeSuccess(val id: String, val delayMillis: Long) : Effect()
        data class ComputeFailure(val id: String, val delayMillis: Long) : Effect()
    }

    private class TestEventConsumer : Consumer<Event> {
        val events = mutableListOf<Event>()
        override fun accept(value: Event) {
            events.add(value)
        }

        fun assertConsumedEvents(vararg expectedEvents: Event): Ordered =
            assertThat(events).containsExactly(*expectedEvents)

    }
}