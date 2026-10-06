"""Reuse adopted audio and allowlist local game-cache audition references."""
from pathlib import Path
import json,hashlib,shutil,csv
R=Path(__file__).resolve().parents[1];P=R.parents[3]
for folder in ['source','references','exports/preview-mixes','review']:(R/folder).mkdir(parents=True,exist_ok=True)
def dump(p,d):p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
inputs=[
 'art/approved-v1/A13-专注进入退出音效-v1/source/focus-enter-master.wav',
 'art/approved-v1/A13-专注进入退出音效-v1/source/focus-exit-master.wav',
 'art/approved-v1/A13-专注进入退出音效-v1/deliverable/focus_enter.ogg',
 'art/approved-v1/A13-专注进入退出音效-v1/deliverable/focus_exit.ogg',
 'art/approved-v1/A13-专注进入退出音效-v1/deliverable/focus-sound-spec.json',
 'art/production-v2/S01/v01/adoption.json',
 'art/production-v2/S01/v01/exports/playback-spec.json',
 'art/production-v2/S01/v01/source/audio-policy.js',
 'art/production-v2/A07-A08-C/v01/adoption.json',
 'art/production-v2/A07-A08-C/A07-A08-C-探索动作衔接复核-v01-审稿包.zip',
 'src/client/java/dev/wildcraft/client/focus/FocusClient.java',
 'src/main/java/dev/wildcraft/registry/WildcraftSounds.java',
 'src/main/resources/assets/wildcraft/sounds.json',
 'src/main/resources/assets/wildcraft/sounds/focus_enter.ogg',
 'src/main/resources/assets/wildcraft/sounds/focus_exit.ogg',
 'src/main/generated/assets/wildcraft/lang/zh_cn.json',
 'src/main/generated/assets/wildcraft/lang/en_us.json',
 'development-assets/wildcraft-0.1.0-dev.14+mc26.3.jar',
]+['art/production-v2/S01/v01/exports/sounds/'+p.name for p in sorted((P/'art/production-v2/S01/v01/exports/sounds').glob('*.ogg'))]
dump(R/'references/baseline.json',[{'path':p,'sha256':hashlib.sha256((P/p).read_bytes()).hexdigest()}for p in inputs])
for src,dst in [
 ('art/approved-v1/A13-专注进入退出音效-v1/deliverable/focus-sound-spec.json','adopted-focus-spec.json'),
 ('art/production-v2/S01/v01/exports/playback-spec.json','adopted-s01-playback.json'),
 ('art/production-v2/S01/v01/source/sound-design.json','adopted-s01-design.json'),
 ('art/production-v2/S01/v01/source/audio-policy.js','s01-policy.js'),
]:shutil.copyfile(P/src,R/'references'/dst)
for id in ['focus_enter','focus_exit']:
 shutil.copyfile(P/'src/main/resources/assets/wildcraft/sounds'/f'{id}.ogg',R/'references'/f'{id}.ogg')
 assert (R/'references'/f'{id}.ogg').read_bytes()==(P/'art/approved-v1/A13-专注进入退出音效-v1/deliverable'/f'{id}.ogg').read_bytes()
for src,dst in [('focus-enter-master.wav','focus-enter-master.wav'),('focus-exit-master.wav','focus-exit-master.wav')]:shutil.copyfile(P/'art/approved-v1/A13-专注进入退出音效-v1/source'/src,R/'references'/dst)
for p in (P/'art/production-v2/S01/v01/exports/sounds').glob('*.ogg'):shutil.copyfile(p,R/'references'/p.name)
index=Path('/Volumes/仕事/Wildcraft/开发环境/cache/gradle/caches/fabric-loom/assets/indexes/26.3-34.json');assets=json.loads(index.read_text())['objects']
selected={'rain1':'ambient/weather/rain1','rain2':'ambient/weather/rain2','snow1':'step/snow1','snow2':'step/snow2','bow':'random/bow','bow_hit':'random/bowhit1','hit':'damage/hit1','strong':'entity/player/attack/strong1'}
native=[]
for id,name in selected.items():
 key='minecraft/sounds/'+name+'.ogg';h=assets[key]['hash'];path=index.parent.parent/'objects'/h[:2]/h
 assert path.exists()and hashlib.sha1(path.read_bytes()).hexdigest()==h
 native.append({'id':id,'assetKey':key,'hash':h,'path':str(path),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'bundled':False,'url':'/native/'+id+'.ogg'})
dump(R/'references/native-local-map.json',{'index':str(index),'indexSha256':hashlib.sha256(index.read_bytes()).hexdigest(),'referenceOnly':True,'redistribute':False,'assets':native})
spec={'asset':'A13-C','version':'v01','date':'2026-10-06','adopted':False,'integrated':False,'gameRuntimeVerified':False,
 'focusFiles':'reuse original OGG and masters unchanged; no new production sound files',
 'profiles':{'current':{'enter':.22,'exit':.22},'proposed':{'enter':.22,'exit':.28}},
 'recommendedProfile':'proposed','pitch':1,'category':'UI (verified local Minecraft26.3 SimpleSoundInstance.forUI)','spatialAttenuation':False,'loop':False,
 'focusVoices':1,'focusReplacement':'retain existing immediate stop of previous focus cue; no pending queue, repeat cooldown or restart on duplicate active sample',
 's01Budget':'reuse adopted6 total/2 same event/1 per source, distance and priority unchanged; focus is a separate one-voice UI cue',
 'otherAudio':'retain native combat, weather and subtitles; no gameplay-side ducking, EQ, master/category override or soundtrack',
 'transportStopRampMs':6,'transportScope':'audition player only; not a proposed gameplay change',
 'referenceSeconds':5,'enterAt':1,'exitAt':3,'referenceMasterGain':1,
 'scenarios':[
  {'id':'quiet','title':'安静环境','note':'原进出音量与退出补偿直接对照'},
  {'id':'rain','title':'雨中探索','note':'原版雨声在本机缓存读取；固定参考音量'},
  {'id':'snow','title':'雪地行走','note':'原版雪地脚步参照，不制作持续降雪声'},
  {'id':'combat','title':'射箭与受击','note':'弓、命中、受击、强攻击同场，原版事件保留'},
  {'id':'machines','title':'机械／能源并存','note':'复用S01机械、充电及制造短音，非新增循环声'},
  {'id':'crowded','title':'六声预算与专注','note':'八个S01事件先裁为六声，专注单声仍独立'},
  {'id':'rain_combat','title':'雨中战斗与机械','note':'最拥挤的组合参照；不以数值断言人耳辨识已通过'},
  {'id':'rapid','title':'快速进出专注','note':'0.12秒间隔切换，停止上一段；重复状态不重复提示'},
 ]}
dump(R/'exports/focus-mix-spec.json',spec)
with (R/'exports/focus-volume-map.csv').open('w',encoding='utf-8-sig',newline='')as f:
 w=csv.writer(f);w.writerow(['事件','原音量','建议音量','文件处理','用途']);w.writerow(['focus.enter',.22,.22,'原OGG不改','保留原进入提示']);w.writerow(['focus.exit',.22,.28,'原OGG不改','补偿较低原声电平，待用户试听采用'])
native_source=Path('/Volumes/仕事/Wildcraft/开发环境/cache/art-native-source/SimpleSoundInstance.java')
assert 'SoundSource.UI' in native_source.read_text()
dump(R/'review/source-audit.json',{'category':'UI','categoryEvidencePath':str(native_source),'categoryEvidenceSha256':hashlib.sha256(native_source.read_bytes()).hexdigest(),'currentEnterVolume':.22,'currentExitVolume':.22,'previousFocusStoppedBeforeReplacement':True,'pauseMenuDeadSuppressed':True,'disconnectResetStopsCue':True,'scope':'local source inspection; not a new game recording'})
q=P/'art/production-docs/ART-DESIGN-PRODUCTION-SCHEDULE-2026-10-04.csv'
with q.open(encoding='utf-8-sig',newline='')as f:r=csv.DictReader(f);fields=r.fieldnames;rows=list(r)
for row in rows:
 if row['编号']=='A13-C':row['制作阶段状态']='v01制作中；原专注双音/S01/原版雨雪战斗叠加复核，待试听未接入'
with q.open('w',encoding='utf-8-sig',newline='')as f:w=csv.DictWriter(f,fieldnames=fields);w.writeheader();w.writerows(rows)
print(json.dumps({'scenarios':8,'focusOriginalFiles':2,'reusedS01':12,'localNativeReferences':8,'baselineFiles':len(inputs),'newProductionAudio':0}))
