from pathlib import Path
import json,hashlib,re,base64,subprocess,urllib.request,urllib.error
R=Path(__file__).resolve().parents[1];P=R.parents[3]
load=lambda p:json.loads(p.read_text())
hashfile=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
checks=[]
def check(name,ok,details=None):
 checks.append({'name':name,'passed':bool(ok),'details':details})
 if not ok:raise AssertionError(name)
baseline=load(R/'references/baseline.json')
check('Thirty protected originals unchanged',len(baseline)==30 and all(hashfile(P/b['path'])==b['sha256'] for b in baseline))
check('Original focus OGG unchanged',all((R/'references'/f'focus_{k}.ogg').read_bytes()==(P/'src/main/resources/assets/wildcraft/sounds'/f'focus_{k}.ogg').read_bytes() for k in ['enter','exit']))
check('Original focus masters unchanged',all((R/'references'/f'focus-{k}-master.wav').read_bytes()==(P/'art/approved-v1/A13-专注进入退出音效-v1/source'/f'focus-{k}-master.wav').read_bytes() for k in ['enter','exit']))
check('Twelve S01 OGG unchanged',all((R/'references'/p.name).read_bytes()==p.read_bytes() for p in (P/'art/production-v2/S01/v01/exports/sounds').glob('*.ogg')))
a=load(R/'references/adopted-s01-design.json');b=load(R/'references/adopted-s01-playback.json')
check('Historical design cue values match adopted S01',all(any(x['id']==c['id'] and x['volume']==c['gameVolume'] and x['distanceBlocks']==c['maxDistance'] and x['priority']==c['priority'] for x in b['distanceAndVolumes']) for c in a['cues']))
html=(R/'review/preview.html').read_text();match=re.search(r'const AUDIO=(.*?);</script>',html);audio=json.loads(match.group(1));check('Fourteen original own OGG embedded',len(audio)==14 and all(base64.b64decode(v)==(R/'references'/f'{k}.ogg').read_bytes() for k,v in audio.items()))
native=load(R/'references/native-local-map.json')['assets'];allhashes=[hashfile(p) for p in R.rglob('*') if p.is_file()]
check('No native audio files bundled',not any(n['sha256'] in allhashes for n in native))
check('No native embedded audio',all(base64.b64encode(Path(n['path']).read_bytes()).decode() not in html for n in native))
check('Only own portable mixed exports',sorted(p.name for p in (R/'exports/preview-mixes').glob('*'))==sorted(f'{s}-{p}.wav' for s in ['quiet','machines','crowded','rapid'] for p in ['current','proposed']))
codec=[]
for p in (R/'references').glob('*.ogg'):
 info=json.loads(subprocess.check_output(['/opt/homebrew/bin/ffprobe','-v','error','-show_streams','-of','json',str(p)]))['streams'][0]
 codec.append({'id':p.stem,'codec':info['codec_name'],'sampleRate':info['sample_rate'],'channels':info['channels']})
check('Own OGG codec mono48k verified',all(x['codec']=='vorbis' and x['sampleRate']=='48000' and x['channels']==1 for x in codec),codec)
for i,s in enumerate(re.findall(r'<script>(.*?)</script>',html,re.S)):
 p=Path('/tmp')/f'a13-final-{i}.js';p.write_text(s)
 check(f'Inline script{i} syntax',subprocess.run(['/Users/zhou/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node','--check',str(p)],capture_output=True).returncode==0)
check('Policy verified',load(R/'review/policy-checks.json')['allPassed'])
check('Audio decoded and reference mixes not clipping',load(R/'review/audio-checks.json')['allPassed'] and max(x['peak'] for x in load(R/'review/audio-checks.json')['mixes'])<1)
check('Browser scheduling verified',load(R/'review/browser-checks.json')['allPassed'])
sup=load(R/'review/browser-supplement.json')
check('Unavailable native fails without playback',sup['failure']['stopped'] and sup['failure']['nodes']==0 and not sup['failure']['loading'])
check('Actual WebAudio dry and crowded output',sup['dry']['signal']>0 and sup['priorMixedSignal']>0 and sup['priorNaturalEndNodes']==0)
check('Mobile layout no horizontal overflow',not sup['layout']['overflow'])
for route in ['/native/bow.ogg','/native/snow1.ogg','/preview.html']:
 with urllib.request.urlopen('http://127.0.0.1:8830'+route) as response:check('Allowed server route '+route,response.status==200)
for route in ['/native/../references/baseline.json','/references/native-local-map.json','/etc/passwd']:
 try:urllib.request.urlopen('http://127.0.0.1:8830'+route);ok=False
 except urllib.error.HTTPError as e:ok=e.code==404
 check('Unlisted server route rejected '+route,ok)
check('Draft and not integrated',not load(R/'exports/focus-mix-spec.json')['adopted'] and not load(R/'exports/focus-mix-spec.json')['integrated'])
report={'allPassed':all(x['passed'] for x in checks),'checks':checks,'protectedFiles':len(baseline),'newProductionSounds':0,'humanListeningVerified':False,'gameRuntimeVerified':False}
(R/'review/delivery-checks.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'deliveryChecks':len(checks),'allPassed':report['allPassed'],'protectedOriginals':30}))
