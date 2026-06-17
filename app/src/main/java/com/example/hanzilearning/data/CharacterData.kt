package com.example.hanzilearning.data

data class CharacterData(
    val character: String,
    val pinyin: String,
    val meaning: String,          // 简短字义（1-2个词）
    val level: Int = 1,
    val strokeCount: Int = 0,
    val fullMeaning: String = ""   // 字典式完整释义
)
