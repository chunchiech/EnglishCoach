package com.andy.englishcoach.data.sm2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SM2EngineTest {

    private val testDate: LocalDate = LocalDate.of(2026, 9, 21)

    @Test
    fun testFirstReviewCorrect() {
        val initial = SM2State(easinessFactor = 2.5, intervalDays = 0, repetitionCount = 0)
        val result = SM2Engine.calculate(initial, isCorrect = true, reviewDate = testDate)

        assertEquals("First correct review must have interval = 1", 1, result.intervalDays)
        assertEquals("First correct review must increment repetition to 1", 1, result.repetitionCount)
        assertEquals(2.5, result.easinessFactor, 0.0001)
        assertEquals("2026-09-22", result.nextReviewDate)
    }

    @Test
    fun testFirstReviewWrong() {
        val initial = SM2State(easinessFactor = 2.5, intervalDays = 0, repetitionCount = 0)
        val result = SM2Engine.calculate(initial, isCorrect = false, reviewDate = testDate)

        assertEquals("Wrong review must reset interval to 1", 1, result.intervalDays)
        assertEquals("Wrong review must reset repetition count to 0", 0, result.repetitionCount)
        assertEquals(1.96, result.easinessFactor, 0.0001)
        assertEquals("2026-09-22", result.nextReviewDate)
    }

    @Test
    fun testRepeatedCorrectAnswersMatchesIosProgression() {
        var state = SM2State(easinessFactor = 2.5, intervalDays = 0, repetitionCount = 0)

        // Step 1: Correct (repCount 0 -> 1, interval 0 -> 1)
        var res = SM2Engine.calculate(state, isCorrect = true, reviewDate = testDate)
        assertEquals(1, res.intervalDays)
        assertEquals(1, res.repetitionCount)
        assertEquals(2.5, res.easinessFactor, 0.0001)
        assertEquals("2026-09-22", res.nextReviewDate)
        state = SM2State(res.easinessFactor, res.intervalDays, res.repetitionCount, res.nextReviewDate)

        // Step 2: Correct (repCount 1 -> 2, interval 1 -> 6)
        res = SM2Engine.calculate(state, isCorrect = true, reviewDate = testDate)
        assertEquals(6, res.intervalDays)
        assertEquals(2, res.repetitionCount)
        assertEquals(2.5, res.easinessFactor, 0.0001)
        assertEquals("2026-09-27", res.nextReviewDate)
        state = SM2State(res.easinessFactor, res.intervalDays, res.repetitionCount, res.nextReviewDate)

        // Step 3: Correct (repCount 2 -> 3, interval 6 -> round(6 * 2.5) = 15)
        res = SM2Engine.calculate(state, isCorrect = true, reviewDate = testDate)
        assertEquals(15, res.intervalDays)
        assertEquals(3, res.repetitionCount)
        assertEquals(2.5, res.easinessFactor, 0.0001)
        assertEquals("2026-10-06", res.nextReviewDate)
        state = SM2State(res.easinessFactor, res.intervalDays, res.repetitionCount, res.nextReviewDate)

        // Step 4: Correct (repCount 3 -> 4, interval 15 -> round(15 * 2.5) = 38)
        res = SM2Engine.calculate(state, isCorrect = true, reviewDate = testDate)
        assertEquals(38, res.intervalDays)
        assertEquals(4, res.repetitionCount)
        assertEquals(2.5, res.easinessFactor, 0.0001)
        assertEquals("2026-10-29", res.nextReviewDate)
        state = SM2State(res.easinessFactor, res.intervalDays, res.repetitionCount, res.nextReviewDate)

        // Step 5: Correct (repCount 4 -> 5, interval 38 -> round(38 * 2.5) = 95)
        res = SM2Engine.calculate(state, isCorrect = true, reviewDate = testDate)
        assertEquals(95, res.intervalDays)
        assertEquals(5, res.repetitionCount)
        assertEquals(2.5, res.easinessFactor, 0.0001)
        assertEquals("2026-12-25", res.nextReviewDate)
        state = SM2State(res.easinessFactor, res.intervalDays, res.repetitionCount, res.nextReviewDate)

        // Step 6: Correct (repCount 5 -> 6, interval 95 -> round(95 * 2.5) = 238)
        res = SM2Engine.calculate(state, isCorrect = true, reviewDate = testDate)
        assertEquals(238, res.intervalDays)
        assertEquals(6, res.repetitionCount)
        assertEquals(2.5, res.easinessFactor, 0.0001)
        assertEquals("2027-05-17", res.nextReviewDate)
    }

    @Test
    fun testRepeatedWrongAnswersAndFloorClamping() {
        var state = SM2State(easinessFactor = 2.5, intervalDays = 15, repetitionCount = 3)

        // Wrong 1: EF drops 2.5 - 0.54 = 1.96, interval = 1, repCount = 0
        var res = SM2Engine.calculate(state, isCorrect = false, reviewDate = testDate)
        assertEquals(1, res.intervalDays)
        assertEquals(0, res.repetitionCount)
        assertEquals(1.96, res.easinessFactor, 0.0001)
        state = SM2State(res.easinessFactor, res.intervalDays, res.repetitionCount, res.nextReviewDate)

        // Wrong 2: EF drops 1.96 - 0.54 = 1.42, interval = 1, repCount = 0
        res = SM2Engine.calculate(state, isCorrect = false, reviewDate = testDate)
        assertEquals(1, res.intervalDays)
        assertEquals(0, res.repetitionCount)
        assertEquals(1.42, res.easinessFactor, 0.0001)
        state = SM2State(res.easinessFactor, res.intervalDays, res.repetitionCount, res.nextReviewDate)

        // Wrong 3: EF drops 1.42 - 0.54 = 0.88 -> clamped to 1.3
        res = SM2Engine.calculate(state, isCorrect = false, reviewDate = testDate)
        assertEquals(1, res.intervalDays)
        assertEquals(0, res.repetitionCount)
        assertEquals(1.3, res.easinessFactor, 0.0001)
        state = SM2State(res.easinessFactor, res.intervalDays, res.repetitionCount, res.nextReviewDate)

        // Wrong 4: EF clamped at 1.3
        res = SM2Engine.calculate(state, isCorrect = false, reviewDate = testDate)
        assertEquals(1, res.intervalDays)
        assertEquals(0, res.repetitionCount)
        assertEquals(1.3, res.easinessFactor, 0.0001)
    }

    @Test
    fun testIosParityTable() {
        data class ParityVector(
            val initialEF: Double,
            val initialInterval: Int,
            val initialRep: Int,
            val isCorrect: Boolean,
            val expectedEF: Double,
            val expectedInterval: Int,
            val expectedRep: Int,
            val expectedDate: String
        )

        val testVectors = listOf(
            // Initial new word
            ParityVector(2.5, 0, 0, true, 2.5, 1, 1, "2026-09-22"),
            ParityVector(2.5, 0, 0, false, 1.96, 1, 0, "2026-09-22"),
            // Repetition 1 word
            ParityVector(2.5, 1, 1, true, 2.5, 6, 2, "2026-09-27"),
            ParityVector(2.5, 1, 1, false, 1.96, 1, 0, "2026-09-22"),
            // Repetition 2 word
            ParityVector(2.5, 6, 2, true, 2.5, 15, 3, "2026-10-06"),
            ParityVector(2.5, 6, 2, false, 1.96, 1, 0, "2026-09-22"),
            // Minimum EF edge cases (EF = 1.3)
            ParityVector(1.3, 1, 0, true, 1.3, 1, 1, "2026-09-22"),
            ParityVector(1.3, 1, 1, true, 1.3, 6, 2, "2026-09-27"),
            ParityVector(1.3, 6, 2, true, 1.3, 8, 3, "2026-09-29"), // round(6 * 1.3) = 8
            ParityVector(1.3, 8, 3, true, 1.3, 10, 4, "2026-10-01"), // round(8 * 1.3) = 10
            ParityVector(1.3, 8, 3, false, 1.3, 1, 0, "2026-09-22"), // clamped at 1.3
            // Higher EF edge cases (EF = 3.0)
            ParityVector(3.0, 6, 2, true, 3.0, 18, 3, "2026-10-09"), // round(6 * 3.0) = 18
            ParityVector(3.0, 6, 2, false, 2.46, 1, 0, "2026-09-22")
        )

        for ((index, v) in testVectors.withIndex()) {
            val input = SM2State(v.initialEF, v.initialInterval, v.initialRep)
            val output = SM2Engine.calculate(input, v.isCorrect, reviewDate = testDate)

            assertEquals("Vector $index: EF mismatch", v.expectedEF, output.easinessFactor, 0.0001)
            assertEquals("Vector $index: Interval mismatch", v.expectedInterval, output.intervalDays)
            assertEquals("Vector $index: Repetition mismatch", v.expectedRep, output.repetitionCount)
            assertEquals("Vector $index: Date mismatch", v.expectedDate, output.nextReviewDate)
        }
    }
}
