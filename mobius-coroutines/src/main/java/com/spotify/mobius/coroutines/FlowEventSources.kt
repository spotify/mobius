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
import java.util.concurrent.atomic.AtomicBoolean
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
                var disposed = AtomicBoolean(false)

                scope.launch {
                    flows.asIterable().merge().collect {
                        synchronized(disposed) {
                            if (!disposed.get()) {
                                consumer.accept(it)
                            }
                        }
                    }
                }

                Disposable {
                    synchronized(disposed) {
                        disposed.set(true)
                    }
                    scope.cancel(CancellationException("EventSource disposed"))
                }
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
