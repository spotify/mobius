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

import com.spotify.mobius.runners.WorkRunner
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** A [WorkRunner] that is backed by a [CoroutineContext] for running work. */
class DispatcherWorker(coroutineContext: CoroutineContext) : WorkRunner {
    private val scope = CoroutineScope(coroutineContext)

    override fun post(task: Runnable) {
        scope.launch {
            if (scope.isActive) task.run()
        }
    }

    override fun dispose() {
        scope.cancel(CancellationException("DispatcherWorker disposed"))
    }
}
