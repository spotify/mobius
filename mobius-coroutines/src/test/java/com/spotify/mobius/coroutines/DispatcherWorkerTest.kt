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

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

@ExperimentalCoroutinesApi
class DispatcherWorkerTest {

    @Test
    @Requirement(
        given = "Dispatcher worker",
        `when` = "a work is post",
        then = "the work is processed"
    )
    fun postedWorkIsProcessed() = runTest {
        val worker = DispatcherWorker(coroutineContext)
        var taskProcessed = false

        worker.post { taskProcessed = true }
        advanceUntilIdle()

        assertThat(taskProcessed).isTrue()
    }

    @Test
    @Requirement(
        given = "Dispatcher worker",
        `when` = "a work is post throwing an exception",
        then = "the exception is propagated"
    )
    fun exceptionsArePropagated() {
        assertThrows("Exception in work", RuntimeException::class.java) {
            runTest {
                val worker = DispatcherWorker(coroutineContext)

                worker.post { error("Exception in work") }
                advanceUntilIdle()
            }
        }
    }

    @Test
    @Requirement(
        given = "Dispatcher worker",
        `when` = "worker is disposed while a work is running",
        then = "the work is cancelled"
    )
    fun workIsCancelled() = runTest {
        var workStarted = false
        var workFinished = false

        val job = Job()
        val context = StandardTestDispatcher(testScheduler) + job
        val worker = DispatcherWorker(context)

        worker.post {
            launch(context) {
                workStarted = true
                delay(2000)
                workFinished = true
            }
        }

        runCurrent()
        worker.dispose()
        advanceUntilIdle()

        assertThat(workStarted).isTrue()
        assertThat(workFinished).isFalse()
    }

    @Test
    @Requirement(
        given = "Dispatcher worker that has been disposed",
        `when` = "a work is post",
        then = "the work is not processed"
    )
    fun workOnDisposedWorker() = runTest {
        var workStarted = false
        val worker = DispatcherWorker(StandardTestDispatcher(testScheduler))

        worker.dispose()
        worker.post { workStarted = true }
        advanceUntilIdle()

        assertThat(workStarted).isFalse()
    }
}
