package com.andy.englishcoach.data.parser

import com.andy.englishcoach.data.model.Word
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader

/**
 * Robust CSV parser for EnglishCoach official TOEIC vocabulary.
 */
object VocabularyCsvParser {

    /**
     * Parses the TOEIC CSV stream into a list of [Word] objects.
     */
    fun parse(inputStream: InputStream): List<Word> {
        return parse(InputStreamReader(inputStream, Charsets.UTF_8))
    }

    /**
     * Parses the TOEIC CSV reader into a list of [Word] objects.
     */
    fun parse(reader: Reader): List<Word> {
        val bufferedReader = if (reader is BufferedReader) reader else BufferedReader(reader)
        val words = mutableListOf<Word>()

        bufferedReader.useLines { lines ->
            var isHeader = true
            for (line in lines) {
                if (isHeader) {
                    isHeader = false
                    continue
                }
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                val fields = parseCSVRow(trimmed)
                if (fields.size >= 11) {
                    words.add(
                        Word(
                            word = fields[0],
                            phonetic = fields[1],
                            translation = fields[2],
                            example = fields[3],
                            exampleTranslation = fields[4],
                            level = fields[5],
                            difficulty = fields[6].toIntOrNull() ?: 1,
                            topic = fields[7],
                            subtopic = fields[8],
                            examTags = fields[9],
                            partOfSpeech = fields[10]
                        )
                    )
                }
            }
        }
        return words
    }

    /**
     * Parses a single CSV line with quote-handling compliant with RFC 4180 and iOS parser.
     */
    fun parseCSVRow(line: String): List<String> {
        val result = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val char = line[i]
            when {
                char == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        currentField.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    result.add(currentField.toString().trim())
                    currentField.setLength(0)
                }
                else -> {
                    currentField.append(char)
                }
            }
            i++
        }
        result.add(currentField.toString().trim())
        return result
    }
}
