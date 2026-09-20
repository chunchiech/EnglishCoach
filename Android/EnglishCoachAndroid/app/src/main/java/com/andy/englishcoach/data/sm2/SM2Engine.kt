package com.andy.englishcoach.data.sm2

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.round

/**
 * State representing SM-2 parameters for a word.
 */
data class SM2State(
    val easinessFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitionCount: Int = 0,
    val nextReviewDate: String? = null
) {
    companion object {
        val INITIAL = SM2State()
    }
}

/**
 * Calculation result containing updated SM-2 parameters.
 */
data class SM2Result(
    val easinessFactor: Double,
    val intervalDays: Int,
    val repetitionCount: Int,
    val nextReviewDate: String
)

/**
 * Pure Kotlin implementation of the SuperMemo 2 (SM-2) spaced repetition algorithm.
 * 100% parity with EnglishCoach iOS implementation in DatabaseManager.swift:
 * - isCorrect == true  -> q = 4
 * - isCorrect == false -> q = 1
 * - q < 3 resets interval to 1 and repetition count to 0
 * - q >= 3 sets interval to 1 (rep 0), 6 (rep 1), round(interval * EF) (rep >= 2), and increments repCount
 * - EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)), clamped to minimum 1.3
 * - nextReviewDate = reviewDate + interval days (formatted as yyyy-MM-dd)
 */
object SM2Engine {

    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    const val MIN_EASINESS_FACTOR: Double = 1.3
    const val DEFAULT_EASINESS_FACTOR: Double = 2.5

    /**
     * Updates SM-2 state based on boolean correctness (matching iOS updateSM2).
     */
    fun calculate(
        currentState: SM2State,
        isCorrect: Boolean,
        reviewDate: LocalDate = LocalDate.now()
    ): SM2Result {
        val q = if (isCorrect) 4 else 1
        return calculate(currentState, q, reviewDate)
    }

    /**
     * Updates SM-2 state based on raw quality rating (0..5).
     */
    fun calculate(
        currentState: SM2State,
        quality: Int,
        reviewDate: LocalDate = LocalDate.now()
    ): SM2Result {
        var repCount = currentState.repetitionCount
        var interval = currentState.intervalDays
        var ef = currentState.easinessFactor

        if (quality < 3) {
            repCount = 0
            interval = 1
        } else {
            interval = when (repCount) {
                0 -> 1
                1 -> 6
                else -> round(interval.toDouble() * ef).toInt()
            }
            repCount += 1
        }

        val qDouble = quality.toDouble()
        ef = ef + (0.1 - (5.0 - qDouble) * (0.08 + (5.0 - qDouble) * 0.02))
        if (ef < MIN_EASINESS_FACTOR) {
            ef = MIN_EASINESS_FACTOR
        }

        val nextDate = reviewDate.plusDays(interval.toLong()).format(DATE_FORMATTER)

        return SM2Result(
            easinessFactor = ef,
            intervalDays = interval,
            repetitionCount = repCount,
            nextReviewDate = nextDate
        )
    }
}
