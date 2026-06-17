package com.example.hanzilearning.data

data class CharacterData(
    val character: String,
    val pinyin: String,
    val meaning: String,
    val level: Int = 1,
    val strokeCount: Int = 0
)
