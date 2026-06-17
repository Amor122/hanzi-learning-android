package com.example.hanzilearning.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hanzi_user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CURRENT_LEVEL = "current_level"
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_CURRENT_INDEX = "current_index"
        private const val KEY_FAVORITE_CHARS = "favorite_chars"
        private const val KEY_HIGH_SCORE_LISTEN = "high_score_listen"
        private const val KEY_HIGH_SCORE_PICK = "high_score_pick"
        private const val KEY_HIGH_SCORE_STROKE = "high_score_stroke"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
    }

    var currentLevel: Int
        get() = prefs.getInt(KEY_CURRENT_LEVEL, 1)
        set(value) = prefs.edit().putInt(KEY_CURRENT_LEVEL, value.coerceIn(1, 5)).apply()

    var dailyGoal: Int
        get() = prefs.getInt(KEY_DAILY_GOAL, 10)
        set(value) = prefs.edit().putInt(KEY_DAILY_GOAL, value.coerceIn(1, 100)).apply()

    var currentIndex: Int
        get() = prefs.getInt(KEY_CURRENT_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_CURRENT_INDEX, value.coerceAtLeast(0)).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_FIRST_LAUNCH, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var highScoreListen: Int
        get() = prefs.getInt(KEY_HIGH_SCORE_LISTEN, 0)
        set(value) = prefs.edit().putInt(KEY_HIGH_SCORE_LISTEN, value).apply()

    var highScorePick: Int
        get() = prefs.getInt(KEY_HIGH_SCORE_PICK, 0)
        set(value) = prefs.edit().putInt(KEY_HIGH_SCORE_PICK, value).apply()

    var highScoreStroke: Int
        get() = prefs.getInt(KEY_HIGH_SCORE_STROKE, 0)
        set(value) = prefs.edit().putInt(KEY_HIGH_SCORE_STROKE, value).apply()

    // 收藏的字
    fun getFavorites(): List<String> {
        val json = prefs.getString(KEY_FAVORITE_CHARS, null)
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addFavorite(char: String) {
        val current = getFavorites().toMutableList()
        if (!current.contains(char)) {
            current.add(char)
            saveFavorites(current)
        }
    }

    fun removeFavorite(char: String) {
        val current = getFavorites().toMutableList()
        current.remove(char)
        saveFavorites(current)
    }

    fun isFavorite(char: String): Boolean {
        return getFavorites().contains(char)
    }

    fun toggleFavorite(char: String): Boolean {
        return if (isFavorite(char)) {
            removeFavorite(char)
            false
        } else {
            addFavorite(char)
            true
        }
    }

    private fun saveFavorites(chars: List<String>) {
        val json = JSONArray(chars)
        prefs.edit().putString(KEY_FAVORITE_CHARS, json.toString()).apply()
    }

    // 更新最高分（如果超过则更新）
    fun updateHighScoreListen(score: Int): Boolean {
        if (score > highScoreListen) {
            highScoreListen = score
            return true
        }
        return false
    }

    fun updateHighScorePick(score: Int): Boolean {
        if (score > highScorePick) {
            highScorePick = score
            return true
        }
        return false
    }

    fun updateHighScoreStroke(score: Int): Boolean {
        if (score > highScoreStroke) {
            highScoreStroke = score
            return true
        }
        return false
    }

    fun resetAll() {
        prefs.edit().clear().apply()
    }
}
