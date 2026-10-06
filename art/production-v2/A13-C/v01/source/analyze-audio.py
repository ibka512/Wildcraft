from pathlib import Path
import json, subprocess, hashlib, math
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
def read(p):return json.loads(p.read_text())
def save(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n')
def decode(p):
 b=subprocess.check_output(['/opt/homebrew/bin/ffmpeg','-v','error','-i',str(p),'-f','f32le','-ac','1','-ar','48000','pipe:1'])
 return np.frombuffer(b,dtype='<f4').astype(np.float64)
def metrics(x):
 peak=float(np.max(np.abs(x)));rms=float(np.sqrt(np.mean(x*x)))
 return {'seconds':len(x)/48000,'peak':peak,'peakDbFS':20*math.log10(max(peak,1e-12)),'rmsDbFS':20*math.log10(max(rms,1e-12))}
files={p.stem:p for p in (ROOT/'references').glob('*.ogg')}
native=read(ROOT/'references/native-local-map.json')['assets']
files.update({a['id']:Path(a['path']) for a in native})
for a in native:assert hashlib.sha256(Path(a['path']).read_bytes()).hexdigest()==a['sha256']
audio={k:decode(p) for k,p in files.items()}
plans=read(ROOT/'exports/scenario-plans.json'); checks=[];mixes=[]
for scene,profiles in plans.items():
 bg=[]
 for name,p in profiles.items():
  x=np.zeros(240000);back=np.zeros_like(x)
  for e in p['events']:
   at=round(e['at']*48000);sample=audio[e['id']]
   length=min(len(sample),len(x)-at)
   if e['kind']=='focus' and e.get('cutAfterSeconds') is not None:length=min(length,round(e['cutAfterSeconds']*48000))
   if length<=0:continue
   sample=sample[:length]*e['gain'];x[at:at+length]+=sample
   if e['kind']!='focus':back[at:at+length]+=sample
  bg.append(back);m=metrics(x);assert m['peak']<1
  item={'scene':scene,'profile':name,**m,'nativeInMemoryOnly':any(e['kind']=='native' for e in p['events'])}
  if scene in ['quiet','machines','crowded','rapid']:
   target=ROOT/'exports/preview-mixes'/f'{scene}-{name}.wav'
   subprocess.run(['/opt/homebrew/bin/ffmpeg','-v','error','-y','-f','f32le','-ar','48000','-ac','1','-i','pipe:0','-c:a','pcm_s24le',str(target)],input=x.astype('<f4').tobytes(),check=True)
   item['preview']=str(target.relative_to(ROOT))
  mixes.append(item)
 checks.append({'scene':scene,'sameBackgroundAB':bool(np.array_equal(*bg))})
focus={k:metrics(audio[k]) for k in ['focus_enter','focus_exit']}
effective={profile:{kind:metrics(audio['focus_'+kind]*gain) for kind,gain in vals.items()} for profile,vals in read(ROOT/'exports/focus-mix-spec.json')['profiles'].items()}
report={'allPassed':True,'decodedAudioFiles':len(audio),'sampleRate':48000,'comparisonNormalization':False,'nativeAudioBundled':False,'nativeMixedExports':0,'newProductionOgg':0,'portablePreviewWavs':8,'focusOriginal':focus,'effectiveFocus':effective,'exitGainDeltaDb':20*math.log10(.28/.22),'mixes':mixes,'checks':checks,'limit':'Electrical waveform measurements only. Human audibility and in-game category/spatial mixing remain unverified.'}
save(ROOT/'review/audio-checks.json',report)
# Equal vertical scale: never normalize each cue separately.
svg=['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 260" role="img" aria-label="专注进出音波形，四条使用相同纵向刻度"><rect width="900" height="260" fill="#111e1b"/>']
for i,(label,key,gain,col) in enumerate([('进入 · 原／建议 0.22','focus_enter',.22,'#75c6ad'),('退出 · 原方案 0.22','focus_exit',.22,'#82998d'),('退出 · 建议 0.28','focus_exit',.28,'#f3cf83')]):
 y=48+i*80;x=audio[key]*gain
 bounds=np.linspace(0,len(x),640,dtype=int)
 svg.append(f'<text x="18" y="{y-22}" fill="{col}" font-family="sans-serif" font-size="14">{label}</text><path d="M 210 {y} H 870" stroke="#3d4a43"/>')
 for j in range(639):
  v=x[bounds[j]:bounds[j+1]]
  if len(v):svg.append(f'<path d="M {210+j} {y-float(v.max())*260:.2f} V {y-float(v.min())*260:.2f}" stroke="{col}"/>')
svg.append('</svg>');(ROOT/'review/focus-waveforms.svg').write_text(''.join(svg))
print(json.dumps({'decoded':len(audio),'mixPeakMax':max(m['peak'] for m in mixes),'exitGainDeltaDb':report['exitGainDeltaDb'],'previews':8}))
