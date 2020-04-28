/*
 * -\-\-
 * Mobius
 * --
 * Copyright (c) 2017-2018 Spotify AB
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
package com.spotify.mobius.android.ktx

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.spotify.mobius.android.LiveQueue

/**
 * A type-alias to simplify code, for a function that acts like an Observable.
 * The parameter of this function is the callback function to be called when an item is emitted.
 */
typealias ObservableFunc<T> = (((T) -> Unit) -> Unit)

/**
 * Curries the target [LiveQueue] with a lifecycle owner. The returned function expects a lambda,
 * which will then be used to subscribe to both foreground and background effects from the target
 * [LiveQueue].
 *
 * The subscription only happens will only happen when the [ObservableFunc] is invoked with an observer.
 */
fun <T> LiveQueue<T>.observeAllWith(lifecycleOwner: LifecycleOwner): ObservableFunc<T> = { observeCallback ->
    setObserver(
        lifecycleOwner,
        Observer { observeCallback(it) },
        Observer { effects -> effects.forEach { observeCallback(it) } }
    )
}

/**
 * Curries the target [LiveQueue] with a lifecycle owner. The returned function expects a lambda,
 * which will then be used to subscribe to only the foreground effects from the target [LiveQueue].
 *
 * The subscription only happens will only happen when the [ObservableFunc] is invoked with an observer.
 */
fun <T> LiveQueue<T>.observeForegroundWith(lifecycleOwner: LifecycleOwner): ObservableFunc<T> =
    { observeCallback ->
        setObserver(lifecycleOwner, Observer { observeCallback(it) })
    }

/**
 * Curries the target [LiveData] with a lifecycle owner. The returned function then expects a
 * lambda that receives events.
 *
 * The call to observe the [LiveData] will only happen when the [ObservableFunc]
 * is invoked with an observer.
 */
fun <T> LiveData<T>.observeWith(lifecycleOwner: LifecycleOwner): ObservableFunc<T> =
    { observeCallback -> observe(lifecycleOwner, Observer { observeCallback(it) }) }

/**
 * Applies the given transformation to each object received by the receiver observable function,
 * giving you a new observable function.
 */
fun <M, S> ObservableFunc<M>.map(mapper: ((M) -> S)): ObservableFunc<S> = { func ->
    this { m: M -> func(mapper(m)) }
}

/**
 * Applies the given filter function to each object received by this observable function,
 * creating a new observable function that only get objects for which the filter returned true.
 */
fun <T> ObservableFunc<T>.filter(filter: ((T) -> Boolean)): ObservableFunc<T> = { func ->
    this { m: T -> if (filter(m)) func(m) }
}
