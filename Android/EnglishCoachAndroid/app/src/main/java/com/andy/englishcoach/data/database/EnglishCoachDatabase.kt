package com.andy.englishcoach.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.andy.englishcoach.data.database.dao.VocabularyDao
import com.andy.englishcoach.data.database.dao.WordProgressDao
import com.andy.englishcoach.data.database.entity.VocabularyEntity
import com.andy.englishcoach.data.database.entity.WordProgressEntity

/**
 * Main Room Database for EnglishCoach Android.
 * Version 1: VocabularyEntity
 * Version 2: Added WordProgressEntity for learning progress and SM-2 spaced repetition
 */
@Database(
    entities = [
        VocabularyEntity::class,
        WordProgressEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class EnglishCoachDatabase : RoomDatabase() {

    abstract fun vocabularyDao(): VocabularyDao
    abstract fun wordProgressDao(): WordProgressDao

    companion object {
        const val DATABASE_NAME = "english_coach.db"

        /**
         * Migration from version 1 to 2: introduces word_progress table.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `word_progress` (
                        `word` TEXT NOT NULL,
                        `learned` INTEGER NOT NULL DEFAULT 0,
                        `learned_date` TEXT,
                        `correct_count` INTEGER NOT NULL DEFAULT 0,
                        `wrong_count` INTEGER NOT NULL DEFAULT 0,
                        `easiness_factor` REAL NOT NULL DEFAULT 2.5,
                        `interval_days` INTEGER NOT NULL DEFAULT 0,
                        `repetition_count` INTEGER NOT NULL DEFAULT 0,
                        `next_review_date` TEXT,
                        PRIMARY KEY(`word`),
                        FOREIGN KEY(`word`) REFERENCES `words`(`word`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_word_progress_word` ON `word_progress` (`word`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_word_progress_next_review_date` ON `word_progress` (`next_review_date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_word_progress_learned` ON `word_progress` (`learned`)")
            }
        }

        /**
         * List of all active migrations.
         */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        @Volatile
        private var INSTANCE: EnglishCoachDatabase? = null

        fun getInstance(context: Context): EnglishCoachDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        fun buildDatabase(context: Context): EnglishCoachDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                EnglishCoachDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(*ALL_MIGRATIONS)
                .build()
        }

        fun buildInMemoryDatabase(context: Context): EnglishCoachDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                EnglishCoachDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
