package com.andy.englishcoach.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.andy.englishcoach.data.model.ToeicTarget
import com.andy.englishcoach.data.model.Word

/**
 * Room entity representing a vocabulary record in the local database.
 * Matches all 11 columns from the official TOEIC 3,600 CSV with a stable unique primary key.
 */
@Entity(
    tableName = "words",
    indices = [
        Index(value = ["word"], unique = true),
        Index(value = ["level"])
    ]
)
data class VocabularyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "word")
    val word: String,

    @ColumnInfo(name = "phonetic")
    val phonetic: String,

    @ColumnInfo(name = "translation")
    val translation: String,

    @ColumnInfo(name = "example")
    val example: String,

    @ColumnInfo(name = "example_translation")
    val exampleTranslation: String,

    @ColumnInfo(name = "level")
    val level: String,

    @ColumnInfo(name = "difficulty")
    val difficulty: Int,

    @ColumnInfo(name = "topic")
    val topic: String,

    @ColumnInfo(name = "subtopic")
    val subtopic: String,

    @ColumnInfo(name = "exam_tags")
    val examTags: String,

    @ColumnInfo(name = "part_of_speech")
    val partOfSpeech: String
) {
    val target: ToeicTarget
        get() = ToeicTarget.from(level)

    fun toWord(): Word {
        return Word(
            word = word,
            phonetic = phonetic,
            translation = translation,
            example = example,
            exampleTranslation = exampleTranslation,
            level = level,
            difficulty = difficulty,
            topic = topic,
            subtopic = subtopic,
            examTags = examTags,
            partOfSpeech = partOfSpeech
        )
    }

    companion object {
        fun fromWord(word: Word, id: Long = 0): VocabularyEntity {
            return VocabularyEntity(
                id = id,
                word = word.word,
                phonetic = word.phonetic,
                translation = word.translation,
                example = word.example,
                exampleTranslation = word.exampleTranslation,
                level = word.level,
                difficulty = word.difficulty,
                topic = word.topic,
                subtopic = word.subtopic,
                examTags = word.examTags,
                partOfSpeech = word.partOfSpeech
            )
        }
    }
}
