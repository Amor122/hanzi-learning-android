# 汉字学习 App

一款面向小学生的汉字学习应用，支持读音学习、笔画学习、小游戏巩固等功能。

## 功能特性

### 学习模块
- **读音学习**：展示汉字的拼音、释义，配合跟读录音与评分反馈
- **笔画学习**：演示汉字书写顺序，支持描红练习
- **进度追踪**：记录每日学习数量、已掌握字数、最高分等数据

### 小游戏
- **听音选字**：听发音从四个字中选择正确汉字
- **选字拼音**：看汉字选择正确拼音
- **笔画拼图**：按笔画顺序拼出完整汉字

### 分级系统
内置 884 个常用汉字，按难度分为 5 个级别：

| 级别 | 字数 | 难度 |
|------|------|------|
| 一年级 | 203 | 基础（数字、人体部位、日常动词） |
| 二年级 | 184 | 入门（颜色、形状、常见动词） |
| 三年级 | 189 | 中等（抽象概念、进阶动词） |
| 四年级 | 203 | 较难（书面语、研究动作） |
| 五年级 | 105 | 进阶（科技、社会、教育类） |

**温故知新**：高年级会混入低年级字复习，比例如下：
- 1-2 年级：100% 本级字
- 3 年级：80% 本级 + 20% 低级
- 4 年级：70% 本级 + 30% 低级
- 5 年级：60% 本级 + 40% 低级

## 技术架构

- **语言**：Kotlin
- **UI 框架**：Jetpack Compose
- **本地存储**：SharedPreferences（用户设置）、Room（学习进度）
- **音频**：MediaPlayer（发音播放）、MediaRecorder（跟读录音）

## 项目结构

```
app/src/main/java/com/example/hanzilearning/
├── data/
│   ├── CharacterData.kt       # 汉字数据结构
│   ├── CommonCharacters.kt    # 分级字库（884字）
│   └── AppDatabase.kt         # Room 数据库
├── ui/
│   ├── MainAppScreen.kt       # 主界面导航
│   ├── screens/
│   │   ├── PronunciationLearnScreen.kt   # 读音学习
│   │   ├── StrokeLearnScreen.kt          # 笔画学习
│   │   ├── ProgressScreen.kt            # 学习进度
│   │   ├── SettingsScreen.kt            # 设置
│   │   ├── ListenAndPickGame.kt         # 听音选字
│   │   ├── CharAndPickPinyinGame.kt      # 选字拼音
│   │   └── StrokePuzzleGame.kt           # 笔画拼图
│   └── components/
│       ├── StrokeOrderView.kt   # 笔画演示组件
│       └── TracingCanvas.kt     # 描红画布
└── util/
    ├── AudioRecorder.kt         # 录音工具
    ├── TextToSpeechHelper.kt   # TTS 发音
    └── UserPreferences.kt      # 用户设置管理
```

## 字库管理工具

项目根目录提供两个 Python 脚本用于维护字库：

- `gen_clean_levels.py`：自动生成干净的 CommonCharacters.kt，去除重复字、补全拼音释义
- `verify_levels.py`：验证分级是否有重复字、拼音/释义是否完整

```bash
python gen_clean_levels.py   # 重新生成字库文件
python verify_levels.py      # 验证字库完整性
```
