from pathlib import Path
import json,hashlib,zipfile,csv
R=Path(__file__).resolve().parents[1];P=R.parents[3]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def dump(p,d):p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
D=json.loads((R/'source/sound-design.json').read_text());C=D['cues']
audio=json.loads((R/'review/audio-checks.json').read_text());browser=json.loads((R/'review/browser-checks.json').read_text());policy=json.loads((R/'review/policy-checks.json').read_text())
assert audio['allPassed'] and browser['allPassed'] and policy['allPassed']
assert len(C)==12 and len(list((R/'exports/sounds').glob('*.ogg')))==12
assert len(list((R/'source/masters').glob('*.wav')))==12 and len(list((R/'source/stems').glob('*/*.wav')))==36
assert not list(R.rglob('*.png')) and not list(R.rglob('*.aseprite')) and not (R/'adoption.json').exists()
for f in json.loads((R/'references/code-and-adopted-baseline.json').read_text()):assert sha(P/f['path'])==f['sha256']
A=P/'art/production-v2/F03-04/v01'
assert json.loads((A/'adoption.json').read_text())['adopted']
assert sha(A.parent/'F03-04-特殊材料及融合过程反馈-v01-审稿包.zip')=='1e219ef7afe12fcecad617be1adb426b9dcb8303d59324c8d3ceaebe223ec20b'
assert sha(P/'development-assets/wildcraft-0.1.0-dev.14+mc26.3.jar')=='c81ce6d7a960d67c3cd2125a2c426a62216e3b5f784c8e2b99d8afc6792a91ac'
for id in ['focus_enter','focus_exit']:assert sha(R/'references'/f'{id}.ogg')==sha(P/'src/main/resources/assets/wildcraft/sounds'/f'{id}.ogg')
html=(R/'review/preview.html').read_text()
for name in ['audio-policy.js','audio-review.js']:assert (R/'source'/name).read_text() in html
assert json.dumps(D,ensure_ascii=False) in html
with (R/'exports/event-map.csv').open(encoding='utf-8-sig') as f:events=list(csv.DictReader(f))
assert len(events)==12 and all(int(r['优先级'])==c['priority'] for r,c in zip(events,C))
proposed=json.loads((R/'exports/sounds-additions-proposal.json').read_text());assert len(proposed)==12 and not any(k.startswith('focus.') for k in proposed)
spec=json.loads((R/'exports/playback-spec.json').read_text());assert not spec['loop'] and not spec['integrated'] and not spec['adopted']
assert all(c['priority']==s['priority'] and c['gameVolume']==s['volume'] for c,s in zip(C,spec['distanceAndVolumes']))
queue=P/'art/production-docs/ART-DESIGN-PRODUCTION-SCHEDULE-2026-10-04.csv'
with queue.open(encoding='utf-8-sig',newline='') as f:r=csv.DictReader(f);fields=r.fieldnames;rows=list(r)
assert next(row for row in rows if row['编号']=='A09-C')['制作阶段状态']=='计划未启动'
assert not (P/'art/production-v2/A09-C').exists()
for row in rows:
    if row['编号']=='S01':row['制作阶段状态']='v01草稿交付；12OGG/12母带/36分层与试听/事件规范齐全，待试听采用，未接入'
with queue.open('w',encoding='utf-8-sig',newline='') as f:w=csv.DictWriter(f,fieldnames=fields);w.writeheader();w.writerows(rows)
entry='| S01 音效配套 | v01 | 2026-10-06草稿待试听采用；12OGG/12母带/36分层及事件/距离/去重规范齐全，0循环；未接入 | [音效试听与文件](S01/v01/README.md) |\n'
index=P/'art/production-v2/README.md';text=index.read_text()
if entry not in text:
    marker='后续资产按';at=text.index(marker);text=text[:at]+entry+'\n'+text[at:];index.write_text(text)
for rel,link in [('art/README.md','production-v2/S01/v01/README.md'),('art/production-docs/README.md','../production-v2/S01/v01/README.md')]:
    p=P/rel;line=f'\n2026-10-06：[S01音效配套v01]({link})草稿已交付，12原创短音/12母带/36分层、便携试听与事件规范齐全，0循环，待用户试听采用，未接入；原专注音和历史采用包保持。\n'
    if line not in p.read_text():p.write_text(p.read_text()+line)
verification=f'''# S01 v01 验证记录

2026-10-06。验证文件导出、审稿播放与独立准入参考；没有人工听觉采用，也没有启动游戏做混音测试。

- 12 OGG实际解码为48kHz单声道Vorbis；12 WAV核对PCM24、48kHz与单声道；时长0.18–0.48秒。
- 母带峰值 −11.50～−9.00 dBFS；OGG四倍FFT插值峰值估计 −11.659～−8.925 dBFS。无削波、非有限数、异常DC；母带尾部18ms静音。
- OGG与母带比较SNR 29.16～34.34dB；编码尾部允许40ms以内填充差异，不误判成音频截断。
- 36层PCM24相加重建12母带，最大样本误差2.3842e−7；六组成对声音的波形相关检查区分通过。波形区分不等于听觉语义被用户认可。
- 独立审稿准入规则{policy['cases']}项通过：未确认、重复包、距离、六声/两声/单来源上限、优先级替换、冷却、来源移除、缓存界限及重连清理。
- 实际浏览器{browser['cases']}项通过：12单音真实播放、14段OGG解码、整组顺序、声音停止、四台同启裁为两声、八事件最多六声、超距静音、卸载与键盘停止、原专注对照、390px与桌面无横向溢出、零播放器错误。
- 基线18文件哈希复核保持：当前玩法Java、语言、声音资源、正式JAR与原专注母带、F03-04采用记录和历史ZIP未改；未更改数据、网络与依赖。A09-C仍未制作。

详细数字：[audio-checks.json](review/audio-checks.json)、[policy-checks.json](review/policy-checks.json)、[browser-checks.json](review/browser-checks.json)、[打包检查](review/package-checks.json)。

听觉音色和辨识由本次试听审稿决定；Minecraft实际空间声、方块遮挡、环境声及战斗/专注并存、暂停和多人客户端事件接入留待游戏验证。页面声音输出检测只能证明播放链路工作，不能代替人耳聆听或全游戏混音。
'''
(R/'verification.md').write_text(verification)
checks={'asset':'S01','allPassed':True,'adopted':False,'integrated':False,'gameRuntimeVerified':False,'originalOgg':12,'masters':12,'editableStems':36,'reusedFocusReferences':2,'loops':0,'newImagesOrModels':0,'audioExportChecksPassed':True,'policyCases':policy['cases'],'actualBrowserCases':browser['cases'],'previousAssetAdopted':True,'previousZipUnchanged':True,'codeLocalesJarAndFocusUnchanged':True,'embeddedSourceConfigAndAudioExact':True,'nextAssetUntouched':True}
# Confirm embedded audio is exact, not a stale proxy.
import base64,re
embedded=json.loads(re.search(r'const AUDIO=(\{.*?\});\n',html).group(1))
for c in C:assert base64.b64decode(embedded[c['id']])==(R/'exports/sounds'/f"{c['id']}.ogg").read_bytes()
dump(R/'review/package-checks.json',checks)
m={**checks,'version':'v01','date':'2026-10-06','status':'draft_for_listening_review','humanListeningApproved':False,'nextAfterAdoption':'A09-C 披风/鞘翅下的侧腰摆放','files':[{'path':str(p.relative_to(R)),'bytes':p.stat().st_size,'sha256':sha(p)} for p in sorted(R.rglob('*')) if p.is_file() and p.name!='manifest.json' and '__pycache__' not in p.parts]};dump(R/'manifest.json',m)
archive=R.parent/'S01-音效配套-v01-审稿包.zip';prefix='S01-音效配套-v01/'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=7) as z:
    for f in m['files']:z.write(R/f['path'],prefix+f['path'])
    z.write(R/'manifest.json',prefix+'manifest.json')
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    for f in m['files']:assert hashlib.sha256(z.read(prefix+f['path'])).hexdigest()==f['sha256']
dump(R.parent/'S01-v01-打包校验.json',{'asset':'S01','allPassed':True,'zipBytes':archive.stat().st_size,'zipSha256':sha(archive),'manifestSha256':sha(R/'manifest.json'),'zipEntries':len(m['files'])+1,'zipCRCAndFileHashesVerified':True})
print(json.dumps({'allPassed':True,'files':len(m['files']),'zipBytes':archive.stat().st_size,'archive':str(archive),'adopted':False,'integrated':False},ensure_ascii=False))
