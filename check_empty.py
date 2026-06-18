import re

with open('D:/AS_Projects/hanziLearning/app/src/main/java/com/example/hanzilearning/data/CommonCharacters.kt', 'r', encoding='utf-8') as f:
    content = f.read()

pm = re.search(r'private val pinyinMap = mapOf\(([\s\S]+?)\n    \)', content)
pinyin_dict = dict(re.findall(r'"([^"]+)" to "([^"]*)"', pm.group(1)))

mm = re.search(r'private val meaningMap = mapOf\(([\s\S]+?)\n    \)', content)
meaning_dict = dict(re.findall(r'"([^"]+)" to "([^"]*)"', mm.group(1)))

empty_py = [c for c, py in pinyin_dict.items() if not py]
empty_m = [c for c, m in meaning_dict.items() if not m]
print('chars without pinyin (' + str(len(empty_py)) + '):', empty_py)
print('chars without meaning (' + str(len(empty_m)) + '):', empty_m)

# 检查哪些字是在 level 4 和 level 5 被新添加的
for i in range(1, 6):
    m = re.search(r'private val level' + str(i) + r'Chars = listOf\(([\s\S]+?)\n    \)', content)
    chars = re.findall(r'"([^"]+)"', m.group(1))
    empty_in_lv = [c for c in chars if not pinyin_dict.get(c, '')]
    print('Level ' + str(i) + ' empty pinyin: ' + str(len(empty_in_lv)) + ' chars')
