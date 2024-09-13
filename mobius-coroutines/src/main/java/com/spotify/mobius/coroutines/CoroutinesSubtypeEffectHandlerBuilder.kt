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

import com.spotify.mobius.Connectable
import com.spotify.mobius.Connection
import com.spotify.mobius.coroutines.CoroutinesSubtypeEffectHandlerBuilder.EffectsHandler
import com.spotify.mobius.coroutines.CoroutinesSubtypeEffectHandlerBuilder.ExecutionPolicy.RunSequentially
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass

/**
 * Builder for a type-routing effect handler.
 *
 * <p>Register handlers for different subtypes of F using the add(...) methods, and call [build]
 * to create an instance of the effect handler. You can then create a loop with the
 * router as the effect handler using [com.spotify.mobius.Mobius.loop].
 *
 * <p>The handler will look at the type of each incoming effect object and try to find a
 * registered handler for that particular subtype of F. If a handler is found, it will be given
 * the effect object, otherwise an exception will be thrown.
 *
 * <p>All the classes that the effect router know about must have a common type F. Note that
 * instances of the builder are mutable and not thread-safe.
 */
class CoroutinesSubtypeEffectHandlerBuilder<F : Any, E : Any> {
    private val effectsHandlersMap = mutableMapOf<KClass<out F>, EffectsHandler<F, E>>()

    /**
     * Adds an "action lambda" for handling effects of a given type. The action will be invoked once
     * for every received effect object that extends the given class.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param G the class to handle
     * @param executionPolicy the [ExecutionPolicy] to use when running effects of the given type
     * @param action the action that should be invoked for the effect
     * @return this builder
     * @throws IllegalStateException if there is a handler collision
     */
    inline fun <reified G : F> addAction(
        executionPolicy: ExecutionPolicy<G, F, E> = RunSequentially(),
        crossinline action: suspend () -> Unit
    ) = addEffectHandler<G>(executionPolicy) { _, _ ->
        action.invoke()
    }

    /**
     * Adds a "consumer lambda" for handling effects of a given type. The consumer will be invoked
     * once for every received effect object that extends the given class.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param G the class to handle
     * @param executionPolicy the [ExecutionPolicy] to use when running effects of the given type
     * @param consumer the consumer that should be invoked for the effect
     * @return this builder
     * @throws IllegalStateException if there is a handler collision
     */
    inline fun <reified G : F> addConsumer(
        executionPolicy: ExecutionPolicy<G, F, E> = RunSequentially(),
        crossinline consumer: suspend (G) -> Unit
    ) = addEffectHandler<G>(executionPolicy) { effect, _ ->
        consumer.invoke(effect)
    }

    /**
     * Adds a "producer lambda" for handling effects of a given type. The producer will be invoked
     * once for every received effect object that extends the given class. The returned event will
     * be forwarded to the Mobius loop.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param G the class to handle
     * @param executionPolicy the [ExecutionPolicy] to use when running effects of the given type
     * @param producer the producer that should be invoked for the effect
     * @return this builder
     * @throws IllegalStateException if there is a handler collision
     */
    inline fun <reified G : F> addProducer(
        executionPolicy: ExecutionPolicy<G, F, E> = RunSequentially(),
        crossinline producer: suspend () -> E
    ) = addEffectHandler<G>(executionPolicy) { _, eventsChannel ->
        val event = producer.invoke()
        eventsChannel.send(event)
    }

    /**
     * Adds a "function lambda" for handling effects of a given type. The function will be invoked
     * once for every received effect object that extends the given class. The returned event will
     * be forwarded to the Mobius loop.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param G the class to handle
     * @param executionPolicy the [ExecutionPolicy] to use when running effects of the given type
     * @param function the function that should be invoked for the effect
     * @return this builder
     * @throws IllegalStateException if there is a handler collision
     */
    inline fun <reified G : F> addFunction(
        executionPolicy: ExecutionPolicy<G, F, E> = RunSequentially(),
        crossinline function: suspend (G) -> E
    ) = addEffectHandler<G>(executionPolicy) { effect, eventsChannel ->
        val event = function.invoke(effect)
        eventsChannel.send(event)
    }

    /**
     * Adds a "flow collector function lambda" for handling effects of a given type. A flow will be created and
     * the flow collector function will be invoked once for every received effect object that extends the given class.
     * The emitted events will be forwarded to the Mobius loop.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param G the class to handle
     * @param executionPolicy the [ExecutionPolicy] to use when running effects of the given type
     * @param flowCollectorFunction the function that should be invoked for the effect
     * @return this builder
     * @throws IllegalStateException if there is a handler collision
     */
    inline fun <reified G : F> addFlow(
        executionPolicy: ExecutionPolicy<G, F, E> = RunSequentially(),
        crossinline flowCollectorFunction: suspend FlowCollector<E>.(G) -> Unit
    ) = addEffectHandler<G>(executionPolicy) { effect, eventsChannel ->
        flow { flowCollectorFunction(effect) }
            .collect { event -> eventsChannel.send(event) }
    }

    /**
     * Adds a "flow producer lambda" for handling effects of a given type. The flow producer function will be invoked
     * once for every received effect object that extends the given class. The emitted events
     * will be forwarded to the Mobius loop.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param G the class to handle
     * @param executionPolicy the [ExecutionPolicy] to use when running effects of the given type
     * @param function the function that should be invoked for the effect to create the flow
     * @return this builder
     * @throws IllegalStateException if there is a handler collision
     */
    inline fun <reified G : F> addFlowProducer(
        executionPolicy: ExecutionPolicy<G, F, E> = RunSequentially(),
        crossinline function: suspend (G) -> Flow<E>
    ) = addEffectHandler<G>(executionPolicy) { effect, eventsChannel ->
        function.invoke(effect)
            .collect { event -> eventsChannel.send(event) }
    }

    /**
     * Adds an [EffectHandler] for handling effects of a given type. The [EffectHandler.handleEffect] function will
     * be invoked once for every received effect object that extends the given class. TThe events sent to
     * the eventsChannel will be forwarded to the Mobius loop.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param G the class to handle
     * @param executionPolicy the [ExecutionPolicy] to use when running effects of the given type
     * @param effectHandler the [EffectHandler] that should be invoked for the effect
     * @return this builder
     * @throws IllegalStateException if there is a handler collision
     */
    inline fun <reified G : F> addEffectHandler(
        executionPolicy: ExecutionPolicy<G, F, E> = RunSequentially(),
        effectHandler: EffectHandler<G, E>,
    ): CoroutinesSubtypeEffectHandlerBuilder<F, E> {
        addEffectHandler(G::class, executionPolicy.createEffectsHandler(effectHandler))
        return this
    }

    /**
     * Adds an [EffectsHandler] for handling effects of a given type. The [EffectsHandler.handleEffects] function will
     * be invoked only once, when the first effect that extends the given class is emitted. All effects from the
     * Mobius loop, will be forwarded to the effectsChannel. The events sent to the eventsChannel will be forwarded
     * back to the Mobius loop.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param kClass the class to handle
     * @param effectsHandler the [EffectsHandler] that should be invoked for all the effects
     * @return this builder
     * @throws IllegalStateException if there is a handler collision
     */
    fun addEffectHandler(kClass: KClass<out F>, effectsHandler: EffectsHandler<F, E>) {
        val previousValue = effectsHandlersMap.put(kClass, effectsHandler)
        if (previousValue != null) error("Trying to add more than one handler for the effect ${kClass.simpleName}")
    }

    /**
     * Creates a [Connectable] to be used as an effect handler. It is backed by an internal [CoroutineScope] created
     * with the given [CoroutineContext]. All coroutines will be canceled when the [Connection.dispose] method
     * is called in a [Connection] created by this [Connectable].
     *
     * @param coroutineContext the context where the effects will run.
     * @return a [Connectable] to be used as an effect handler.
     * */
    fun build(coroutineContext: CoroutineContext = EmptyCoroutineContext) = Connectable { eventConsumer ->
        val scope = CoroutineScope(coroutineContext)
        val eventsChannel = Channel<E>()
        val subEffectChannelsMap = mutableMapOf<KClass<out F>, Channel<F>>()
        val mutex = Mutex()

        // Connects the eventConsumer
        scope.launch {
            for (event in eventsChannel) {
                if (isActive) eventConsumer.accept(event)
            }
        }

        object : Connection<F> {
            override fun accept(effect: F) {
                scope.launch {
                    val subEffectChannel = mutex.withLock {
                        // Prevents the creation of an effectChannel if the scope is not active
                        if(!isActive) return@launch

                        subEffectChannelsMap.getOrPut(effect::class) {
                            // Creates an effectChannel the first time the effect is processed
                            val subEffectChannel = Channel<F>()
                            val effectHandler = effectsHandlersMap[effect::class]
                                ?: error("No effectHandler for $effect")
                            // Connects the effectHandler the first time the effect is processed
                            scope.launch {
                                if (isActive) effectHandler.handleEffects(
                                    subEffectChannel, eventsChannel
                                )
                            }
                            subEffectChannel
                        }
                    }

                    // Prevents the processing of the effect if the scope is not active
                    if (isActive) subEffectChannel.send(effect)
                }
            }

            override fun dispose() {
                scope.cancel("Effect Handler disposed")
                eventsChannel.close()
                runBlocking {
                    mutex.withLock {
                        subEffectChannelsMap.values.forEach { it.close() }
                        subEffectChannelsMap.clear()
                    }
                }
            }
        }
    }

    /**
     * An execution policy defines how effects of the same type are executed. It is used to create
     * an [EffectsHandler] from an [EffectHandler] implementing its own concurrency execution policy.
     * */
    @Suppress("UNCHECKED_CAST")
    fun interface ExecutionPolicy<G : F, F, E> {

        /**
         * Creates an [EffectsHandler] from an [EffectHandler] implementing its own concurrency execution policy.
         *
         * @param effectHandler the [EffectHandler] to use when handling single effects of type [G]
         * @return an [EffectsHandler] that handles all effects of type [G]
         * */
        fun createEffectsHandler(effectHandler: EffectHandler<G, E>): EffectsHandler<F, E>

        /**
         *  Implementation of [ExecutionPolicy] where all effects of the same type wait for the previous one
         *  to finish executing before being executed.
         * */
        class RunSequentially<G : F, F : Any, E : Any> : ExecutionPolicy<G, F, E> {
            override fun createEffectsHandler(effectHandler: EffectHandler<G, E>) =
                EffectsHandler<F, E> { effectChannel, eventsChannel ->
                    for (effect in effectChannel) {
                        effectHandler.handleEffect(effect as G, eventsChannel)
                    }
                }
        }

        /**
         *  Implementation of [ExecutionPolicy] where all effects of the same type are executed immediately
         *  and concurrently.
         * */
        class RunConcurrently<G : F, F, E> : ExecutionPolicy<G, F, E> {
            override fun createEffectsHandler(effectHandler: EffectHandler<G, E>) =
                EffectsHandler<F, E> { effectChannel, eventsChannel ->
                    coroutineScope {
                        for (effect in effectChannel) {
                            launch { effectHandler.handleEffect(effect as G, eventsChannel) }
                        }
                    }
                }
        }

        /**
         *  Implementation of [ExecutionPolicy] where a new effect cancels the execution of any previously running
         *  effect of the same type and start executing immediately.
         * */
        class CancelPrevious<G : F, F, E> : ExecutionPolicy<G, F, E> {
            override fun createEffectsHandler(effectHandler: EffectHandler<G, E>) =
                EffectsHandler<F, E> { effectChannel, eventsChannel ->
                    coroutineScope {
                        var currentJob: Job = Job()
                        for (effect in effectChannel) {
                            currentJob.cancel()
                            currentJob = launch { effectHandler.handleEffect(effect as G, eventsChannel) }
                        }
                    }
                }
        }
    }

    /**
     * An [EffectHandler] is a function that handles a single effect of type [F] and sends events of type [E].
     * It is used in conjunction with an [ExecutionPolicy] to create an [EffectsHandler] for handling all effects of
     * the same type.
     * */
    fun interface EffectHandler<F, E> {
        /**
         * Handles a single effect of type [F] and sends events of type [E].
         *
         *
         * @param effect the effect to handle
         * @param eventsChannel the channel where the events should be sent
         * */
        suspend fun handleEffect(effect: F, eventsChannel: SendChannel<E>)
    }

    /**
     * An [EffectsHandler] is a function that handles all effects of type [F] and sends events of type [E].
     * */
    fun interface EffectsHandler<F, E> {
        /**
         * Handles all effects of type [F] and sends events of type [E].
         *
         * @param effectsChannel the channel where the effects are received
         * @param eventsChannel the channel where the events should be sent
         * */
        suspend fun handleEffects(effectsChannel: ReceiveChannel<F>, eventsChannel: SendChannel<E>)
    }
}
