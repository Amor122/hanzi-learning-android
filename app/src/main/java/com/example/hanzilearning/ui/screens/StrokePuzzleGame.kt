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
fun StrokePuzzleGame(
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

    var currentRound by remember { mutableIntStateOf(0) }
    val totalRounds = 8
    var score by remember { mutableIntStateOf(0) }
    var gameEnded by remember { mutableStateOf(false) }

    // 当前题目
    var targetChar by remember { mutableStateOf("") }
    var targetMeaning by remember { mutableStateOf("") }

    // 拆字：将汉字拆成"部件"进行排序
    // 我们用一种简化的方式：根据汉字笔画生成一些"虚拟笔画块"
    // 让用户按正确顺序选择
    var puzzlePieces by remember { mutableStateOf(listOf<String>()) }
    var currentOrder by remember { mutableStateOf(listOf<Int>()) }
    var correctOrder by remember { mutableStateOf(listOf<Int>()) }
    var showResult by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // 根据汉字生成"部件块" —— 用描述性文字代表笔画顺序
    // 例如"好" = "女"+"子"，需要先女后子
    fun generatePuzzle(char: String, pinyin: String, meaning: String): Pair<List<String>, List<Int>> {
        // 简化的"拆字"：根据一些常见汉字拆出部件
        val decomposable = mapOf(
            "好" to listOf("女" to "左边", "子" to "右边"),
            "明" to listOf("日" to "左边", "月" to "右边"),
            "休" to listOf("亻" to "左边", "木" to "右边"),
            "林" to listOf("木" to "左木", "木" to "右木"),
            "森" to listOf("木" to "上木", "木" to "左下", "木" to "右下"),
            "看" to listOf("手" to "上部", "目" to "下部"),
            "妈" to listOf("女" to "左边", "马" to "右边"),
            "爸" to listOf("父" to "上部", "巴" to "下部"),
            "安" to listOf("宀" to "顶部", "女" to "底部"),
            "家" to listOf("宀" to "顶部", "豕" to "底部"),
            "想" to listOf("相" to "上部", "心" to "下部"),
            "思" to listOf("田" to "上部", "心" to "下部"),
            "念" to listOf("今" to "上部", "心" to "下部"),
            "唱" to listOf("口" to "左边", "昌" to "右边"),
            "和" to listOf("禾" to "左边", "口" to "右边"),
            "加" to listOf("力" to "左边", "口" to "右边"),
            "的" to listOf("白" to "左边", "勺" to "右边"),
            "地" to listOf("土" to "左边", "也" to "右边"),
            "你" to listOf("亻" to "左边", "尔" to "右边"),
            "他" to listOf("亻" to "左边", "也" to "右边"),
            "她" to listOf("女" to "左边", "也" to "右边"),
            "们" to listOf("亻" to "左边", "门" to "右边"),
            "说" to listOf("讠" to "左边", "兑" to "右边"),
            "话" to listOf("讠" to "左边", "舌" to "右边"),
            "读" to listOf("讠" to "左边", "卖" to "右边"),
            "写" to listOf("冖" to "上部", "与" to "下部"),
            "字" to listOf("宀" to "上部", "子" to "下部"),
            "学" to listOf("⺍" to "上部", "子" to "下部"),
            "校" to listOf("木" to "左边", "交" to "右边"),
            "国" to listOf("口" to "外框", "玉" to "内部"),
            "图" to listOf("口" to "外框", "冬" to "内部"),
            "回" to listOf("口" to "外框", "口" to "内部"),
            "口" to listOf("丨" to "竖", "ㄱ" to "横折", "一" to "横"),
            "日" to listOf("丨" to "左竖", "ㄱ" to "横折", "一" to "中横", "一" to "底横"),
            "月" to listOf("丿" to "撇", "ㄱ" to "横折", "一" to "横1", "一" to "横2"),
            "山" to listOf("丨" to "中竖", "ㄥ" to "左折", "丨" to "右竖"),
            "水" to listOf("亅" to "竖钩", "丿" to "左撇", "丿" to "撇", "㇏" to "捺"),
            "火" to listOf("丶" to "点1", "丿" to "撇", "丿" to "长撇", "㇏" to "捺"),
            "土" to listOf("一" to "上横", "丨" to "中竖", "一" to "下横"),
            "木" to listOf("一" to "上横", "丨" to "中竖", "丿" to "撇", "㇏" to "捺"),
            "天" to listOf("一" to "上横", "一" to "下横", "丿" to "撇", "㇏" to "捺"),
            "人" to listOf("丿" to "撇", "㇏" to "捺"),
            "大" to listOf("一" to "横", "丿" to "撇", "㇏" to "捺"),
            "小" to listOf("亅" to "竖钩", "丿" to "撇", "丶" to "点"),
            "上" to listOf("丨" to "竖", "一" to "上横", "一" to "下横"),
            "下" to listOf("一" to "上横", "丨" to "竖", "丶" to "点"),
            "中" to listOf("口" to "框", "丨" to "中竖"),
            "三" to listOf("一" to "上横", "一" to "中横", "一" to "下横"),
            "二" to listOf("一" to "上横", "一" to "下横"),
            "一" to listOf("一" to "横"),
            "十" to listOf("一" to "横", "丨" to "竖"),
            "白" to listOf("丿" to "撇", "日" to "日", "一" to "底横"),
            "百" to listOf("一" to "上横", "白" to "下部"),
            "千" to listOf("丿" to "撇", "十" to "十字"),
            "万" to listOf("一" to "横", "勹" to "撇折钩", "丿" to "撇"),
            "亿" to listOf("亻" to "人旁", "乙" to "右边"),
            "男" to listOf("田" to "上部", "力" to "下部"),
            "女" to listOf("ㄑ" to "撇点", "丿" to "撇", "一" to "横"),
            "子" to listOf("了" to "上", "一" to "横"),
            "儿" to listOf("丿" to "撇", "乚" to "竖弯钩"),
            "哥" to listOf("可" to "上", "可" to "下"),
            "姐" to listOf("女" to "左", "且" to "右"),
            "弟" to listOf("丷" to "上", "弓" to "中", "丿" to "撇"),
            "妹" to listOf("女" to "左", "未" to "右"),
            "爷" to listOf("父" to "上", "卩" to "下"),
            "奶" to listOf("女" to "左", "乃" to "右"),
            "我" to listOf("丿" to "撇", "扌" to "手旁", "戈" to "右部"),
            "你" to listOf("亻" to "左边", "尔" to "右边"),
            "是" to listOf("日" to "上", "正" to "下"),
            "有" to listOf("一" to "上", "月" to "下"),
            "在" to listOf("土" to "左", "才" to "右"),
            "不" to listOf("一" to "上", "丶" to "点", "丿" to "撇", "丨" to "竖"),
            "这" to listOf("文" to "文", "辶" to "走之旁"),
            "那" to listOf("那" to "那"),
            "个" to listOf("人" to "人", "丨" to "竖"),
            "为" to listOf("丶" to "点", "力" to "力", "丶" to "点"),
            "来" to listOf("一" to "上", "米" to "下"),
            "去" to listOf("土" to "上", "厶" to "下"),
            "出" to listOf("山" to "上", "山" to "下"),
            "进" to listOf("井" to "井", "辶" to "走之"),
            "回" to listOf("口" to "外", "口" to "内"),
            "到" to listOf("至" to "左", "刂" to "刀旁"),
            "东" to listOf("一" to "上", "小" to "中", "丨" to "竖"),
            "西" to listOf("一" to "上", "儿" to "下"),
            "南" to listOf("十" to "上", "冂" to "框", "¥" to "内"),
            "北" to listOf("丨" to "中", "一" to "横", "丿" to "左撇", "乚" to "右钩"),
            "左" to listOf("一" to "上", "丿" to "撇", "工" to "下"),
            "右" to listOf("一" to "上", "口" to "下"),
            "前" to listOf("丷" to "上", "月" to "中", "刂" to "右"),
            "后" to listOf("尸" to "上", "口" to "下"),
            "里" to listOf("日" to "上", "土" to "下"),
            "外" to listOf("夕" to "夕", "卜" to "卜"),
            "内" to listOf("冂" to "框", "人" to "内"),
            "走" to listOf("土" to "上", "止" to "下"),
            "行" to listOf("彳" to "左", "亍" to "右"),
            "跑" to listOf("足" to "足旁", "包" to "右"),
            "跳" to listOf("足" to "足旁", "兆" to "右"),
            "飞" to listOf("乙" to "飞1", "丨" to "飞2", "丶" to "飞3"),
            "心" to listOf("丶" to "点1", "㇃" to "卧钩", "丶" to "点2", "丶" to "点3")
        )

        // 如果有现成的拆解，使用它
        val pieces = decomposable[char]
        if (pieces != null) {
            val ordered = pieces.map { "${it.first} (${it.second})" }
            return ordered to ordered.indices.toList()
        }

        // 否则用简化的笔画描述
        val simplified = listOf(
            "第一笔",
            "第二笔",
            "第三笔",
            "第四笔",
            "第五笔",
            "第六笔",
            "第七笔",
            "第八笔"
        )

        // 根据字符长度估算笔画数（简单伪估算）
        val strokeCount = when (char.length) {
            1 -> (3..6).random()
            else -> 4
        }
        val selected = simplified.take(strokeCount).mapIndexed { i, s ->
            "$s (${char}的第${i + 1}笔)"
        }
        return selected to selected.indices.toList()
    }

    fun generateQuestion() {
        // 优先从可分解汉字中选
        val decomposableCandidates = listOf(
            "好", "明", "休", "林", "森", "看", "妈", "爸", "安", "家",
            "想", "思", "念", "唱", "和", "的", "地", "你", "他", "她",
            "们", "说", "话", "字", "学", "校", "国", "图", "回", "口",
            "日", "月", "山", "水", "火", "土", "木", "天", "人", "大",
            "小", "上", "下", "中", "三", "二", "一", "十", "男", "女",
            "子", "哥", "姐", "弟", "妹", "爷", "奶", "我", "是", "有",
            "在", "不", "为", "来", "去", "出", "进", "回", "到", "东",
            "西", "南", "北", "左", "右", "前", "后", "里", "外", "走",
            "行", "跑", "跳", "飞", "心"
        )

        // 先尝试从可拆分的候选字中选择
        val decomposableInLevel = decomposableCandidates.filter { char ->
            gameChars.any { it.character == char }
        }
        
        val chosen = if (decomposableInLevel.isNotEmpty()) {
            decomposableInLevel.random()
        } else {
            gameChars.random().character
        }

        val charData = gameChars.firstOrNull { it.character == chosen }
            ?: CharacterData(chosen, "ch", "常用字", 1, 0)

        targetChar = chosen
        targetMeaning = charData.meaning

        val (pieces, order) = generatePuzzle(chosen, charData.pinyin, charData.meaning)
        // 打乱顺序
        val shuffled = pieces.withIndex().shuffled()
        puzzlePieces = shuffled.map { it.value }
        correctOrder = shuffled.map { it.index }
        currentOrder = emptyList()
        showResult = false
        errorMessage = ""
    }

    LaunchedEffect(Unit) {
        generateQuestion()
        kotlinx.coroutines.delay(300)
        ttsHelper.speak("笔顺拼图游戏开始")
        kotlinx.coroutines.delay(1200)
        ttsHelper.speak("请按正确顺序点击$targetChar 的各个部件")
    }

    fun selectPiece(index: Int) {
        if (showResult) return
        if (index in currentOrder) {
            // 取消选择
            val pos = currentOrder.indexOf(index)
            currentOrder = currentOrder.take(pos)
            return
        }
        currentOrder = currentOrder + index

        // 检查是否完成
        if (currentOrder.size == puzzlePieces.size) {
            // 判断是否正确 —— 比较实际索引和正确顺序
            val selectedOrder = currentOrder.map { correctOrder[it] }
            isCorrect = selectedOrder == puzzlePieces.indices.toList()
            showResult = true
            if (isCorrect) {
                score += 12
                ttsHelper.speakWithHappyTone("正确！你真棒")
                progressDb.updateProgress(
                    character = targetChar,
                    gameDelta = 12,
                    incrementPractice = true
                )
            } else {
                ttsHelper.speak("顺序不对哦，$targetChar 需要按笔画顺序")
                errorMessage = "顺序错误"
            }
        }
    }

    fun nextRound() {
        if (currentRound + 1 >= totalRounds) {
            gameEnded = true
            if (score > 0) {
                userPrefs.updateHighScoreStroke(score)
            }
        } else {
            currentRound++
            generateQuestion()
            ttsHelper.speak("请按正确顺序点击$targetChar 的各个部件")
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
        TopBar(title = "笔顺拼图", onBack = onBack)

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
                    color = Color(0xFF009688)
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
                            score >= 72 -> Color(0xFFE8F5E9)
                            score >= 48 -> Color(0xFFFFF3E0)
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
                                score >= 72 -> "🎉 太棒了！"
                                score >= 48 -> "👍 不错！"
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
                            color = Color(0xFF009688)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { restartGame() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF009688)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("再玩一次", fontSize = 18.sp)
                        }
                    }
                }
            } else {
                // 目标汉字显示
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE0F2F1)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🧩 请组装这个字",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00695C)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = targetChar,
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF009688)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$targetMeaning",
                            fontSize = 14.sp,
                            color = Color(0xFF795548)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "需要按正确笔画/部件顺序点击",
                            fontSize = 12.sp,
                            color = Color(0xFF888888)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "点击部件按顺序排列",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 显示已选顺序
                if (currentOrder.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "已选:",
                                fontSize = 14.sp,
                                color = Color(0xFF795548)
                            )
                            currentOrder.forEachIndexed { idx, pieceIdx ->
                                Text(
                                    text = "${idx + 1}. ${puzzlePieces[pieceIdx]}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9800),
                                    modifier = Modifier.clickable {
                                        if (!showResult) {
                                            // 点击已选项可以移除它及之后的项
                                            val pos = currentOrder.indexOf(pieceIdx)
                                            currentOrder = currentOrder.take(pos)
                                        }
                                    }
                                )
                                if (idx < currentOrder.size - 1) {
                                    Text("→", fontSize = 12.sp, color = Color(0xFF888888))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 可点击的部件块
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    puzzlePieces.forEachIndexed { idx, piece ->
                        val isSelected = idx in currentOrder
                        val orderNum = if (isSelected) currentOrder.indexOf(idx) + 1 else 0
                        val bgColor = when {
                            showResult -> {
                                val actualIdx = correctOrder[idx]
                                val expectedOrder = puzzlePieces.indices.toList().indexOf(actualIdx)
                                val userPosition = currentOrder.indexOf(idx)
                                if (userPosition == expectedOrder && isCorrect) Color(0xFF4CAF50)
                                else if (isSelected) Color(0xFFF44336)
                                else Color.White
                            }
                            isSelected -> Color(0xFFFFC107)
                            else -> Color.White
                        }
                        val textColor = when {
                            showResult && (isSelected) -> Color.White
                            else -> Color(0xFF5D4037)
                        }
                        Button(
                            onClick = { selectPiece(idx) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = bgColor,
                                contentColor = textColor
                            ),
                            border = BorderStroke(
                                2.dp,
                                Color(0xFF009688).copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Text(
                                        text = "$orderNum",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = piece,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 操作按钮
                if (!showResult) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { currentOrder = emptyList() },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = currentOrder.isNotEmpty()
                        ) {
                            Text("清除", fontSize = 14.sp)
                        }
                        Button(
                            onClick = {
                                // 跳过
                                showResult = true
                                isCorrect = false
                                errorMessage = "已跳过"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF9E9E9E)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("跳过", fontSize = 14.sp)
                        }
                    }
                } else {
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
                                text = if (isCorrect) "✓ 顺序正确！+12分"
                                else "✗ 顺序不对哦",
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
