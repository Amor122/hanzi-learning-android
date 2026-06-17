package com.example.hanzilearning.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.hanzilearning.data.*
import com.example.hanzilearning.ui.theme.KaiTiFontFamily
import com.example.hanzilearning.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StrokeLearnScreen(
    onBack: () -> Unit,
    ttsHelper: TextToSpeechHelper,
    userPrefs: UserPreferences,
    progressDb: ProgressDatabaseHelper
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val selectedLevel = userPrefs.currentLevel
    val characters = remember(selectedLevel) { CommonCharacters.getByLevel(selectedLevel) }
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentChar = characters.getOrNull(currentIndex) ?: characters.firstOrNull()
        ?: CharacterData("好", "hǎo", "好的", 1, 0)

    var quizScore by remember { mutableStateOf("") }
    var practiceCount by remember { mutableStateOf(0) }

    LaunchedEffect(currentIndex, currentChar.character) {
        quizScore = ""
        practiceCount = 0
        ttsHelper.speak("现在学习${currentChar.character}字，笔画顺序如下")
    }

    fun nextChar() { if (currentIndex < characters.size - 1) currentIndex++ }
    fun prevChar() { if (currentIndex > 0) currentIndex-- }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8E1))
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("笔画学习", fontFamily = KaiTiFontFamily) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Text("←", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF3F51B5))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "第 ${currentIndex + 1} / ${characters.size} 字",
                fontSize = 13.sp, color = Color(0xFF888888)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 当前字信息卡
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "stroke_title_anim")
                    val floatY by infiniteTransition.animateFloat(
                        initialValue = -2f, targetValue = 2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ), label = "stroke_title_float"
                    )
                    Text(currentChar.character,
                        fontSize = 44.sp, fontWeight = FontWeight.Bold,
                        fontFamily = KaiTiFontFamily, color = Color(0xFF1976D2),
                        modifier = Modifier.graphicsLayer { translationY = floatY }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentChar.pinyin,
                            fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            fontFamily = KaiTiFontFamily, color = Color(0xFFE91E63))
                        Text(currentChar.meaning,
                            fontSize = 14.sp, fontFamily = KaiTiFontFamily, color = Color(0xFF5D4037))
                    }
                    Button(
                        onClick = { ttsHelper.speak(currentChar.character) },
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("🔊", fontSize = 18.sp) }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ========= 核心内容：左侧循环演示，右侧描红练习 =========
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 320.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 左侧：循环演示（WebView）
                Column(modifier = Modifier.weight(1f)) {
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF3F51B5).copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                            Text("📺 笔画演示", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                fontFamily = KaiTiFontFamily, color = Color(0xFF3F51B5))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxSize()) {
                                StrokeAnimationWebView(
                                    character = currentChar.character,
                                    mode = "loop",
                                    strokeColor = "#3F51B5",
                                    outlineColor = "#BBBBBB"
                                )
                            }
                        }
                    }
                }

                // 右侧：描红练习（原生 Canvas 自由画线，无笔画判断，流畅不卡）
                Column(modifier = Modifier.weight(1f)) {
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4CAF50).copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                            Text("✍️ 描红练习", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                fontFamily = KaiTiFontFamily, color = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxSize()) {
                                SimpleDrawPad(character = currentChar.character, key = currentChar.character + "_" + currentIndex)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 控制按钮：重写练习
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { ttsHelper.speak(currentChar.character) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("🔊 听读音", fontSize = 13.sp, fontFamily = KaiTiFontFamily) }
                Button(
                    onClick = { practiceCount++ },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("✓ 完成这字", fontSize = 13.sp, fontFamily = KaiTiFontFamily) }
            }

            if (practiceCount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("很棒！已完成 $practiceCount 次练习 ✨", fontSize = 13.sp,
                    fontFamily = KaiTiFontFamily, color = Color(0xFF4CAF50))
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("提示：看左边演示笔顺，用手指在右边灰色字格中按顺序描红",
                fontSize = 12.sp, fontFamily = KaiTiFontFamily, color = Color(0xFF795548))

            Spacer(modifier = Modifier.height(14.dp))

            // 上下字
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = { prevChar() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    enabled = currentIndex > 0,
                    shape = RoundedCornerShape(14.dp)
                ) { Text("← 上一字", fontSize = 14.sp, fontFamily = KaiTiFontFamily) }
                Button(onClick = { nextChar() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("下一字 →", fontSize = 14.sp, fontFamily = KaiTiFontFamily) }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ========= 核心组件：笔画动画 WebView（支持动画演示和描红两种模式）============
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StrokeAnimationWebView(
    character: String,
    mode: String = "animate",  // "animate" | "loop" | "quiz"
    strokeColor: String = "#4CAF50",
    outlineColor: String = "#DDDDDD"
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    val webViewStateHolder = remember { mutableStateOf<WebView?>(null) }

    // 每次 character/mode 变化时，重置状态并在 1.2s 后关闭加载中提示
    LaunchedEffect(character, mode) {
        isLoading = true
        isError = false
        delay(1200)
        isLoading = false
    }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                webViewStateHolder.value?.onResume()
            }
            override fun onPause(owner: LifecycleOwner) {
                webViewStateHolder.value?.onPause()
            }
            override fun onDestroy(owner: LifecycleOwner) {
                webViewStateHolder.value?.let { wv ->
                    wv.stopLoading()
                    wv.removeAllViews()
                    (wv.parent as? ViewGroup)?.removeView(wv)
                    wv.destroy()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.WHITE)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.setSupportZoom(false)
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val showChar = if (mode == "quiz") false else false
                        val showOutline = true
                        val autoAnim = when (mode) {
                            "loop" -> 1
                            "animate" -> 1
                            else -> 0
                        }
                        view?.evaluateJavascript(
                            "initCharacter('$character', $showOutline, $showChar, '$strokeColor', '$outlineColor', $autoAnim)",
                            null
                        )
                        if (mode == "quiz") {
                            view?.postDelayed({ view.evaluateJavascript("quiz()", null) }, 500)
                        }
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        isError = true
                    }
                }
                loadUrl("file:///android_asset/hanzi_writer.html")
                webViewRef = this
                webViewStateHolder.value = this
            }
        },
        update = { view ->
            val showChar = if (mode == "quiz") false else false
            val showOutline = true
            val autoAnim = when (mode) {
                "loop" -> 1
                "animate" -> 1
                else -> 0
            }
            view.evaluateJavascript(
                "initCharacter('$character', $showOutline, $showChar, '$strokeColor', '$outlineColor', $autoAnim)",
                null
            )
            if (mode == "quiz") {
                view.postDelayed({ view.evaluateJavascript("quiz()", null) }, 600)
            }
        }
    )

    // 加载中显示 - 使用较低 alpha 避免完全遮挡
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.85f },
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp),
                    color = Color(0xFF3F51B5), strokeWidth = 2.dp)
            }
        }
    }

    // 错误提示
    if (isError) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.9f },
            contentAlignment = Alignment.Center) {
            Text("需要网络加载笔画数据", fontSize = 11.sp,
                fontFamily = KaiTiFontFamily, color = Color(0xFFF44336))
        }
    }
}

// ========= 简单描红画板：原生 Canvas 手指画线，不做笔画判断 =========
@Composable
fun SimpleDrawPad(character: String, key: String) {
    // 每条笔画 = 一个点列表，多条笔画组成完整书写
    var strokes by remember(key) { mutableStateOf(listOf<List<Offset>>()) }
    var currentStroke by remember(key) { mutableStateOf(listOf<Offset>()) }
    var isDrawing by remember(key) { mutableStateOf(false) }

    // Canvas 区域需要知道密度进行坐标转换
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {
        // 浅灰色楷体底字（作为临摹参考）
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.22f },
            contentAlignment = Alignment.Center) {
            Text(character, fontSize = 140.sp, fontFamily = KaiTiFontFamily,
                color = Color(0xFF5D4037))
        }

        // 用户绘制的 Canvas（手指画线显示在这里）
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(key) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentStroke = listOf(offset)
                            isDrawing = true
                        },
                        onDrag = { change, _ ->
                            currentStroke = currentStroke + change.position
                            isDrawing = true
                        },
                        onDragEnd = {
                            if (currentStroke.isNotEmpty()) {
                                strokes = strokes + listOf(currentStroke)
                                currentStroke = emptyList()
                            }
                            isDrawing = false
                        },
                        onDragCancel = {
                            if (currentStroke.isNotEmpty()) {
                                strokes = strokes + listOf(currentStroke)
                                currentStroke = emptyList()
                            }
                            isDrawing = false
                        }
                    )
                }
        ) {
            // 绘制已完成的每条笔画（粗线条）
            strokes.forEach { stroke ->
                if (stroke.size >= 2) {
                    val path = Path().apply {
                        moveTo(stroke.first().x, stroke.first().y)
                        for (i in 1 until stroke.size) {
                            lineTo(stroke[i].x, stroke[i].y)
                        }
                    }
                    drawPath(path, color = Color(0xFFE91E63),
                        style = Stroke(width = 22.dp.toPx(),
                            cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            // 绘制当前正在画的笔画
            if (currentStroke.size >= 2) {
                val path = Path().apply {
                    moveTo(currentStroke.first().x, currentStroke.first().y)
                    for (i in 1 until currentStroke.size) {
                        lineTo(currentStroke[i].x, currentStroke[i].y)
                    }
                }
                drawPath(path, color = Color(0xFFE91E63),
                    style = Stroke(width = 22.dp.toPx(),
                        cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        // 清空按钮（右下角小按钮，不显眼，方便重写）
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(end = 4.dp, bottom = 2.dp),
            contentAlignment = Alignment.BottomEnd) {
            androidx.compose.material3.Surface(
                onClick = {
                    strokes = emptyList()
                    currentStroke = emptyList<Offset>()
                },
                modifier = Modifier.size(32.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color(0xFF4CAF50),
                contentColor = Color.White,
                tonalElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🧹", fontSize = 14.sp)
                }
            }
        }
    }
}
