import re

with open('D:/AS_Projects/hanziLearning/app/src/main/java/com/example/hanzilearning/data/CommonCharacters.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 剩余缺意义的字
m_missing = {
    '示': '表示，示意', '发': '发现，发展', '打': '打击，敲打',
    '首': '首先，首部', '成': '成功，完成', '就': '成就，就是',
    '负': '负担，负责', '赏': '欣赏，赞赏', '政': '政治，政府',
    '完': '完成，完毕', '寻': '寻找，寻求', '肃': '严肃，肃静',
    '凡': '平凡，凡是', '别': '区别，特别', '异': '异同，差异'
}
py_missing = {
    '示': 'shì', '发': 'fā', '打': 'dǎ', '首': 'shǒu',
    '成': 'chéng', '就': 'jiù', '负': 'fù', '赏': 'shǎng',
    '政': 'zhèng', '完': 'wán', '寻': 'xún', '肃': 'sù',
    '凡': 'fán', '别': 'bié', '异': 'yì'
}

# 替换
def replace_map(section_name, extra):
    global content
    m = re.search(r'private val ' + section_name + r' = mapOf\(([\s\S]+?)\n    \)', content)
    if not m:
        print(section_name + ' NOT FOUND')
        return
    body = m.group(1)
    new_body = body
    for c, v in extra.items():
        old = '"' + c + '" to ""'
        new = '"' + c + '" to "' + v + '"'
        if old in new_body:
            new_body = new_body.replace(old, new)
        else:
            # 尝试匹配非空的已有条目（仅当意义为空白时）
            pass
    content = content.replace('private val ' + section_name + ' = mapOf(' + body, 'private val ' + section_name + ' = mapOf(' + new_body)

replace_map('meaningMap', m_missing)
replace_map('pinyinMap', py_missing)

with open('D:/AS_Projects/hanziLearning/app/src/main/java/com/example/hanzilearning/data/CommonCharacters.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print('done')
