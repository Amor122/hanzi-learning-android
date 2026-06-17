package com.example.hanzilearning.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hanzilearning.data.*
import com.example.hanzilearning.ui.*
import com.example.hanzilearning.util.*
import kotlin.random.Random

@Composable
fun ListenAndPickGame(
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
            2 -> CommonCharacters.getByLevel(1)  // 2年级混入1年级
            3 -> (1..2).flatMap { CommonCharacters.getByLevel(it) }  // 3年级混入1-2年级
            4 -> (1..3).flatMap { CommonCharacters.getByLevel(it) }  // 4年级混入1-3年级
            5 -> (1..4).flatMap { CommonCharacters.getByLevel(it) }  // 5年级混入1-4年级
            else -> emptyList()
        }
        // 混合比例：高级别混入更多低级别字
        val ratio = when (selectedLevel) {
            1, 2 -> 1.0  // 1-2年级100%本级
            3 -> 0.8     // 3年级80%本级
            4 -> 0.7     // 4年级70%本级
            5 -> 0.6     // 5年级60%本级
            else -> 1.0
        }
        val mainCount = (currentLevel.size * ratio).toInt()
        val reviewCount = (currentLevel.size * (1 - ratio)).toInt()
        (currentLevel.shuffled().take(mainCount) + reviewChars.shuffled().take(reviewCount)).shuffled()
    }
    
    // 获取当前等级的字（用于显示题目难度）
    val currentLevelChars = remember(selectedLevel) { CommonCharacters.getByLevel(selectedLevel) }

    var currentRound by remember { mutableIntStateOf(0) }
    val totalRounds = 10
    var score by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }

    var targetChar by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf("", "", "", "")) }
    var gameEnded by remember { mutableStateOf(false) }

    // 生成新的一题
    fun generateQuestion() {
        if (gameChars.isEmpty()) return
        // 从混合字库中选取目标字
        val target = gameChars.random()
        targetChar = target.character

        // 生成3个干扰项（来自混合字库但不是目标字）
        val distractors = mutableListOf<String>()
        var attempts = 0
        while (distractors.size < 3 && attempts < 100) {
            attempts++
            val candidate = gameChars.random().character
            if (candidate != targetChar && candidate !in distractors) {
                distractors.add(candidate)
            }
        }

        options = (listOf(targetChar) + distractors).shuffled()
        selectedAnswer = null
        showResult = false
    }

    LaunchedEffect(Unit) {
        generateQuestion()
        kotlinx.coroutines.delay(300)
        if (targetChar.isNotEmpty()) {
            ttsHelper.speak("听音选字游戏开始，请听汉字读音")
            kotlinx.coroutines.delay(1200)
            ttsHelper.speak(targetChar)
        }
    }

    // 每次换题时朗读
    LaunchedEffect(currentRound) {
        if (currentRound > 0 && !gameEnded && targetChar.isNotEmpty()) {
            kotlinx.coroutines.delay(300)
            ttsHelper.speak(targetChar)
        }
    }

    fun onOptionSelected(option: String) {
        if (showResult) return
        selectedAnswer = option
        showResult = true
        isCorrect = option == targetChar
        if (isCorrect) {
            score += 10
            ttsHelper.speakWithHappyTone("正确！你真棒")
            // 更新进度
            progressDb.updateProgress(
                character = targetChar,
                gameDelta = 10,
                incrementPractice = true
            )
        } else {
            ttsHelper.speak("正确答案是$targetChar")
        }
    }

    fun nextRound() {
        if (currentRound + 1 >= totalRounds) {
            gameEnded = true
            // 更新最高分
            if (score > 0) {
                userPrefs.updateHighScoreListen(score)
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
        TopBar(title = "听音选字", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 状态栏
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
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (gameEnded) {
                // 游戏结束画面
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
                            color = Color(0xFFFF9800)
                        )
                        Text(
                            text = "正确 ${score / 10} / $totalRounds 题",
                            fontSize = 16.sp,
                            color = Color(0xFF795548)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        if (score > userPrefs.highScoreListen) {
                            Text(
                                text = "🏆 新纪录！",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE91E63)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Button(
                            onClick = { restartGame() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("再玩一次", fontSize = 18.sp)
                        }
                    }
                }
            } else {
                // 题目显示
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
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
                            text = "👂 听读音",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5D4037)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { ttsHelper.speak(targetChar) },
                            modifier = Modifier
                                .size(100.dp),
                            shape = RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("🔊", fontSize = 40.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击喇叭听读音",
                            fontSize = 14.sp,
                            color = Color(0xFF795548)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "选出正确的汉字",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 选项：2x2 网格
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OptionButton(
                            char = options[0],
                            isSelected = selectedAnswer == options[0],
                            isCorrect = if (showResult) options[0] == targetChar else null,
                            onClick = { onOptionSelected(options[0]) },
                            modifier = Modifier.weight(1f)
                        )
                        OptionButton(
                            char = options[1],
                            isSelected = selectedAnswer == options[1],
                            isCorrect = if (showResult) options[1] == targetChar else null,
                            onClick = { onOptionSelected(options[1]) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OptionButton(
                            char = options[2],
                            isSelected = selectedAnswer == options[2],
                            isCorrect = if (showResult) options[2] == targetChar else null,
                            onClick = { onOptionSelected(options[2]) },
                            modifier = Modifier.weight(1f)
                        )
                        OptionButton(
                            char = options[3],
                            isSelected = selectedAnswer == options[3],
                            isCorrect = if (showResult) options[3] == targetChar else null,
                            onClick = { onOptionSelected(options[3]) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 结果反馈
                if (showResult) {
                    AnimatedVisibility(visible = showResult) {
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
                                    else "✗ 正确答案是：$targetChar",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrect) Color(0xFF4CAF50)
                                    else Color(0xFFF44336)
                                )
                            }
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

@Composable
fun OptionButton(
    char: String,
    isSelected: Boolean,
    isCorrect: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isCorrect == true -> Color(0xFF4CAF50)
        isCorrect == false && isSelected -> Color(0xFFF44336)
        isSelected -> Color(0xFFFFC107)
        else -> Color.White
    }
    val textColor = when {
        isCorrect == true -> Color.White
        isCorrect == false && isSelected -> Color.White
        else -> Color(0xFF3E2723)
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            Color(0xFF5D4037).copy(alpha = 0.2f)
        )
    ) {
        Text(
            text = char,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
