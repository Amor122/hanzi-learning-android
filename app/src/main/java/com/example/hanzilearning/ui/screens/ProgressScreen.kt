package com.example.hanzilearning.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hanzilearning.data.*
import com.example.hanzilearning.ui.*
import com.example.hanzilearning.util.*

@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    userPrefs: UserPreferences,
    progressDb: ProgressDatabaseHelper
) {
    val scrollState = rememberScrollState()
    val totalChars = CommonCharacters.totalCount()
    val masteredCount = remember { progressDb.getMasteredCount() }
    val totalPractices = remember { progressDb.getTotalPracticeCount() }
    val todayStats = remember { progressDb.getTodayStats() }
    val weeklyStats = remember { progressDb.getWeeklyStats() }
    val dailyGoal = userPrefs.dailyGoal

    // 计算总体进度
    val overallProgress = if (totalChars > 0) (masteredCount * 100f / totalChars) else 0f
    val todayProgress = if (dailyGoal > 0) (todayStats.charactersLearned * 100f / dailyGoal) else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8E1))
            .verticalScroll(scrollState)
    ) {
        TopBar(title = "学习进度", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 今日进度大卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFF9800)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "今日目标",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${todayStats.charactersLearned} / $dailyGoal 字",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 进度条
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(todayProgress.coerceIn(0f, 100f) / 100f)
                                .background(Color.White)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${todayProgress.toInt()}% 完成",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 统计网格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard2(
                    title = "总掌握",
                    value = "$masteredCount",
                    subValue = "/ $totalChars 字",
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                StatCard2(
                    title = "总练习",
                    value = "$totalPractices",
                    subValue = "次",
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard2(
                    title = "今日得分",
                    value = "${todayStats.totalScore}",
                    subValue = "分",
                    color = Color(0xFF9C27B0),
                    modifier = Modifier.weight(1f)
                )
                StatCard2(
                    title = "今日练习",
                    value = "${todayStats.totalPractices}",
                    subValue = "次",
                    color = Color(0xFF009688),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 本周学习趋势
            SectionTitle2("本周学习趋势")
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    WeeklyChart(weeklyStats = weeklyStats)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val days = listOf("一", "二", "三", "四", "五", "六", "日")
                        days.forEach {
                            Text(
                                text = it,
                                fontSize = 12.sp,
                                color = Color(0xFF795548)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 总体进度环
            SectionTitle2("总体掌握进度")
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProgressRing(
                        progress = overallProgress,
                        size = 180.dp,
                        strokeWidth = 18.dp,
                        color = Color(0xFFE91E63)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${overallProgress.toInt()}%",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE91E63)
                            )
                            Text(
                                text = "已掌握",
                                fontSize = 14.sp,
                                color = Color(0xFF795548)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "继续加油，每天学习一点点！",
                        fontSize = 14.sp,
                        color = Color(0xFF795548)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 最近学习
            val recentChars = remember { progressDb.getRecentCharacters(6) }
            if (recentChars.isNotEmpty()) {
                SectionTitle2("最近学习")
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            recentChars.take(6).forEach { progress ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = progress.character,
                                        fontSize = 28.sp,
                                        color = Color(0xFF3E2723)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${progress.practiceCount}次",
                                        fontSize = 11.sp,
                                        color = Color(0xFF888888)
                                    )
                                    if (progress.mastered) {
                                        Text(
                                            text = "✓",
                                            fontSize = 12.sp,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SectionTitle2(text: String) {
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
    }
}

@Composable
fun StatCard2(
    title: String,
    value: String,
    subValue: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = subValue,
                    fontSize = 12.sp,
                    color = color,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color(0xFF5D4037)
            )
        }
    }
}

@Composable
fun WeeklyChart(weeklyStats: List<com.example.hanzilearning.data.DailyStats>) {
    val maxVal = weeklyStats.maxOfOrNull { it.charactersLearned } ?: 1
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val width = size.width
        val height = size.height
        val barWidth = width / weeklyStats.size * 0.6f
        val spacing = width / weeklyStats.size * 0.4f

        weeklyStats.forEachIndexed { index, stats ->
            val barHeight = if (maxVal > 0) {
                (stats.charactersLearned / maxVal.toFloat()) * height * 0.9f
            } else 0f
            val left = index * width / weeklyStats.size + spacing / 2
            val top = height - barHeight

            // 绘制柱子
            drawRoundRect(
                color = Color(0xFFFF9800),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )

            // 数值
            // 文字会很复杂，用简单的Text代替
        }
    }

    // 在图表顶部显示数值
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weeklyStats.forEach { stats ->
            Text(
                text = "${stats.charactersLearned}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF5D4037)
            )
        }
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    size: androidx.compose.ui.unit.Dp,
    strokeWidth: androidx.compose.ui.unit.Dp,
    color: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            // 背景圆
            drawCircle(
                color = color.copy(alpha = 0.15f),
                radius = size.toPx() / 2 - strokeWidth.toPx() / 2,
                style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth.toPx())
            )

            // 进度圆弧
            val sweepAngle = (progress / 100f) * 360f
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    strokeWidth.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                ),
                size = androidx.compose.ui.geometry.Size(
                    width = size.toPx() - strokeWidth.toPx(),
                    height = size.toPx() - strokeWidth.toPx()
                ),
                topLeft = Offset(
                    strokeWidth.toPx() / 2,
                    strokeWidth.toPx() / 2
                )
            )
        }
        content()
    }
}
