package com.spotify.mobius.composable

import com.spotify.mobius.Effects.effects
import com.spotify.mobius.Next
import com.spotify.mobius.Next.*
import com.spotify.mobius.composable.ComposableUpdate.merge
import org.junit.Test
import org.quicktheories.QuickTheory.qt
import org.quicktheories.generators.Generate.constant
import org.quicktheories.generators.SourceDSL.lists

class ComposableUpdateMergeTest {

    private fun mergedUpdate(updates: List<ComposableUpdate<String, Int>>): Next<String, Effect<Int>> {
        val first = updates[0]
        val rest = updates.drop(1).toTypedArray()
        return merge(first, *rest).update("", 0)
    }

    private val noChangeUpdate = ComposableUpdate<String, Int> { _, _ -> noChange() }
    private val listsOfNoChangeUpdates = lists().of(constant(noChangeUpdate))

    private fun appendNumberUpdate(n: Int) = ComposableUpdate<String, Int> { m, _ -> next(m + n) }

    private fun emitEffectUpdate(f: Effect<Int>) = ComposableUpdate<String, Int> { _, _ -> dispatch(effects(f)) }

    private fun numberEffect(n: Int): Effect<Int> {
        return Effect.fromProducer { n }
    }

    @Test
    fun noChangeOnlyBecomesNoChange() {
        qt()
                .forAll(
                        listsOfNoChangeUpdates.ofSizeBetween(1, 3))

                .check { updates ->
                    mergedUpdate(updates) == noChange<String, Effect<Int>>()
                }
    }

    @Test
    fun singleRealUpdateModifiesModelRegardlessOfNoChanges() {
        qt()
                .forAll(
                        listsOfNoChangeUpdates.ofSizeBetween(0, 2),
                        listsOfNoChangeUpdates.ofSizeBetween(0, 2))

                .check { before, after ->
                    val updates = before +
                            appendNumberUpdate(1) +
                            after
                    mergedUpdate(updates) == next<String, Effect<Int>>("1")
                }
    }

    @Test
    fun twoRealUpdatesModifiesModelRegardlessOfNoChanges() {
        qt()
                .forAll(
                        listsOfNoChangeUpdates.ofSizeBetween(0, 2),
                        listsOfNoChangeUpdates.ofSizeBetween(0, 2),
                        listsOfNoChangeUpdates.ofSizeBetween(0, 2))

                .check { before, between, after ->
                    val updates = before +
                            appendNumberUpdate(1) +
                            between +
                            appendNumberUpdate(2) +
                            after

                    mergedUpdate(updates) == next<String, Effect<Int>>("12")
                }
    }

    @Test
    fun singleRealUpdateEmitsEffectsRegardlessOfNoChanges() {
        val first = numberEffect(1)

        qt()
                .forAll(
                        listsOfNoChangeUpdates.ofSizeBetween(0, 2),
                        listsOfNoChangeUpdates.ofSizeBetween(0, 2))

                .check { before, after ->
                    val updates = before +
                            emitEffectUpdate(first) +
                            after
                    mergedUpdate(updates) == dispatch<String, Effect<Int>>(effects(first))
                }
    }


    @Test
    fun twoRealUpdatesEmitsEffectsModelRegardlessOfNoChanges() {
        val first = numberEffect(1)
        val second = numberEffect(2)

        qt()
                .forAll(
                        listsOfNoChangeUpdates.ofSizeBetween(0, 2),
                        listsOfNoChangeUpdates.ofSizeBetween(0, 2),
                        listsOfNoChangeUpdates.ofSizeBetween(0, 2))

                .check { before, between, after ->
                    val updates = before +
                            emitEffectUpdate(first) +
                            between +
                            emitEffectUpdate(second) +
                            after

                    mergedUpdate(updates) == dispatch<String, Effect<Int>>(effects(first, second))
                }
    }

    @Test
    fun twoRealUpdatesCanModifyModelAndEmitEffectsModelRegardlessOfNoChanges() {
        val first = numberEffect(1)

        qt()
                .forAll(
                        listsOfNoChangeUpdates.ofSizeBetween(0, 2),
                        listsOfNoChangeUpdates.ofSizeBetween(0, 2),
                        listsOfNoChangeUpdates.ofSizeBetween(0, 2))

                .check { before, between, after ->
                    val updates = before +
                            emitEffectUpdate(first) +
                            between +
                            appendNumberUpdate(2) +
                            after

                    mergedUpdate(updates) == next("2", effects(first))
                }
    }
}
