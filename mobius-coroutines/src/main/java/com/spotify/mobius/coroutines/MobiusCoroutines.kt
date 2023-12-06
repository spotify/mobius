package com.spotify.mobius.coroutines

import com.spotify.mobius.MobiusLoop
import kotlin.coroutines.CoroutineContext

/** Factory methods for wrapping Mobius core classes in coroutines transformers. */
interface MobiusCoroutines {

    companion object {
        /**
         * Create a [CoroutinesSubtypeEffectHandlerBuilder] for handling effects based on their type.
         *
         * @param <F> the effect type
         * @param <E> the event type
         */
        fun <F : Any, E : Any> subtypeEffectHandler() = CoroutinesSubtypeEffectHandlerBuilder<F, E>()

        /**
         * Returns a new [MobiusLoop.Builder] with a [DispatcherWorker] using the supplied [CoroutineContext]
         * as effect runner, and the same values as the current one for the other fields.
         */
        fun <M, E, F> MobiusLoop.Builder<M, E, F>.effectRunner(coroutineContext: CoroutineContext) =
            effectRunner { DispatcherWorker(coroutineContext) }

        /**
         * Returns a new [MobiusLoop.Builder] with a [DispatcherWorker] using the supplied [CoroutineContext]
         * as event runner, and the same values as the current one for the other fields.
         */
        fun <M, E, F> MobiusLoop.Builder<M, E, F>.eventRunner(coroutineContext: CoroutineContext) =
            eventRunner { DispatcherWorker(coroutineContext) }
    }
}
