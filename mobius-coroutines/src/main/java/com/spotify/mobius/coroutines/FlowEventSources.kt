package com.spotify.mobius.coroutines

import com.spotify.mobius.EventSource
import com.spotify.mobius.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

/** Contains utility methods for converting back and forth between [Flow]s and [EventSource]s. */
interface FlowEventSources {
    companion object {

        /**
         * Creates an [EventSource] from the given [Flow]s.
         *
         * <p>All streams must be mapped to your event type.
         *
         * @param coroutineContext the context where the flows will run.
         * @param flows the [Flow]s you want to include in the returned [EventSource].
         * @param <E> the event type.
         *
         * @return an [EventSource] based on the provided [Flow]s
         */
        fun <E : Any> fromFlows(coroutineContext: CoroutineContext = Dispatchers.Default, vararg flows: Flow<E>) =
            EventSource { consumer ->
                val scope = CoroutineScope(coroutineContext)

                scope.launch {
                    flows.asIterable().merge().collect { consumer.accept(it) }
                }

                Disposable { scope.cancel(CancellationException("EventSource disposed")) }
            }

        /**
         * Creates a [Flow] from the given [EventSource].
         *
         * @receiver the [EventSource] you want to convert to a [Flow]
         * @param <E> the event type
         * @return a [Flow] based on the provided [EventSource]
         */
        fun <E : Any> EventSource<E>.asFlow() = callbackFlow<E> {
            val disposable = this@asFlow.subscribe { event ->
                trySendBlocking(event)
                    .onFailure { exception ->
                        if (exception != null && exception !is InterruptedException) throw exception
                    }
            }
            awaitClose {
                disposable.dispose()
            }
        }
    }
}
