package com.example.hanzilearning.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.hanzilearning.data.*
import com.example.hanzilearning.ui.screens.*
import com.example.hanzilearning.util.*
import kotlinx.coroutines.launch

enum class AppScreen {
    MAIN_MENU,
    PRONUNCIATION_LEARN,
    STROKE_LEARN,
    PROGRESS,
    GAME_LISTEN_PICK, // 听音选字
    GAME_CHAR_PICK_PINYIN, // 看字选音
    GAME_STROKE_PUZZLE, // 笔顺拼图
    SETTINGS
}

@Composable
fun MainAppScreen(activity: Activity) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(AppScreen.MAIN_MENU) }
    val ttsHelper = remember { TextToSpeechHelper(context) }
    val audioHelper = remember { AudioRecorderHelper(context) }
    val userPrefs = remember { UserPreferences(context) }
    val progressDb = remember { ProgressDatabaseHelper(context) }

    // 检查录音权限
    val RECORD_PERMISSION = Manifest.permission.RECORD_AUDIO
    val hasRecordPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, RECORD_PERMISSION) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun requestRecordPermission() {
        if (ContextCompat.checkSelfPermission(context, RECORD_PERMISSION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(RECORD_PERMISSION),
                1001
            )
        } else {
            hasRecordPermission.value = true
        }
    }

    // 简单的Toast
    fun showToast(msg: String) {
        scope.launch {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
            audioHelper.cleanup()
        }
    }

    // 回退处理
    BackHandler(enabled = currentScreen != AppScreen.MAIN_MENU) {
        currentScreen = AppScreen.MAIN_MENU
    }

    // 路由
    when (currentScreen) {
        AppScreen.MAIN_MENU -> {
            MainMenuScreen(
                onPronunciationClick = { currentScreen = AppScreen.PRONUNCIATION_LEARN },
                onStrokeClick = { currentScreen = AppScreen.STROKE_LEARN },
                onProgressClick = { currentScreen = AppScreen.PROGRESS },
                onGame1Click = { currentScreen = AppScreen.GAME_LISTEN_PICK },
                onGame2Click = { currentScreen = AppScreen.GAME_CHAR_PICK_PINYIN },
                onGame3Click = { currentScreen = AppScreen.GAME_STROKE_PUZZLE },
                onSettingsClick = { currentScreen = AppScreen.SETTINGS },
                ttsHelper = ttsHelper,
                userPrefs = userPrefs,
                progressDb = progressDb
            )
        }

        AppScreen.PRONUNCIATION_LEARN -> {
            PronunciationLearnScreen(
                onBack = { currentScreen = AppScreen.MAIN_MENU },
                ttsHelper = ttsHelper,
                audioHelper = audioHelper,
                userPrefs = userPrefs,
                progressDb = progressDb,
                hasRecordPermission = hasRecordPermission.value,
                onRequestPermission = { requestRecordPermission() },
                showToast = { showToast(it) }
            )
        }

        AppScreen.STROKE_LEARN -> {
            StrokeLearnScreen(
                onBack = { currentScreen = AppScreen.MAIN_MENU },
                ttsHelper = ttsHelper,
                userPrefs = userPrefs,
                progressDb = progressDb
            )
        }

        AppScreen.PROGRESS -> {
            ProgressScreen(
                onBack = { currentScreen = AppScreen.MAIN_MENU },
                userPrefs = userPrefs,
                progressDb = progressDb
            )
        }

        AppScreen.GAME_LISTEN_PICK -> {
            ListenAndPickGame(
                onBack = { currentScreen = AppScreen.MAIN_MENU },
                ttsHelper = ttsHelper,
                userPrefs = userPrefs,
                progressDb = progressDb,
                showToast = { showToast(it) }
            )
        }

        AppScreen.GAME_CHAR_PICK_PINYIN -> {
            CharAndPickPinyinGame(
                onBack = { currentScreen = AppScreen.MAIN_MENU },
                ttsHelper = ttsHelper,
                userPrefs = userPrefs,
                progressDb = progressDb,
                showToast = { showToast(it) }
            )
        }

        AppScreen.GAME_STROKE_PUZZLE -> {
            StrokePuzzleGame(
                onBack = { currentScreen = AppScreen.MAIN_MENU },
                ttsHelper = ttsHelper,
                userPrefs = userPrefs,
                progressDb = progressDb,
                showToast = { showToast(it) }
            )
        }

        AppScreen.SETTINGS -> {
            SettingsScreen(
                onBack = { currentScreen = AppScreen.MAIN_MENU },
                userPrefs = userPrefs,
                progressDb = progressDb
            )
        }
    }
}

@Composable
fun MainMenuScreen(
    onPronunciationClick: () -> Unit,
    onStrokeClick: () -> Unit,
    onProgressClick: () -> Unit,
    onGame1Click: () -> Unit,
    onGame2Click: () -> Unit,
    onGame3Click: () -> Unit,
    onSettingsClick: () -> Unit,
    ttsHelper: TextToSpeechHelper,
    userPrefs: UserPreferences,
    progressDb: ProgressDatabaseHelper
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 欢迎信息
    val welcomeText = if (userPrefs.userName.isNotEmpty()) {
        "欢迎，${userPrefs.userName}！"
    } else {
        "欢迎学习汉字！"
    }

    val todayStats = remember { progressDb.getTodayStats() }
    val masteredCount = remember { progressDb.getMasteredCount() }
    val totalChars = remember { CommonCharacters.totalCount() }

    LaunchedEffect(Unit) {
        // 启动时播放欢迎语（如果开启了声音）
        if (userPrefs.soundEnabled) {
            kotlinx.coroutines.delay(300)
            ttsHelper.speak(welcomeText)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Text(
            text = "汉字学习",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD2691E)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = welcomeText,
            fontSize = 18.sp,
            color = Color(0xFF8B4513)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 进度小卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "已掌握",
                value = "$masteredCount/$totalChars",
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "今日学习",
                value = "${todayStats.charactersLearned}字",
                color = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "今日练习",
                value = "${todayStats.totalPractices}次",
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 学习区（大卡片）
        SectionTitle("学习模式")
        Spacer(modifier = Modifier.height(12.dp))

        BigFeatureCard(
            title = "读音学习",
            subtitle = "拼音 · 跟读 · 录音评分",
            emoji = "🔊",
            color = Color(0xFFE91E63),
            onClick = {
                ttsHelper.speak("开始读音学习")
                onPronunciationClick()
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        BigFeatureCard(
            title = "笔画学习",
            subtitle = "笔顺动画 · 田字格描红",
            emoji = "✍️",
            color = Color(0xFF3F51B5),
            onClick = {
                ttsHelper.speak("开始笔画学习")
                onStrokeClick()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 游戏区
        SectionTitle("小游戏")
        Spacer(modifier = Modifier.height(12.dp))

        SmallFeatureCard(
            title = "听音选字",
            subtitle = "听读音，选正确的字",
            emoji = "👂",
            color = Color(0xFF4CAF50),
            onClick = {
                ttsHelper.speak("开始听音选字游戏")
                onGame1Click()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        SmallFeatureCard(
            title = "看字选音",
            subtitle = "看汉字，选正确的拼音",
            emoji = "📖",
            color = Color(0xFF9C27B0),
            onClick = {
                ttsHelper.speak("开始看字选音游戏")
                onGame2Click()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        SmallFeatureCard(
            title = "笔顺拼图",
            subtitle = "按正确顺序排列笔画",
            emoji = "🧩",
            color = Color(0xFF009688),
            onClick = {
                ttsHelper.speak("开始笔顺拼图游戏")
                onGame3Click()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 其他
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedCard(
                title = "学习进度",
                emoji = "📊",
                color = Color(0xFF607D8B),
                modifier = Modifier.weight(1f),
                onClick = { onProgressClick() }
            )
            OutlinedCard(
                title = "设置",
                emoji = "⚙️",
                color = Color(0xFF795548),
                modifier = Modifier.weight(1f),
                onClick = { onSettingsClick() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "共收录 $totalChars 个常用汉字",
            fontSize = 14.sp,
            color = Color(0xFF888888)
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5D4037)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFF5D4037).copy(alpha = 0.3f))
        )
    }
}

@Composable
fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color(0xFF5D4037)
            )
        }
    }
}

@Composable
fun BigFeatureCard(
    title: String,
    subtitle: String,
    emoji: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = color
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "→",
                fontSize = 32.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun SmallFeatureCard(
    title: String,
    subtitle: String,
    emoji: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = color
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Text(
                text = "▶",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun OutlinedCard(
    title: String,
    emoji: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            color.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(Color(0xFFFFF8E1))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Text(
                text = "←",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5D4037)
            )
        }
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5D4037)
        )
    }
}
