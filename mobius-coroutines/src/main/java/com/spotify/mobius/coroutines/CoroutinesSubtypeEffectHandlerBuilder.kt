package com.spotify.mobius.coroutines

import com.spotify.mobius.Connectable
import com.spotify.mobius.Connection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.coroutines.CoroutineContext
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
    private val effectHandlersMap = mutableMapOf<KClass<out F>, suspend (F) -> Flow<E>>()

    inline fun <reified G : F> addAction(
        crossinline action: suspend () -> Unit
    ) = addFlowProducer<G> {
        action.invoke()
        flowOf()
    }

    inline fun <reified G : F> addConsumer(
        crossinline consumer: suspend (G) -> Unit
    ) = addFlowProducer<G> { effect ->
        consumer.invoke(effect)
        flowOf()
    }

    inline fun <reified G : F> addProducer(
        crossinline producer: suspend () -> E
    ) = addFlowProducer<G> {
        val event = producer.invoke()
        flowOf(event)
    }

    inline fun <reified G : F> addFunction(
        crossinline function: suspend (G) -> E
    ): CoroutinesSubtypeEffectHandlerBuilder<F, E> = addFlowProducer<G> { effect ->
        val event = function.invoke(effect)
        flowOf(event)
    }

    inline fun <reified G : F> addFlowProducer(
        crossinline function: suspend (G) -> Flow<E>
    ): CoroutinesSubtypeEffectHandlerBuilder<F, E> {
        addEffectHandler(G::class) { effect ->
            function.invoke(effect as G)
        }
        return this
    }

    fun addEffectHandler(kClass: KClass<out F>, function: suspend (F) -> Flow<E>) {
        val previousValue = effectHandlersMap.put(kClass, function)
        if (previousValue != null) error("Trying to add more than one handler for the effect ${kClass.simpleName}")
    }

    fun build(coroutineContext: CoroutineContext) = build(CoroutineScope(coroutineContext))

    private fun build(scope: CoroutineScope) = Connectable { eventConsumer ->
        object : Connection<F> {
            override fun accept(effect: F) {
                scope.launch {
                    if (scope.isActive) {
                        val effectHandler = effectHandlersMap[effect::class] ?: error("No effectHandler for $effect")
                        effectHandler.invoke(effect).collect {
                            if (scope.isActive) eventConsumer.accept(it)
                        }
                    }
                }
            }

            override fun dispose() {
                scope.cancel(CancellationException("Effect Handler disposed"))
            }
        }
    }
}
