"""Read-only exported PNG inspection + SVG review document (no pixel asset edit)."""
from pathlib import Path
from PIL import Image
import html
ROOT=Path(__file__).resolve().parents[1]
W,H=1040,1040
out=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">', '<rect width="1040" height="1040" fill="#151d1b"/>']
def text(x,y,t,size=16,color='#dce3d9'):
 out.append(f'<text x="{x}" y="{y}" font-family="PingFang SC,Arial,sans-serif" font-size="{size}" fill="{color}">{html.escape(t)}</text>')
def rect(x,y,w,h,c,r=0):out.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{c}"/>')
def pixels(p,x,y,k):
 im=Image.open(p).convert('RGBA')
 for j in range(im.height):
  for i in range(im.width):
   r,g,b,a=im.getpixel((i,j))
   if a:rect(x+i*k,y+j*k,k,k,f'#{r:02x}{g:02x}{b:02x}')
text(28,30,'WILDCRAFT • F01-02 • v01',12,'#c9a875');text(28,69,'四类料理物品外观',28)
text(28,99,'同一木碗 · 原生像素制作 · 32 / 16 独立适配 · 草稿待审，未接入游戏',15,'#a5b6a9')
labels=[('vegetable','普通蔬菜','胡萝卜 / 土豆'),('warming','保暖料理','甜菜红汤 / 土豆 / 短蒸汽'),('cooling','耐热料理','苹果切面 / 西瓜与瓜皮'),('recovery','精力恢复','红蘑菇白斑 / 菌柄 / 糖点')]
for i,(kind,label,sub) in enumerate(labels):
 x=28+i*250;rect(x,122,234,570,'#202a25',12)
 text(x+16,154,label,21);text(x+16,180,sub,12,'#adbaa7')
 text(x+16,210,'32 × 32  /  6倍',13,'#d6c09b');rect(x+21,223,192,192,'#e7e2cf',5);pixels(ROOT/'exports'/f'meal-{kind}-32.png',x+21,223,6)
 text(x+16,450,'16 × 16  /  12倍',13,'#d6c09b');rect(x+21,462,192,192,'#e7e2cf',5);pixels(ROOT/'exports'/f'meal-{kind}-16.png',x+21,462,12)
 # Exact native sprite size and two-times preview, dark background.
 pixels(ROOT/'exports'/f'meal-{kind}-32.png',x+22,702,1);pixels(ROOT/'exports'/f'meal-{kind}-16.png',x+67,710,1)
 pixels(ROOT/'exports'/f'meal-{kind}-32.png',x+105,694,2);pixels(ROOT/'exports'/f'meal-{kind}-16.png',x+185,710,2)
text(28,777,'下方：原始尺寸与2倍显示。上方放大仅以整数方格呈现，不重采样原件。',14,'#b3c1b5')
text(28,803,'料理等级I / II共用外观；本次交付8份PNG + 8份Aseprite源稿。效果HUD图标继续使用已采用版本。',13,'#a5b6a9')
text(28,844,'深色背景对照 · 32px与16px均为4倍显示',14,'#b3c1b5')
for i,(kind,label,_) in enumerate(labels):
 x=28+i*250;rect(x,856,234,158,'#0b1410',10);pixels(ROOT/'exports'/f'meal-{kind}-32.png',x+12,862,4);pixels(ROOT/'exports'/f'meal-{kind}-16.png',x+155,891,4);text(x+16,1000,label,13,'#a5b6a9')
out.append('</svg>');(ROOT/'review/meal-family-review.svg').write_text('\n'.join(out))
print('SVG审稿文档已生成，实际PNG像素逐格展示。')
