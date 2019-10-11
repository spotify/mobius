package com.spotify.mobius.composable

import com.spotify.mobius.Effects.effects
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.composable.Lens.lens
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.test.NextMatchers.hasModel
import com.spotify.mobius.test.NextMatchers.hasNothing
import junit.framework.Assert.assertEquals
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class ComposableUpdatePullbackTest {

    fun numberEffect(n: Int) = Effect.fromProducer { n }

    private val innerUpdateModelChange = ComposableUpdate<String, Int> { m, _ -> next(m.toUpperCase()) }
    private val innerUpdateDispatchEffects = ComposableUpdate<String, Int> { _, e -> dispatch(effects(numberEffect(e+1))) }

    private val lens = lens<Model, String>(
            { it.innerModel },
            { model, innerModel -> model.copy(innerModel = innerModel) })

    private val prism = Prism.prism<String, Int>(
            { it.toIntOrNull() },
            { it.toString() }
    )

    data class Model(val innerModel: String, val anotherThing: Int)

    @Test
    fun pullbackUpdatesOuterModel() {
        val pullback = ComposableUpdate.pullback(lens, prism, innerUpdateModelChange)

        val result = pullback
                .update(Model("Hello", 0), "1")

        assertThat(result, hasModel(Model("HELLO", 0)))
    }

    @Test
    fun pullbackDoesNotUpdateModelIfIncompatibleEvent() {
        val pullback = ComposableUpdate.pullback(lens, prism, innerUpdateModelChange)

        val result = pullback
                .update(Model("Hello", 0), "Foo")

        assertThat(result, hasNothing())
    }

    @Test
    fun pullbackWrapsInnerEffects() {
        val pullback = ComposableUpdate.pullback(lens, prism, innerUpdateDispatchEffects)

        val result = pullback.update(Model("Hello", 0), "2")

        val holder = mutableListOf<String>()

        for (effect in result.effects()) {

            effect.execute(object : Effect.Callback<String> {
                override fun onStart(disposable: Disposable?) {}
                override fun onComplete() {}

                override fun onValue(value: String?) {
                    holder += value ?: "!"
                }
            })
        }

        assertEquals(holder, listOf("3"))
    }
}
