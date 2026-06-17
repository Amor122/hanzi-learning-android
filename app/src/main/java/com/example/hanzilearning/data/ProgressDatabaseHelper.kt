package com.example.hanzilearning.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LearningProgress(
    val character: String,
    val pinyinScore: Int = 0,
    val strokeScore: Int = 0,
    val gameScore: Int = 0,
    val practiceCount: Int = 0,
    val lastPractice: Long = 0L,
    val mastered: Boolean = false
) {
    val totalScore: Int get() = pinyinScore + strokeScore + gameScore
}

data class DailyStats(
    val date: String,
    val charactersLearned: Int,
    val totalPractices: Int,
    val totalScore: Int
)

class ProgressDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "hanzi_progress.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_PROGRESS = "progress"
        private const val TABLE_DAILY = "daily_stats"

        private const val KEY_CHAR = "character"
        private const val KEY_PINYIN_SCORE = "pinyin_score"
        private const val KEY_STROKE_SCORE = "stroke_score"
        private const val KEY_GAME_SCORE = "game_score"
        private const val KEY_PRACTICE_COUNT = "practice_count"
        private const val KEY_LAST_PRACTICE = "last_practice"
        private const val KEY_MASTERED = "mastered"

        private const val KEY_DATE = "date"
        private const val KEY_LEARNED = "chars_learned"
        private const val KEY_TOTAL_PRACTICE = "total_practices"
        private const val KEY_TOTAL_SCORE = "total_score"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createProgressTable = """
            CREATE TABLE $TABLE_PROGRESS (
                $KEY_CHAR TEXT PRIMARY KEY,
                $KEY_PINYIN_SCORE INTEGER DEFAULT 0,
                $KEY_STROKE_SCORE INTEGER DEFAULT 0,
                $KEY_GAME_SCORE INTEGER DEFAULT 0,
                $KEY_PRACTICE_COUNT INTEGER DEFAULT 0,
                $KEY_LAST_PRACTICE INTEGER DEFAULT 0,
                $KEY_MASTERED INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createProgressTable)

        val createDailyTable = """
            CREATE TABLE $TABLE_DAILY (
                $KEY_DATE TEXT PRIMARY KEY,
                $KEY_LEARNED INTEGER DEFAULT 0,
                $KEY_TOTAL_PRACTICE INTEGER DEFAULT 0,
                $KEY_TOTAL_SCORE INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createDailyTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PROGRESS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DAILY")
        onCreate(db)
    }

    // 更新字的进度
    fun updateProgress(
        character: String,
        pinyinDelta: Int = 0,
        strokeDelta: Int = 0,
        gameDelta: Int = 0,
        incrementPractice: Boolean = true
    ) {
        val db = writableDatabase
        val existing = getProgress(character)

        val values = ContentValues().apply {
            put(KEY_CHAR, character)
            if (existing != null) {
                put(KEY_PINYIN_SCORE, (existing.pinyinScore + pinyinDelta).coerceIn(0, 100))
                put(KEY_STROKE_SCORE, (existing.strokeScore + strokeDelta).coerceIn(0, 100))
                put(KEY_GAME_SCORE, (existing.gameScore + gameDelta).coerceIn(0, 100))
                val newCount = existing.practiceCount + if (incrementPractice) 1 else 0
                put(KEY_PRACTICE_COUNT, newCount)
                val mastered = (existing.pinyinScore + pinyinDelta >= 80 &&
                        existing.strokeScore + strokeDelta >= 60 &&
                        newCount >= 3)
                put(KEY_MASTERED, if (mastered) 1 else 0)
            } else {
                put(KEY_PINYIN_SCORE, pinyinDelta.coerceIn(0, 100))
                put(KEY_STROKE_SCORE, strokeDelta.coerceIn(0, 100))
                put(KEY_GAME_SCORE, gameDelta.coerceIn(0, 100))
                put(KEY_PRACTICE_COUNT, if (incrementPractice) 1 else 0)
                put(KEY_MASTERED, 0)
            }
            put(KEY_LAST_PRACTICE, System.currentTimeMillis())
        }

        db.insertWithOnConflict(TABLE_PROGRESS, null, values, SQLiteDatabase.CONFLICT_REPLACE)

        // 更新每日统计
        updateDailyStats(character, pinyinDelta + strokeDelta + gameDelta)
    }

    private fun updateDailyStats(character: String, scoreDelta: Int) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        val db = writableDatabase

        val cursor = db.query(
            TABLE_DAILY,
            null,
            "$KEY_DATE = ?",
            arrayOf(today),
            null, null, null
        )

        val values = if (cursor.moveToFirst()) {
            val learned = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_LEARNED))
            val practices = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TOTAL_PRACTICE))
            val totalScore = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TOTAL_SCORE))

            // 检查是否为新学的字
            val charProgress = getProgress(character)
            val isNewChar = charProgress == null || charProgress.practiceCount <= 1
            ContentValues().apply {
                put(KEY_DATE, today)
                put(KEY_LEARNED, learned + if (isNewChar) 1 else 0)
                put(KEY_TOTAL_PRACTICE, practices + 1)
                put(KEY_TOTAL_SCORE, totalScore + scoreDelta)
            }
        } else {
            ContentValues().apply {
                put(KEY_DATE, today)
                put(KEY_LEARNED, 1)
                put(KEY_TOTAL_PRACTICE, 1)
                put(KEY_TOTAL_SCORE, scoreDelta)
            }
        }
        cursor.close()

        db.insertWithOnConflict(TABLE_DAILY, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getProgress(character: String): LearningProgress? {
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_PROGRESS,
            null,
            "$KEY_CHAR = ?",
            arrayOf(character),
            null, null, null
        )

        var result: LearningProgress? = null
        if (cursor.moveToFirst()) {
            result = LearningProgress(
                character = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CHAR)),
                pinyinScore = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PINYIN_SCORE)),
                strokeScore = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_STROKE_SCORE)),
                gameScore = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_GAME_SCORE)),
                practiceCount = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PRACTICE_COUNT)),
                lastPractice = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_LAST_PRACTICE)),
                mastered = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MASTERED)) == 1
            )
        }
        cursor.close()
        return result
    }

    fun getAllProgress(): List<LearningProgress> {
        val db = readableDatabase
        val cursor: Cursor = db.query(TABLE_PROGRESS, null, null, null, null, null, null)
        val result = mutableListOf<LearningProgress>()

        while (cursor.moveToNext()) {
            result.add(
                LearningProgress(
                    character = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CHAR)),
                    pinyinScore = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PINYIN_SCORE)),
                    strokeScore = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_STROKE_SCORE)),
                    gameScore = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_GAME_SCORE)),
                    practiceCount = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PRACTICE_COUNT)),
                    lastPractice = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_LAST_PRACTICE)),
                    mastered = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MASTERED)) == 1
                )
            )
        }
        cursor.close()
        return result
    }

    fun getMasteredCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_PROGRESS WHERE $KEY_MASTERED = 1",
            null
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    fun getTotalPracticeCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM($KEY_PRACTICE_COUNT) FROM $TABLE_PROGRESS",
            null
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    fun getTodayStats(): DailyStats {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DAILY,
            null,
            "$KEY_DATE = ?",
            arrayOf(today),
            null, null, null
        )

        val stats = if (cursor.moveToFirst()) {
            DailyStats(
                date = today,
                charactersLearned = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_LEARNED)),
                totalPractices = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TOTAL_PRACTICE)),
                totalScore = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TOTAL_SCORE))
            )
        } else {
            DailyStats(today, 0, 0, 0)
        }
        cursor.close()
        return stats
    }

    fun getWeeklyStats(): List<DailyStats> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Date()
        val dates = (0 until 7).map { offset ->
            val date = Date(today.time - (6 - offset) * 24L * 60L * 60L * 1000L)
            dateFormat.format(date)
        }

        val result = mutableListOf<DailyStats>()
        val db = readableDatabase
        for (date in dates) {
            val cursor = db.query(
                TABLE_DAILY,
                null,
                "$KEY_DATE = ?",
                arrayOf(date),
                null, null, null
            )
            if (cursor.moveToFirst()) {
                result.add(
                    DailyStats(
                        date = date,
                        charactersLearned = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_LEARNED)),
                        totalPractices = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TOTAL_PRACTICE)),
                        totalScore = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TOTAL_SCORE))
                    )
                )
            } else {
                result.add(DailyStats(date, 0, 0, 0))
            }
            cursor.close()
        }
        return result
    }

    fun getRecentCharacters(limit: Int = 10): List<LearningProgress> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PROGRESS,
            null,
            null,
            null,
            null,
            null,
            "$KEY_LAST_PRACTICE DESC",
            limit.toString()
        )
        val result = mutableListOf<LearningProgress>()
        while (cursor.moveToNext()) {
            result.add(
                LearningProgress(
                    character = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CHAR)),
                    pinyinScore = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PINYIN_SCORE)),
                    strokeScore = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_STROKE_SCORE)),
                    gameScore = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_GAME_SCORE)),
                    practiceCount = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PRACTICE_COUNT)),
                    lastPractice = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_LAST_PRACTICE)),
                    mastered = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MASTERED)) == 1
                )
            )
        }
        cursor.close()
        return result
    }

    fun clearAllProgress() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_PROGRESS")
        db.execSQL("DELETE FROM $TABLE_DAILY")
    }
}
