package com.andy.englishcoach.data

import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.model.Word
import com.andy.englishcoach.data.parser.VocabularyCsvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class VocabularyParserTest {

    private lateinit var words: List<Word>

    @Before
    fun setUp() {
        // Locate toeic_3600.csv from various possible working directories during test execution
        val possiblePaths = listOf(
            File("src/main/assets/toeic_3600.csv"),
            File("app/src/main/assets/toeic_3600.csv"),
            File("../app/src/main/assets/toeic_3600.csv")
        )
        val csvFile = possiblePaths.firstOrNull { it.exists() }
        assertNotNull("Could not find toeic_3600.csv in asset directories", csvFile)

        words = FileInputStream(csvFile!!).use { stream ->
            VocabularyCsvParser.parse(stream)
        }
    }

    @Test
    fun testExactly3600Words() {
        assertEquals("Total vocabulary count must be exactly 3,600", 3600, words.size)
    }

    @Test
    fun testDuplicateCountIsZero() {
        val wordNames = words.map { it.word }
        val duplicates = wordNames.groupBy { it }.filter { it.value.size > 1 }
        assertTrue("Duplicate words found: ${duplicates.keys}", duplicates.isEmpty())
        assertEquals("Unique word count must equal 3,600", 3600, wordNames.toSet().size)
    }

    @Test
    fun testEmptyRequiredFieldsCountIsZero() {
        var emptyFieldCount = 0
        for (w in words) {
            if (w.word.isBlank()) emptyFieldCount++
            if (w.phonetic.isBlank()) emptyFieldCount++
            if (w.translation.isBlank()) emptyFieldCount++
            if (w.example.isBlank()) emptyFieldCount++
            if (w.exampleTranslation.isBlank()) emptyFieldCount++
            if (w.level.isBlank()) emptyFieldCount++
            if (w.topic.isBlank()) emptyFieldCount++
            if (w.subtopic.isBlank()) emptyFieldCount++
            if (w.examTags.isBlank()) emptyFieldCount++
            if (w.partOfSpeech.isBlank()) emptyFieldCount++
        }
        assertEquals("Empty required fields count must be 0", 0, emptyFieldCount)
    }

    @Test
    fun testLevelDistribution() {
        val basicWords = words.filter { it.level == "toeic_basic" }
        val advancedWords = words.filter { it.level == "toeic_advanced" }
        val goldWords = words.filter { it.level == "toeic_gold" }

        assertEquals("Basic count must be 1,200", 1200, basicWords.size)
        assertEquals("Advanced count must be 1,400", 1400, advancedWords.size)
        assertEquals("Gold count must be 1,000", 1000, goldWords.size)

        val allDistinctLevels = words.map { it.level }.toSet()
        val expectedLevels = setOf("toeic_basic", "toeic_advanced", "toeic_gold")
        assertEquals("Levels must strictly contain only basic, advanced, gold", expectedLevels, allDistinctLevels)
    }

    @Test
    fun testDifficultyDistributionPerLevel() {
        for (w in words) {
            when (w.level) {
                "toeic_basic" -> {
                    assertTrue("Basic word '${w.word}' difficulty must be 1–2 (was ${w.difficulty})", w.difficulty in 1..2)
                }
                "toeic_advanced" -> {
                    assertTrue("Advanced word '${w.word}' difficulty must be 2–4 (was ${w.difficulty})", w.difficulty in 2..4)
                }
                "toeic_gold" -> {
                    assertTrue("Gold word '${w.word}' difficulty must be 3–5 (was ${w.difficulty})", w.difficulty in 3..5)
                }
            }
        }

        val diffDistribution = words.groupBy { it.difficulty }.mapValues { it.value.size }
        assertEquals(741, diffDistribution[1])
        assertEquals(897, diffDistribution[2])
        assertEquals(881, diffDistribution[3])
        assertEquals(669, diffDistribution[4])
        assertEquals(412, diffDistribution[5])
    }

    @Test
    fun testTargetIsolationUsesLevelNotDifficulty() {
        // Difficulty 2 spans both Basic and Advanced
        val basicDiff2 = words.filter { it.level == "toeic_basic" && it.difficulty == 2 }
        val advancedDiff2 = words.filter { it.level == "toeic_advanced" && it.difficulty == 2 }
        assertTrue("Difficulty 2 must exist in Basic", basicDiff2.isNotEmpty())
        assertTrue("Difficulty 2 must exist in Advanced", advancedDiff2.isNotEmpty())

        // Difficulty 3 spans both Advanced and Gold
        val advancedDiff3 = words.filter { it.level == "toeic_advanced" && it.difficulty == 3 }
        val goldDiff3 = words.filter { it.level == "toeic_gold" && it.difficulty == 3 }
        assertTrue("Difficulty 3 must exist in Advanced", advancedDiff3.isNotEmpty())
        assertTrue("Difficulty 3 must exist in Gold", goldDiff3.isNotEmpty())

        // Confirm level partitions the dataset into mutually exclusive subsets
        val basicSet = words.filter { it.target == ToeicTarget.BASIC }.map { it.word }.toSet()
        val advancedSet = words.filter { it.target == ToeicTarget.ADVANCED }.map { it.word }.toSet()
        val goldSet = words.filter { it.target == ToeicTarget.GOLD }.map { it.word }.toSet()

        assertTrue(basicSet.intersect(advancedSet).isEmpty())
        assertTrue(basicSet.intersect(goldSet).isEmpty())
        assertTrue(advancedSet.intersect(goldSet).isEmpty())
        assertEquals(3600, basicSet.size + advancedSet.size + goldSet.size)
    }

    @Test
    fun testIpaFormatIntegrity() {
        for (w in words) {
            assertTrue("Word '${w.word}' phonetic '${w.phonetic}' should start with '/'", w.phonetic.startsWith("/"))
            assertTrue("Word '${w.word}' phonetic '${w.phonetic}' should end with '/'", w.phonetic.endsWith("/"))
        }
    }

    @Test
    fun testTraditionalChineseMeaningIntegrity() {
        for (w in words) {
            assertFalse("Translation for '${w.word}' must not be empty", w.translation.isBlank())
            // Verify translation contains CJK ideographs (\u4e00-\u9fa5)
            val hasChineseChar = w.translation.any { it.code in 0x4E00..0x9FFF }
            assertTrue("Translation '${w.translation}' for '${w.word}' must contain Chinese characters", hasChineseChar)
        }
    }

    @Test
    fun testNoSensitiveOrRemovedWordsExist() {
        val removedWords = setOf(
            "genocide", "insurgent", "weaponry", "terrorism", "abortion",
            "kidnap", "self-defense", "fridays", "jumped", "leaves", "hello", "okay",
            "terrorist"
        )
        val wordSet = words.map { it.word.lowercase() }.toSet()
        for (rw in removedWords) {
            assertFalse("Removed sensitive word '$rw' must not be in the vocabulary", wordSet.contains(rw))
        }
    }
}
