import glob, os, re, sys
src = os.path.dirname(os.path.abspath(__file__)) + '/aihelp_src'
files = sorted(glob.glob(src + '/*.md'))
text = '\n'.join(open(f, encoding='utf-8').read().rstrip() + '\n' for f in files)
# split into chunks < 18000 chars at blank lines
chunks, cur = [], ''
for para in text.split('\n\n'):
    add = para + '\n\n'
    if len(cur) + len(add) > 18000:
        chunks.append(cur); cur = ''
    cur += add
if cur: chunks.append(cur)
def lit(s):
    lines = s.split('\n')
    out = []
    for i, l in enumerate(lines):
        e = l.replace('\\', '\\\\').replace('"', '\\"')
        end = '\\n' if i < len(lines) - 1 else ''
        out.append('        "' + e + end + '"')
    return ' +\n'.join(out)
j = ['package com.zue.attackeditor;', '',
     '/** Texto fixo do TBRHelp.md (gerado a partir de aihelp_src/*.md); os {{MARCADORES}} sao preenchidos por {@link AiHelp}. Nao editar a mao. */',
     'final class AiHelpDoc {', '    private AiHelpDoc() { }', '', '    static final String[] PARTS = {']
for c in chunks:
    j.append(lit(c) + ',')
j.append('    };')
j.append('}')
open(sys.argv[1], 'w', encoding='utf-8').write('\n'.join(j) + '\n')
print(len(text), 'chars', len(chunks), 'chunks')
