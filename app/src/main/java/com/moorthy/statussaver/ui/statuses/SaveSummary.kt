package com.moorthy.statussaver.ui.statuses

import com.moorthy.statussaver.data.model.SaveResult

/**
 * How a save of one or more statuses ended, counted by outcome (FR4). The user sees one
 * message per save action, however many statuses it covered.
 */
data class SaveSummary(
    val saved: Int = 0,
    val alreadySaved: Int = 0,
    val failed: Int = 0,
) {
    val total: Int
        get() = saved + alreadySaved + failed

    /** This summary with one more status counted under [result]. */
    operator fun plus(result: SaveResult): SaveSummary = when (result) {
        SaveResult.SAVED -> copy(saved = saved + 1)
        SaveResult.ALREADY_SAVED -> copy(alreadySaved = alreadySaved + 1)
        SaveResult.FAILED -> copy(failed = failed + 1)
    }
}
