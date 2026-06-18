import re
from collections import OrderedDict

# 读取现有文件以抽取 pinyinMap / meaningMap 数据
with open('D:/AS_Projects/hanziLearning/app/src/main/java/com/example/hanzilearning/data/CommonCharacters.kt', 'r', encoding='utf-8') as f:
    content = f.read()

pm_match = re.search(r'private val pinyinMap\s*=\s*mapOf\(([\s\S]*?)\n\s*\)', content)
if not pm_match:
    print('pinyinMap NOT FOUND')
    exit(1)
pinyin_dict = dict(re.findall(r'"([^"]+)"\s*to\s*"([^"]*)"', pm_match.group(1)))
print('pinyin entries:', len(pinyin_dict))

mm_match = re.search(r'private val meaningMap\s*=\s*mapOf\(([\s\S]*?)\n\s*\)', content)
if not mm_match:
    print('meaningMap NOT FOUND')
    exit(1)
meaning_dict = dict(re.findall(r'"([^"]+)"\s*to\s*"([^"]*)"', mm_match.group(1)))
print('meaning entries:', len(meaning_dict))

# ===== 五级别字定义（按严格顺序组织，一级最基础；跨级去重）=====
level1 = ' '.join([
    '一 二 三 四 五 六 七 八 九 十',
    '百 千 万 零',
    '大 小 多 少 高 矮 长 短 宽 窄',
    '厚 薄 胖 瘦 美 快 慢 冷 热 温',
    '凉 暖 硬 软 轻 重 远 近 深 浅',
    '早 晚 先 后',
    '上 下 左 右 前 里 外 中 东 西',
    '南 北',
    '人 口 目 耳 手 足 头 心 身 子',
    '女 男 爸 妈 哥 姐 弟 妹 爷 奶',
    '我 你 他 她 们',
    '的 是 有 在 不 了 这 那 个 和',
    '与 及',
    '来 去 进 出 回 到 往 走 跑 跳',
    '飞 看 听 说 读 写 想 做 吃 喝',
    '唱 笑 哭 爱 好 坏 对 错 非',
    '新 旧 老',
    '今 明 昨 天 日 月 年 时 分 秒',
    '点 钟',
    '春 夏 秋 冬 风 云 雨 雪 雷 电',
    '水 火 山 石 田 土 木 树 林 森',
    '花 草 叶 根 果 瓜 米 面 饭 菜',
    '肉 鱼 鸡 鸭 鹅 鸟 马 牛 羊 猪',
    '狗 猫 兔 虎 龙 蛇 虫 贝',
    '车 船 路 桥 门 窗 房 屋 家 楼',
    '书 本 笔 纸'
]).split()

level2 = ' '.join([
    '白 黑 红 黄 蓝 绿 紫 粉 灰 棕',
    '色 颜 彩 亮 暗 光',
    '圆 方 角 边 体 形 状 尖 平 直',
    '弯 曲 斜 歪',
    '数 量 加 减 乘 除 等 号',
    '字 词 句 文 章 页 题 答 问 念 思',
    '考 记 忘 认 识 知 道 学 习 教 师',
    '生 活 死 病 医 药 院 所',
    '工 作 业 事 情 况 物 品',
    '国 乡 村 镇 城 市 省 区 县 街 巷',
    '店 铺 商 场 买 卖 钱 币 银 金',
    '铜 铁 钢 铝',
    '布 丝 线 绳 带 包 袋 箱 盒 盘',
    '碗 杯 瓶 罐 锅',
    '刀 叉 勺 筷 碟 盆 桶',
    '床 桌 椅 凳 柜 架 灯',
    '夜 晨 午 昏 星 期 周 季 节 假',
    '间 候 久 暂 停 止 始 终 结 束',
    '开 关 启',
    '动 静 声 音 响 乐 歌 舞 蹈 画',
    '图 片 像 照 相 影 视',
    '闻 嗅 尝 味 香 臭 甜 苦 酸 辣',
    '咸 淡 浓 清'
]).split()

level3 = ' '.join([
    '净 脏 乱 整 齐 稳 安 全 危 险',
    '胜 败 赢 输 功 绩',
    '因 原 故 由 理 于 以 此 彼 哪',
    '谁 什 么 怎 为 何 必 须 应 该',
    '当 可 能 够 会 懂',
    '楚 模 糊 含 混 杂 简 单 复 繁',
    '琐 细 微 粗 壮 巨 庞 硕 广 阔',
    '狭 遥 旁 沿 着 朝 向',
    '退 撤 入 归 返 转 移 搬 挪 迁',
    '改 变 更 换 替 代 交 易 难 困',
    '纯 洁 真 实 存 现 虚 假 空 满',
    '充 用 使 利 采 运 适 专 通 共',
    '同 起 享 受 遭 遇 承 忍 接 经',
    '历 过 穿 越 超 任 责 务',
    '义 公 正 直 德 行 法 规 则 定',
    '律 准 标 划 程 流 课',
    '式 办 处 置 放 搁 摆 陈 设 计',
    '建 立 造 制 订 拟 度 约 限 管',
    '控 支 配 匹 搭 调 排 部 署 顿',
    '抚 慰 宁 息 然 保 闲 逸 神 魂',
    '眠 睡 梦 养 机 气 力'
]).split()

level4 = ' '.join([
    '研 究 钻 熟 虑 索 维 绪 段 针',
    '略 策 位 便',
    '诗 语 讲 话 谈 论 议 讨 争 辩',
    '解 释 阐 述 叙',
    '表 示 显 露 揭 达 抵 传 递 送',
    '给 付 发',
    '拿 取 抓 捉 握 持 执 掌 捏 提',
    '拎 举 抬 扛',
    '推 拉 拖 拽 扯 撕 拆 卸 拢 聚',
    '集 合 措 施 阻 拦 挡 碍 妨',
    '攻 击 打 敲 伤 害 损 毁 灭 消',
    '除 失 丢 掉 落',
    '留 护 防 备 预',
    '录 登 载 抄 印 刷 扫',
    '互 遍 序 第 首 初 末 尾',
    '重 再 又 也 还 仍 依 旧',
    '阅 览 诵 背 朗 翻 译 参 验 成',
    '就 曾 已 虽 但 而 却 致 使 如',
    '果 假 设 若 否 则 即 让 令 于',
    '至 关 系 相 介 绍 注 视 专 志',
    '尽 努 负 担 承 认 办 纳',
    '鉴 赏 游 访 赞 政 军 战 谋 议',
    '齐 洁 理 完 美 毕 整 天 个 点',
    '数 据 值 码'
]).split()

# Level 5: 真正较深/书面/五年级常用字，确保与上面不重复
level5_extra_candidates = '''
航 舰 察 析 测 监 督 统 府 治 领
社 团 织 科 践 聪 智 慧 勤 礼 貌
容 感 态 标 准 划 程 组 员 队 部
首 领 导 办 理 处 置 决 定 规 则
纪 律 法 制 度 计 划 设 施 检 查
盘 算 推 敲 指 挥 控 制 调 节 操 作
建 筑 设 计 创 造 发 明 发 现 发 展
制 订 拟 定 修 订 改 进 创 新 历 史
经 济 政 治 文 化 科 学 技 术 教 育
环 境 保 护 资 源 能 源 原 因 原 理
原 则 原 因 根 源 来 源 出 处 根 据
依 据 证 明 证 据 显 示 表 示 表 现
代 表 示 范 典 型 案 例 事 实 真 相
本 质 实 质 性 质 特 点 特 色 特 征
普 通 一 般 平 凡 个 性 各 种 样
'''.split()

levels_raw = [level1, level2, level3, level4, level5_extra_candidates]

# 逐字收集，按级严格去重（首次出现的级别保留）
seen = OrderedDict()
clean_levels = [[], [], [], [], []]
for level_idx, chars in enumerate(levels_raw):
    for c in chars:
        if len(c) != 1:
            continue
        if c in seen:
            continue
        seen[c] = True
        clean_levels[level_idx].append(c)

# 打印统计
for i, lv in enumerate(clean_levels):
    print('Level', i + 1, ':', len(lv), '字')
print('total:', sum(len(x) for x in clean_levels))

# 最后再补一批额外的 Level 4/5 字，让数量更多一些
extra_pools = [
    # 偏书面/抽象（级别稍高）
    '布 局 模 式 系 列 版 本 阶 段',
    '流 程 方 法 步 骤 策 略 技 巧',
    '重 点 要 点 难 点 关 键 核 心',
    '因 素 元 素 基 础 根 基 本 质',
    '内 容 外 形 外 部 内 部',
    # 更偏教育/抽象概念
    '原 因 结 果 效 果 结 论',
    '过 程 进 程 进 度 进 步',
    '发 展 开 展 进 行 举 办 组 织',
    '安 排 准 备 计 划 打 算',
    '实 现 达 到 实 施 执 行 落 实',
    '完 成 成 功 取 得 获 得',
    '赢 得 取 胜 失 败 挫 折 经 历',
    '体 验 感 受 感 觉 觉 得',
    '认 识 知 道 了 解 懂 得 理 解',
    '掌 握 学 会 运 用 使 用',
    '解 决 问 题 处 理 对 付',
    '面 对 面 临 应 对 处 理',
    '考 虑 思 考 思 索 想 法 主 张',
    '观 点 意 见 看 法 建 议 提 议',
    '创 意 创 造 创 新 发 明',
    '设 想 假 设 预 测 预 期',
    '愿 望 希 望 梦 想 追 求 寻 求',
    '寻 找 搜 索 探 索 探 讨 研 讨',
    '研 究 研 讨 研 究 课 题',
    '专 题 调 查 调 研 分 析',
    '综 合 整 合 组 合 结 合',
    '比 较 对 比 对 照 区 分',
    '区 别 差 异 不 同 相 似',
    '相 同 类 似 相 当 等 同',
    '普 遍 广 泛 普 及 普 通',
    '特 别 特 殊 特 有 独 特',
    '唯 一 独 一 无 二 单 独',
    '各 个 每 个 分 别 逐 个',
    '整 个 全 部 全 体 整 体',
    '总 体 整 个 大 部 分 部 份',
    '一 部 分 一 半 半 个 一 个',
    '两 个 三 个 几 个 多 个',
    '多 数 少 数 半 数 全 部',
    '主 要 重 要 紧 要 关 键',
    '首 要 必 要 必 须 需 要',
    '应 该 应 当 需 要 要 求',
    '请 求 寻 求 追 求 期 待',
    '盼 望 渴 望 希 望 愿 望',
    '诚 实 真 诚 坦 率 直 率',
    '认 真 负 责 谨 慎 仔 细',
    '粗 心 大 意 马 虎 简 单',
    '容 易 轻 易 轻 松 松 散',
    '严 格 严 肃 严 厉 认 真',
    '勤 奋 勤 劳 刻 苦 努 力',
    '刻 苦 认 真 专 心 用 心',
    '专 注 注 意 留 心 小 心',
    '谨 慎 谨 严 严 格 严 肃'
]
extra_chars = list(dict.fromkeys([c for c in ' '.join(extra_pools).split() if len(c) == 1]))
# 把这些补到 Level 4 与 Level 5（按顺序，未出现的）
add_to_level4 = []
add_to_level5 = []
for c in extra_chars:
    if c in seen:
        continue
    seen[c] = True
    # 交替分配给 level 4 和 level 5
    if (len(add_to_level4) + len(add_to_level5)) % 2 == 0:
        add_to_level4.append(c)
    else:
        add_to_level5.append(c)

clean_levels[3].extend(add_to_level4)
clean_levels[4].extend(add_to_level5)

print('after enrich:')
for i, lv in enumerate(clean_levels):
    print('Level', i + 1, ':', len(lv))
print('total:', sum(len(x) for x in clean_levels))

# 校验跨级重复
check = set()
dup_count = 0
for lv in clean_levels:
    for c in lv:
        if c in check:
            dup_count += 1
        check.add(c)
print('cross-level dupes:', dup_count)

# 收集所有字符，用于拼音/字义映射
all_chars = []
for lv in clean_levels:
    all_chars.extend(lv)

# 生成 Kotlin 源码
def esc(s):
    return s.replace('\\', '\\\\').replace('"', '\\"')

def fmt_chars(chars, indent='        '):
    lines = []
    for i in range(0, len(chars), 15):
        chunk = chars[i:i+15]
        lines.append(indent + ', '.join('"' + esc(c) + '"' for c in chunk) + ',')
    return '\n'.join(lines)

out = []
out.append('package com.example.hanzilearning.data')
out.append('')
out.append('import com.example.hanzilearning.data.CharacterData')
out.append('')
out.append('// 常用汉字分级（按一~五年级严格分，每级字与其他级不重复）')
out.append('// Level 1: 最基础字；Level 5: 最复杂/书面语')
out.append('// 使用 getByLevel(level) 获取该级字列表')
out.append('object CommonCharacters {')
out.append('')

# 字列表
for i, lv in enumerate(clean_levels):
    out.append('    // ============ Level ' + str(i + 1) + ' (' + str(len(lv)) + ' 字) ============')
    out.append('    private val level' + str(i + 1) + 'Chars = listOf(')
    out.append(fmt_chars(lv, indent='        '))
    out.append('    )')
    out.append('')

# 拼音 map
out.append('    // ============ 拼音映射（级别越高越需要完整拼音）============')
out.append('    private val pinyinMap = mapOf(')
entries = []
for c in all_chars:
    py = pinyin_dict.get(c, '')
    entries.append('"' + esc(c) + '" to "' + esc(py) + '"')
for i in range(0, len(entries), 8):
    chunk = entries[i:i+8]
    out.append('        ' + ', '.join(chunk) + ',')
out.append('    )')
out.append('')

# 字义 map
out.append('    // ============ 简短字义映射 ============')
out.append('    private val meaningMap = mapOf(')
entries = []
for c in all_chars:
    m = meaning_dict.get(c, '')
    entries.append('"' + esc(c) + '" to "' + esc(m) + '"')
for i in range(0, len(entries), 8):
    chunk = entries[i:i+8]
    out.append('        ' + ', '.join(chunk) + ',')
out.append('    )')
out.append('')

# 工具函数
out.append('    // ============ 工具函数：按级别字列表转换为 CharacterData ============')
out.append('    private fun toDataList(level: Int, chars: List<String>): List<CharacterData> {')
out.append('        val seen = mutableSetOf<String>()')
out.append('        val result = mutableListOf<CharacterData>()')
out.append('        for (c in chars) {')
out.append('            if (seen.contains(c)) continue')
out.append('            seen.add(c)')
out.append('            val py = pinyinMap[c] ?: ""')
out.append('            val m = meaningMap[c] ?: ""')
out.append('            result.add(')
out.append('                CharacterData(character = c, pinyin = py, meaning = m, level = level, strokeCount = 0, fullMeaning = m)')
out.append('            )')
out.append('        }')
out.append('        return result')
out.append('    }')
out.append('')

for i in range(1, 6):
    out.append('    private val level' + str(i) + 'Data by lazy { toDataList(' + str(i) + ', level' + str(i) + 'Chars) }')
out.append('')

out.append('    /**')
out.append('     * 根据级别返回汉字列表。级别 1~5 对应 一~五年级。')
out.append('     * 每级的字与其他级严格不重复。')
out.append('     */')
out.append('    fun getByLevel(level: Int): List<CharacterData> {')
out.append('        return when (level) {')
for i in range(1, 6):
    out.append('            ' + str(i) + ' -> level' + str(i) + 'Data')
out.append('            else -> level1Data')
out.append('        }')
out.append('    }')
out.append('')
out.append('    /** 返回全部字（1~5 级合并），用于跟读学习等场景。 */')
out.append('    fun getAll(): List<CharacterData> {')
out.append('        return level1Data + level2Data + level3Data + level4Data + level5Data')
out.append('    }')
out.append('}')

with open('D:/AS_Projects/hanziLearning/app/src/main/java/com/example/hanzilearning/data/CommonCharacters.kt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(out) + '\n')
print('written.')
