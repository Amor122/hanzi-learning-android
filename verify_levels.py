import re

with open('D:/AS_Projects/hanziLearning/app/src/main/java/com/example/hanzilearning/data/CommonCharacters.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 提取每级的字
levels = {}
for i in range(1, 6):
    m = re.search(r'private val level' + str(i) + r'Chars = listOf\(([\s\S]+?)\n    \)', content)
    if not m:
        print('level' + str(i) + ' NOT FOUND')
        continue
    chars = re.findall(r'"([^"]+)"', m.group(1))
    # 去重（同级内）
    seen_lv = []
    dup_in_lv = 0
    for c in chars:
        if len(c) != 1:
            print('  multi-char: ' + c)
        if c in seen_lv:
            dup_in_lv += 1
        else:
            seen_lv.append(c)
    levels[i] = seen_lv
    print('Level ' + str(i) + ': ' + str(len(seen_lv)) + ' chars (internal dupes: ' + str(dup_in_lv) + ')')

# 跨级重复检查
total = set()
cross_dupes = []
for lvl, lst in levels.items():
    for c in lst:
        if c in total:
            cross_dupes.append((c, lvl))
        total.add(c)
print('cross-level dupes:', len(cross_dupes))
if cross_dupes:
    print('examples:', cross_dupes[:10])

# 检查拼音和字义
pm = re.search(r'private val pinyinMap = mapOf\(([\s\S]+?)\n    \)', content)
pinyin_pairs = re.findall(r'"([^"]+)" to "([^"]*)"', pm.group(1))
print('pinyin entries:', len(pinyin_pairs))
py_empty = sum(1 for c, py in pinyin_pairs if not py)
print('pinyin empty:', py_empty)

mm = re.search(r'private val meaningMap = mapOf\(([\s\S]+?)\n    \)', content)
meaning_pairs = re.findall(r'"([^"]+)" to "([^"]*)"', mm.group(1))
print('meaning entries:', len(meaning_pairs))
m_empty = sum(1 for c, m in meaning_pairs if not m)
print('meaning empty:', m_empty)
print('total unique chars:', len(total))
