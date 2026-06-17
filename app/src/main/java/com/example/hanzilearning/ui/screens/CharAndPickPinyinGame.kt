package com.example.hanzilearning.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hanzilearning.data.*
import com.example.hanzilearning.ui.*
import com.example.hanzilearning.util.*

@Composable
fun CharAndPickPinyinGame(
    onBack: () -> Unit,
    ttsHelper: TextToSpeechHelper,
    userPrefs: UserPreferences,
    progressDb: ProgressDatabaseHelper,
    showToast: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    // 按当前等级获取汉字（从设置中读取）
    val selectedLevel = userPrefs.currentLevel
    
    // 计算温故知新的混合比例
    val gameChars = remember(selectedLevel) {
        val currentLevel = CommonCharacters.getByLevel(selectedLevel)
        val reviewChars = when (selectedLevel) {
            1 -> emptyList()
            2 -> CommonCharacters.getByLevel(1)
            3 -> (1..2).flatMap { CommonCharacters.getByLevel(it) }
            4 -> (1..3).flatMap { CommonCharacters.getByLevel(it) }
            5 -> (1..4).flatMap { CommonCharacters.getByLevel(it) }
            else -> emptyList()
        }
        val ratio = when (selectedLevel) {
            1, 2 -> 1.0
            3 -> 0.8
            4 -> 0.7
            5 -> 0.6
            else -> 1.0
        }
        val mainCount = (currentLevel.size * ratio).toInt()
        val reviewCount = (currentLevel.size * (1 - ratio)).toInt()
        (currentLevel.shuffled().take(mainCount) + reviewChars.shuffled().take(reviewCount)).shuffled()
    }
    
    // 获取当前等级的字
    val currentLevelChars = remember(selectedLevel) { CommonCharacters.getByLevel(selectedLevel) }

    var currentRound by remember { mutableIntStateOf(0) }
    val totalRounds = 10
    var score by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }

    var targetCharData by remember { mutableStateOf<CharacterData?>(null) }
    var options by remember { mutableStateOf(listOf("", "", "", "")) }
    var gameEnded by remember { mutableStateOf(false) }

    fun generateQuestion() {
        if (gameChars.isEmpty()) return
        val target = gameChars.random()
        targetCharData = target

        // 生成3个干扰拼音
        val distractors = mutableListOf<String>()
        var attempts = 0
        while (distractors.size < 3 && attempts < 100) {
            attempts++
            val candidate = gameChars.random().pinyin
            if (candidate != target.pinyin && candidate !in distractors) {
                distractors.add(candidate)
            }
        }

        options = (listOf(target.pinyin) + distractors).shuffled()
        selectedAnswer = null
        showResult = false
    }

    LaunchedEffect(Unit) {
        generateQuestion()
        kotlinx.coroutines.delay(300)
        ttsHelper.speak("看字选音游戏开始")
    }

    fun onOptionSelected(option: String) {
        if (showResult) return
        selectedAnswer = option
        showResult = true
        isCorrect = option == targetCharData?.pinyin
        if (isCorrect) {
            score += 10
            ttsHelper.speakWithHappyTone("正确！${targetCharData?.character}的拼音是${targetCharData?.pinyin}")
            progressDb.updateProgress(
                character = targetCharData?.character ?: "",
                gameDelta = 10,
                incrementPractice = true
            )
        } else {
            ttsHelper.speak("${targetCharData?.character}的拼音是${targetCharData?.pinyin}")
        }
    }

    fun nextRound() {
        if (currentRound + 1 >= totalRounds) {
            gameEnded = true
            if (score > 0) {
                userPrefs.updateHighScorePick(score)
            }
        } else {
            currentRound++
            generateQuestion()
        }
    }

    fun restartGame() {
        currentRound = 0
        score = 0
        gameEnded = false
        generateQuestion()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8E1))
            .verticalScroll(scrollState)
    ) {
        TopBar(title = "看字选音", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "第 ${currentRound + 1} / $totalRounds 题",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )
                Text(
                    text = "得分: $score",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9C27B0)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (gameEnded) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            score >= 80 -> Color(0xFFE8F5E9)
                            score >= 50 -> Color(0xFFFFF3E0)
                            else -> Color(0xFFFFEBEE)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when {
                                score >= 80 -> "🎉 太棒了！"
                                score >= 50 -> "👍 不错！"
                                else -> "💪 继续努力！"
                            },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5D4037)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "本次得分",
                            fontSize = 16.sp,
                            color = Color(0xFF795548)
                        )
                        Text(
                            text = "$score 分",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9C27B0)
                        )
                        Text(
                            text = "正确 ${score / 10} / $totalRounds 题",
                            fontSize = 16.sp,
                            color = Color(0xFF795548)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { restartGame() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF9C27B0)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("再玩一次", fontSize = 18.sp)
                        }
                    }
                }
            } else {
                // 显示汉字大字
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF3E5F5)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📖 看汉字",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF795548)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = targetCharData?.character ?: "",
                            fontSize = 120.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9C27B0)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击汉字听读音",
                            fontSize = 14.sp,
                            color = Color(0xFF795548),
                            modifier = Modifier.clickable {
                                ttsHelper.speak(targetCharData?.character ?: "")
                            }
                        )
                        Button(
                            onClick = { ttsHelper.speak(targetCharData?.character ?: "") },
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .height(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800)
                            )
                        ) {
                            Text("🔊 听读音", fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "选择正确的拼音",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 拼音选项：4个按钮
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    options.forEach { option ->
                        val isThisCorrect = option == targetCharData?.pinyin
                        val isThisSelected = selectedAnswer == option
                        val bgColor = when {
                            showResult && isThisCorrect -> Color(0xFF4CAF50)
                            showResult && !isThisCorrect && isThisSelected -> Color(0xFFF44336)
                            !showResult && isThisSelected -> Color(0xFFFFC107)
                            else -> Color.White
                        }
                        val textColor = when {
                            showResult && (isThisCorrect || (isThisSelected && !isThisCorrect)) -> Color.White
                            else -> Color(0xFF5D4037)
                        }
                        Button(
                            onClick = { onOptionSelected(option) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = bgColor,
                                contentColor = textColor
                            ),
                            border = BorderStroke(
                                2.dp,
                                Color(0xFF9C27B0).copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = option,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showResult) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCorrect) Color(0xFFE8F5E9)
                            else Color(0xFFFFEBEE)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isCorrect) "✓ 回答正确！+10分"
                                else "✗ 正确答案是：${targetCharData?.pinyin}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCorrect) Color(0xFF4CAF50)
                                else Color(0xFFF44336)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { nextRound() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (currentRound + 1 >= totalRounds) "查看成绩" else "下一题 →",
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
