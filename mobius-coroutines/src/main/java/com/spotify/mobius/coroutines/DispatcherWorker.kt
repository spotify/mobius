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
