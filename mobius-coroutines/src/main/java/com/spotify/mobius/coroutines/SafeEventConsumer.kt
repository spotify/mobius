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

import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer

/**
 * Forwards events to [delegate] until disposed. [dispose] waits for an in-flight [accept] to finish,
 * because MobiusLoop rejects events once the dispose of its effect handler or event source returns.
 */
internal class SafeEventConsumer<E>(private val delegate: Consumer<E>) : Consumer<E>, Disposable {
    private var disposed = false

    @Synchronized
    override fun accept(value: E) {
        if (!disposed) delegate.accept(value)
    }

    @Synchronized
    override fun dispose() {
        disposed = true
    }
}
