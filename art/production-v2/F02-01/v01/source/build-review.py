"""Build a self-contained temperature review by reusing the adopted meal HUD template.
Only read the older template; do not execute it or overwrite its adopted outputs.
"""
from pathlib import Path
import ast,json,base64
ROOT=Path(__file__).resolve().parents[1]
OLD=ROOT.parents[1]/'F01-03/v01'
tree=ast.parse((OLD/'source/build-review.py').read_text())
html=next(n.value.value for n in tree.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='html' for t in n.targets) and isinstance(n.value,ast.Constant))
def uri(p):return 'data:image/png;base64,'+base64.b64encode(p.read_bytes()).decode()
assets={key:uri(ROOT/'references'/f'{stem}-16.png') for key,stem in [('warmth','warmth-meal'),('cooling','cooling-meal'),('recovery','stamina-recovery')]}
assets.update(stamina=uri(ROOT/'references/stamina-hud-atlas.png'),temperature=uri(ROOT/'exports/ambient-temperature-color-16.png'),temperature32=uri(ROOT/'exports/ambient-temperature-color-32.png'))
html=html.replace('F01-03','F02-01').replace('Wildcraft 料理效果 HUD · v01 审稿','Wildcraft 七档温度 HUD · v01 审稿').replace('<h1>料理效果 HUD 排布</h1>','<h1>七档环境温度 HUD</h1>').replace('沿用已采用的保暖、耐热、精力恢复图标。下方是布局模拟，字体近似；不是实机截图。','温度计沿用上暖下冷v2待审稿，七档亮框与文字表示当前读数。料理HUD复用刚采用布局。下方是布局模拟，字体近似，不是实机截图。')
html=html.replace('<label>语言<select', '<label>温度档位<select id="band"></select></label><label><input id="edge" type="checkbox" checked>轻边缘色调</label><label>语言<select')
html=html.replace('<section class="stage" aria-label="可切换的HUD布局模拟">','<section class="stage" aria-label="温度与料理联合HUD布局模拟">')
start=html.index('<div class="notes">');end=html.index('<h2>状态对照',start)
html=html[:start]+'''<div class="notes"><div class="note"><h2>上暖下冷 · 当前档位单独标记</h2><p>温度计保留静态冷热配色；旁边七格从上方极热到下方极冷。亮框、指向标记与文字共同指出当前档位，不把蓝色底泡当作实时读数。</p></div><div class="note"><h2>环境信息 · 料理负责辅助</h2><p>不显示摄氏度，不增加普通冷热伤害、减速或精力惩罚。保暖料理辅助细雪冻结；耐热料理不表示抗火或岩浆免疫。</p></div><div class="note"><h2>与已采用料理HUD衔接</h2><p>温度16px组件放在原文字锚点上方3px。下方料理仍从原锚点+14开始，图标与两列文字保留；精力隐藏时两组一起上移24px。</p></div></div>''' + html[end:]
html=html.replace('温度暂用现有七格占位；下一项再审温度图标。红心 / 吸收心为位置示意；精力条用已接入纹理。底部经验与快捷栏只标预留区域。图标原件及源稿保持原哈希，本次布局未采用、未接入游戏。','温度图标与新七档组件待采用；料理图标和排布已采用。红心/吸收心、景物、底部区域为示意，字体近似。边缘色调沿现有4px低透明度规则，专注时关闭；未接入游戏。')
html=html.replace('__MODEL__',(ROOT/'references/adopted-meal-hud-layout.js').read_text()+'\n'+(ROOT/'source/temperature-hud.js').read_text()).replace('__ASSETS__',json.dumps(assets))
html=html.replace('const M=WildcraftMealHud,imgs={};','const M=WildcraftMealHud,T=WildcraftTemperatureHud,imgs={};')
html=html.replace('let state={...M.presets.normal};','let state={...M.presets.normal,band:3,edge:true};')
# Preserve every adopted meal position and draw routine; replace only the old temperature placeholder.
a=html.index(" c.fillStyle='rgba(21,35,27,.44)';c.fillRect(10,l.temperatureY-2");b=html.index('\n if(l.fits)',a)
html=html[:a]+" drawTemperature(c,s,l);"+html[b:]
helper='''function drawTemperature(c,s,l){
 const q=T.layout(s,l);c.globalAlpha=1;c.fillStyle='rgba(21,35,27,.44)';c.fillRect(q.box.x,q.box.y,q.box.width,q.box.height);c.drawImage(imgs.temperature,q.iconX,q.iconY,16,16);
 for(const z of q.gauge){c.globalAlpha=z.active?1:.58;c.fillStyle=z.color;c.fillRect(z.x,z.y,z.width,z.height);}c.globalAlpha=1;
 const a=q.marker;c.fillStyle='#eff0d6';c.fillRect(a.x,a.y,a.width,1);c.fillRect(a.x,a.y+2,a.width,1);c.fillRect(a.x,a.y+1,1,1);c.fillRect(a.x+6,a.y+1,1,1);c.fillRect(39,a.y+1,1,1);c.fillRect(40,a.y,1,1);c.fillRect(40,a.y+2,1,1);
 label(c,q.label,q.labelX,q.labelY,q.color);return q;
}
function drawEdges(c,s){const z=T.edge(s);if(z.visible){c.globalAlpha=z.alpha/255;c.fillStyle=z.color;c.fillRect(0,0,4,s.h);c.fillRect(s.w-4,0,4,s.h);}c.globalAlpha=1;}
'''
html=html.replace('function draw(canvas,s,scale){',helper+'\nfunction draw(canvas,s,scale){').replace('backdrop(c,s);hearts(c,s);','backdrop(c,s);drawEdges(c,s);hearts(c,s);')
html=html.replace('window.lastLayout=l;','window.lastLayout=l;window.lastTemperature=T.layout(state,l);window.lastEdge=T.edge(state);')
html=html.replace('state={...M.presets[key],levels:',"state={...M.presets[key],band:Number(document.querySelector('#band').value),edge:document.querySelector('#edge').checked,levels:")
html=html.replace("for(const [k,s] of Object.entries(M.presets)){let o=new Option", "for(let i=6;i>=0;i--){const o=new Option(T.states[i].zh,String(i));document.querySelector('#band').add(o);}document.querySelector('#band').value='3';\nfor(const [k,s] of Object.entries(M.presets)){let o=new Option")
html=html.replace("document.querySelector('#scenario').onchange", "document.querySelector('#band').onchange=e=>{state.band=Number(e.target.value);update();};document.querySelector('#edge').onchange=e=>{state.edge=e.target.checked;update();};\ndocument.querySelector('#scenario').onchange")
html=html.replace("`${state.title} · ${state.w}×${state.h}","`${T.states[state.band][state.language]} · ${state.title} · ${state.w}×${state.h}")
html=html.replace("let l=draw(cv,s,1);","let l=draw(cv,{...s,band:3,edge:true},1);")
html=html.replace('window.reviewReady=true;','update();window.reviewReady=true;')
(ROOT/'review/preview.html').write_text(html)
print('温度七档与采用料理HUD联合交互预览已生成。')
