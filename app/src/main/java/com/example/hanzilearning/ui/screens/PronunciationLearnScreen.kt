package com.example.hanzilearning.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hanzilearning.data.*
import com.example.hanzilearning.ui.theme.KaiTiFontFamily
import com.example.hanzilearning.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PronunciationLearnScreen(
    onBack: () -> Unit,
    ttsHelper: TextToSpeechHelper,
    audioHelper: AudioRecorderHelper,
    userPrefs: UserPreferences,
    progressDb: ProgressDatabaseHelper,
    hasRecordPermission: Boolean,
    onRequestPermission: () -> Unit,
    showToast: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val selectedLevel = userPrefs.currentLevel
    val characters = remember(selectedLevel) { CommonCharacters.getByLevel(selectedLevel) }
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentChar = characters.getOrNull(currentIndex) ?: characters.firstOrNull()
        ?: CharacterData("好", "hǎo", "好的", 1, 0)

    var isRecording by remember { mutableStateOf(false) }
    var recordingFile by remember { mutableStateOf<java.io.File?>(null) }
    var showScoreAnimation by remember { mutableStateOf(false) }
    var isFavorited by remember { mutableStateOf(userPrefs.isFavorite(currentChar.character)) }
    var lastScore by remember { mutableStateOf<Int?>(null) }
    var practiceCount by remember { mutableStateOf(0) }

    LaunchedEffect(currentIndex, currentChar.character) {
        isFavorited = userPrefs.isFavorite(currentChar.character)
        recordingFile = null
        isRecording = false
        lastScore = null
        showScoreAnimation = false
    }

    fun playStandard() { ttsHelper.speak(currentChar.character) }
    fun playSlow() { ttsHelper.speakSlow(currentChar.character) }

    fun startRecording() {
        if (!hasRecordPermission) { onRequestPermission(); return }
        if (audioHelper.startRecording(currentChar.character)) {
            isRecording = true
            showToast("开始录音，请跟读...")
        }
    }
    fun stopRecording() {
        val file = audioHelper.stopRecording()
        isRecording = false
        recordingFile = file
        if (file != null) {
            lastScore = audioHelper.evaluateRecording(file)
            showScoreAnimation = true
            practiceCount++
            progressDb.updateProgress(
                character = currentChar.character,
                pinyinDelta = (lastScore ?: 0) / 10,
                incrementPractice = true
            )
            scope.launch { delay(4000); showScoreAnimation = false }
        }
    }
    fun playRecording() {
        recordingFile?.let { audioHelper.playRecording(it) {} }
    }

    fun nextChar() { if (currentIndex < characters.size - 1) currentIndex++ }
    fun prevChar() { if (currentIndex > 0) currentIndex-- }
    fun toggleFavorite() {
        isFavorited = userPrefs.toggleFavorite(currentChar.character)
        showToast(if (isFavorited) "已加入收藏" else "已取消收藏")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8E1))
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("读音学习", fontFamily = KaiTiFontFamily) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Text("←", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFF9800))
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("第 ${currentIndex + 1} / ${characters.size} 字",
                fontSize = 12.sp, color = Color(0xFF888888))

            Spacer(modifier = Modifier.height(8.dp))

            // ========= 1. 主字卡片 =========
            Card(
                modifier = Modifier.fillMaxWidth().height(210.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = androidx.compose.foundation.BorderStroke(3.dp,
                    Color(0xFFE91E63).copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(6.dp),
                        horizontalArrangement = Arrangement.End) {
                        FavoriteHeart(isFavorited = isFavorited, onClick = { toggleFavorite() })
                    }
                    Text(currentChar.pinyin, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        fontFamily = KaiTiFontFamily, color = Color(0xFFE91E63))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(currentChar.character, fontSize = 90.sp,
                        fontWeight = FontWeight.Bold, fontFamily = KaiTiFontFamily,
                        color = Color(0xFF3E2723))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(currentChar.meaning, fontSize = 14.sp,
                        fontFamily = KaiTiFontFamily, color = Color(0xFF795548))
                    if (practiceCount > 0) {
                        Text("已练习 $practiceCount 次", fontSize = 11.sp,
                            color = Color(0xFF888888))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ========= 2. 笔画顺序 =========
            Card(modifier = Modifier.fillMaxWidth().height(190.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(2.dp,
                    Color(0xFF9C27B0).copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✏️ 笔画顺序", fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, fontFamily = KaiTiFontFamily,
                            color = Color(0xFF9C27B0))
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxSize()) {
                        StrokeAnimationWebView(character = currentChar.character,
                            mode = "loop",
                            strokeColor = "#9C27B0",
                            outlineColor = "#CCCCCC")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ========= 3. 拼音 + 字义 =========
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 拼音
                Card(modifier = Modifier.weight(1f).height(150.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(2.dp,
                        Color(0xFFE91E63).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Text("拼音", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            fontFamily = KaiTiFontFamily, color = Color(0xFFE91E63))
                        Spacer(Modifier.height(4.dp))
                        Text(currentChar.pinyin, fontSize = 34.sp,
                            fontWeight = FontWeight.Bold, fontFamily = KaiTiFontFamily,
                            color = Color(0xFFE91E63))
                        Spacer(Modifier.height(4.dp))
                        Text(currentChar.character, fontSize = 22.sp,
                            fontFamily = KaiTiFontFamily, color = Color(0xFF3E2723))
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { playStandard() },
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE91E63)),
                                shape = RoundedCornerShape(10.dp)) {
                                Text("🔊", fontSize = 12.sp)
                            }
                            Button(onClick = { playSlow() },
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF48FB1)),
                                shape = RoundedCornerShape(10.dp)) {
                                Text("慢读", fontSize = 12.sp, fontFamily = KaiTiFontFamily)
                            }
                        }
                    }
                }

                // 字义
                Card(modifier = Modifier.weight(1f).height(150.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    border = androidx.compose.foundation.BorderStroke(2.dp,
                        Color(0xFF2196F3).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Text("字义", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            fontFamily = KaiTiFontFamily, color = Color(0xFF1976D2))
                        Spacer(Modifier.height(6.dp))
                        Text(currentChar.character, fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, fontFamily = KaiTiFontFamily,
                            color = Color(0xFF1976D2))
                        Spacer(Modifier.height(6.dp))
                        Text(currentChar.meaning, fontSize = 18.sp,
                            fontFamily = KaiTiFontFamily, color = Color(0xFF5D4037))
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { playStandard() },
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2196F3)),
                            shape = RoundedCornerShape(10.dp)) {
                            Text("朗读", fontSize = 12.sp, fontFamily = KaiTiFontFamily)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ========= 4. 跟读练习 =========
            Card(modifier = Modifier.fillMaxWidth().height(170.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(2.dp,
                    Color(0xFF4CAF50).copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text("🎤 跟读练习", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        fontFamily = KaiTiFontFamily, color = Color(0xFF4CAF50))
                    Spacer(Modifier.height(4.dp))
                    if (showScoreAnimation && lastScore != null) {
                        val scoreAnim by animateIntAsState(
                            targetValue = lastScore ?: 0,
                            animationSpec = tween(durationMillis = 1200,
                                easing = FastOutSlowInEasing),
                            label = "read_score")
                        Text("$scoreAnim 分", fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                lastScore!! >= 85 -> Color(0xFF4CAF50)
                                lastScore!! >= 70 -> Color(0xFF2196F3)
                                lastScore!! >= 50 -> Color(0xFFFF9800)
                                else -> Color(0xFFF44336)
                            })
                        Spacer(Modifier.height(2.dp))
                        Text(currentChar.pinyin, fontSize = 14.sp,
                            color = Color(0xFF888888), fontFamily = KaiTiFontFamily)
                    } else {
                        Text(currentChar.character, fontSize = 42.sp,
                            fontWeight = FontWeight.Bold, fontFamily = KaiTiFontFamily,
                            color = Color(0xFF4CAF50))
                        Spacer(Modifier.height(2.dp))
                        Text(currentChar.pinyin, fontSize = 16.sp,
                            color = Color(0xFF5D4037), fontFamily = KaiTiFontFamily)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (isRecording) stopRecording() else startRecording()
                        },
                            modifier = Modifier.height(40.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) Color(0xFFF44336) else Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(12.dp)) {
                            Text(if (isRecording) "⏹ 停止" else "🎤 跟读",
                                fontSize = 12.sp, fontFamily = KaiTiFontFamily)
                        }
                        OutlinedButton(onClick = { playRecording() },
                            modifier = Modifier.height(40.dp),
                            enabled = recordingFile != null,
                            shape = RoundedCornerShape(12.dp)) {
                            Text("▶ 回放", fontSize = 12.sp, fontFamily = KaiTiFontFamily)
                        }
                    }
                    if (!hasRecordPermission) {
                        Spacer(Modifier.height(3.dp))
                        Text("⚠ 请先授权录音权限",
                            fontSize = 10.sp, color = Color(0xFFF44336))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ========= 上下字 =========
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = { prevChar() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    enabled = currentIndex > 0,
                    shape = RoundedCornerShape(14.dp)
                ) { Text("← 上一字", fontSize = 14.sp, fontFamily = KaiTiFontFamily) }
                Button(onClick = { nextChar() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("下一字 →", fontSize = 14.sp, fontFamily = KaiTiFontFamily) }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

// ========== 收藏心形动画 ==========
@Composable
fun FavoriteHeart(isFavorited: Boolean, onClick: () -> Unit) {
    var triggerPop by remember { mutableStateOf(0) }
    val popScale = animateFloatAsState(
        targetValue = if (triggerPop > 0) 1.4f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.4f),
        label = "learn_heart_pop",
        finishedListener = { if (triggerPop > 0) triggerPop = 0 }).value
    LaunchedEffect(isFavorited) { if (isFavorited) triggerPop++ }
    IconButton(onClick = onClick) {
        Text(if (isFavorited) "❤️" else "🤍", fontSize = 18.sp,
            modifier = Modifier.graphicsLayer { scaleX = popScale; scaleY = popScale })
    }
}
