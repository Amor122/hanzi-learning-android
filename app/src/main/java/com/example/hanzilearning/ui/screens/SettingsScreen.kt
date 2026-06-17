package com.example.hanzilearning.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hanzilearning.data.*
import com.example.hanzilearning.ui.*
import com.example.hanzilearning.util.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    userPrefs: UserPreferences,
    progressDb: ProgressDatabaseHelper
) {
    val scrollState = rememberScrollState()
    var userName by remember { mutableStateOf(userPrefs.userName) }
    var currentLevel by remember { mutableStateOf(userPrefs.currentLevel) }
    var dailyGoal by remember { mutableStateOf(userPrefs.dailyGoal) }
    var soundEnabled by remember { mutableStateOf(userPrefs.soundEnabled) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    val favorites = remember { userPrefs.getFavorites() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8E1))
            .verticalScroll(scrollState)
    ) {
        TopBar(title = "设置", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 用户信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "👤",
                        fontSize = 60.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (userName.isNotEmpty()) userName else "学习者",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3E2723)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showNameDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("修改昵称", fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 年级选择
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
                        .padding(20.dp)
                ) {
                    Text(
                        text = "学习年级",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "选择年级后，学习和游戏将使用对应难度的汉字",
                        fontSize = 14.sp,
                        color = Color(0xFF888888)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    GradeSelectorRow(
                        selectedLevel = currentLevel,
                        onLevelSelected = {
                            currentLevel = it
                            userPrefs.currentLevel = it
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 每日目标设置
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
                        .padding(20.dp)
                ) {
                    Text(
                        text = "每日学习目标",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "每天计划学习多少个汉字？",
                        fontSize = 14.sp,
                        color = Color(0xFF888888)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(5, 10, 20, 30, 50).forEach { goal ->
                            Button(
                                onClick = {
                                    dailyGoal = goal
                                    userPrefs.dailyGoal = goal
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (dailyGoal == goal) Color(0xFFFF9800)
                                    else Color(0xFFFF9800).copy(alpha = 0.2f),
                                    contentColor = if (dailyGoal == goal) Color.White
                                    else Color(0xFF5D4037)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("$goal", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 声音设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "语音朗读",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5D4037)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "学习时自动播放语音提示",
                            fontSize = 14.sp,
                            color = Color(0xFF888888)
                        )
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            userPrefs.soundEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF2196F3),
                            checkedTrackColor = Color(0xFF2196F3).copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 收藏列表
            if (favorites.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "我的收藏 (${favorites.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5D4037)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            favorites.take(10).forEach { char ->
                                Text(
                                    text = char,
                                    fontSize = 32.sp,
                                    color = Color(0xFFE91E63),
                                    modifier = Modifier.clickable {
                                        userPrefs.removeFavorite(char)
                                    }
                                )
                            }
                        }
                        Text(
                            text = "点击汉字可取消收藏",
                            fontSize = 11.sp,
                            color = Color(0xFF888888)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 最高分记录
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF3E5F5)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "🏆 最佳成绩",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        HighScoreItem(
                            title = "听音选字",
                            score = userPrefs.highScoreListen,
                            color = Color(0xFF4CAF50)
                        )
                        HighScoreItem(
                            title = "看字选音",
                            score = userPrefs.highScorePick,
                            color = Color(0xFF9C27B0)
                        )
                        HighScoreItem(
                            title = "笔顺拼图",
                            score = userPrefs.highScoreStroke,
                            color = Color(0xFF009688)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 清除数据
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFAFAFA)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "数据管理",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("清除所有学习记录", color = Color(0xFFF44336))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 版本信息
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "汉字学习 v1.0",
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // 修改昵称对话框
    if (showNameDialog) {
        var inputName by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("修改昵称", fontSize = 18.sp) },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("请输入昵称") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    userPrefs.userName = inputName
                    userName = inputName
                    showNameDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 清除数据对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清除", fontSize = 18.sp) },
            text = {
                Text("确定要清除所有学习记录吗？此操作不可撤销。", color = Color(0xFFF44336))
            },
            confirmButton = {
                TextButton(onClick = {
                    progressDb.clearAllProgress()
                    userPrefs.resetAll()
                    showClearDialog = false
                }) {
                    Text("确定清除", color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun GradeSelectorRow(selectedLevel: Int, onLevelSelected: (Int) -> Unit) {
    val levelNames = mapOf(
        1 to "一年级",
        2 to "二年级",
        3 to "三年级",
        4 to "四年级",
        5 to "五年级"
    )
    val levelColors = mapOf(
        1 to Color(0xFF4CAF50),
        2 to Color(0xFF2196F3),
        3 to Color(0xFF9C27B0),
        4 to Color(0xFFFF9800),
        5 to Color(0xFFF44336)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        (1..5).forEach { level ->
            val isSelected = selectedLevel == level
            Button(
                onClick = { onLevelSelected(level) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) levelColors[level]!!
                    else Color.Transparent,
                    contentColor = if (isSelected) Color.White else levelColors[level]!!
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${levelNames[level]}",
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun HighScoreItem(title: String, score: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = "$score",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color(0xFF795548)
        )
    }
}
