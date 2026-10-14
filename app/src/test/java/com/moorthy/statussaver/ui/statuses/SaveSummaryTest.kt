package com.moorthy.statussaver.ui.statuses

import com.moorthy.statussaver.data.model.SaveResult
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveSummaryTest {

    @Test
    fun countsEachOutcome() {
        val summary = SaveSummary() +
            SaveResult.SAVED +
            SaveResult.FAILED +
            SaveResult.SAVED +
            SaveResult.ALREADY_SAVED +
            SaveResult.SAVED

        assertEquals(SaveSummary(saved = 3, alreadySaved = 1, failed = 1), summary)
        assertEquals(5, summary.total)
    }

    @Test
    fun startsWithNothingCounted() {
        assertEquals(0, SaveSummary().total)
    }
}
