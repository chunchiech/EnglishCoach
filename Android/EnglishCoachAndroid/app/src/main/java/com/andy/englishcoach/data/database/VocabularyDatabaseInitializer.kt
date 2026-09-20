package com.andy.englishcoach.data.database

import android.content.Context
import com.andy.englishcoach.data.database.entity.VocabularyEntity
import com.andy.englishcoach.data.parser.VocabularyCsvParser
import java.io.InputStream

/**
 * Initializes the Room database from the official TOEIC CSV resource.
 * Guarantees idempotence, transaction atomicity, rollback on failure, and zero duplicate records.
 */
object VocabularyDatabaseInitializer {

    const val ASSET_FILE_NAME = "toeic_3600.csv"
    const val EXPECTED_WORD_COUNT = 3600

    /**
     * Initializes the database from assets/toeic_3600.csv if the database does not already contain words.
     * @return true if initialization took place, false if already populated.
     */
    fun initializeIfNeeded(context: Context, database: EnglishCoachDatabase): Boolean {
        val dao = database.vocabularyDao()
        val currentCount = dao.countWords()

        if (currentCount >= EXPECTED_WORD_COUNT) {
            return false
        }

        context.assets.open(ASSET_FILE_NAME).use { stream ->
            populateFromStream(database, stream)
        }
        return true
    }

    /**
     * Parses the given [inputStream] and inserts entities within a single database transaction.
     * If an exception occurs, the transaction automatically rolls back.
     */
    fun populateFromStream(database: EnglishCoachDatabase, inputStream: InputStream) {
        val words = VocabularyCsvParser.parse(inputStream)
        val entities = words.map { w ->
            VocabularyEntity(
                word = w.word,
                phonetic = w.phonetic,
                translation = w.translation,
                example = w.example,
                exampleTranslation = w.exampleTranslation,
                level = w.level,
                difficulty = w.difficulty,
                topic = w.topic,
                subtopic = w.subtopic,
                examTags = w.examTags,
                partOfSpeech = w.partOfSpeech
            )
        }

        database.runInTransaction {
            val dao = database.vocabularyDao()
            dao.insertAll(entities)
        }
    }
}
