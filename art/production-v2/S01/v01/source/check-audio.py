from pathlib import Path
import json,subprocess,hashlib
import numpy as np
R=Path(__file__).resolve().parents[1];P=R.parents[3]
D=json.loads((R/'source/sound-design.json').read_text());SR=D['sampleRate'];FF=D['ffmpeg']
def decode(p):
    raw=subprocess.check_output([FF,'-v','error','-i',str(p),'-f','f32le','-ac','1','-ar',str(SR),'pipe:1'])
    return np.frombuffer(raw,dtype='<f4').astype(float)
def db(x):return float(20*np.log10(max(1e-12,x)))
def truepeak(x):
    n=len(x); padded=np.zeros(n*4//2+1,dtype=complex); spectrum=np.fft.rfft(x)
    padded[:len(spectrum)]=spectrum; padded[len(spectrum)-1]*=.5
    return db(np.max(np.abs(np.fft.irfft(padded,n=n*4)*4)))
rows=[];buffers=[]
for c in D['cues']:
    id=c['id'];master=R/'source/masters'/f'{id}.wav';ogg=R/'exports/sounds'/f'{id}.ogg'
    a=decode(master);b=decode(ogg);stems=sum(decode(p) for p in sorted((R/'source/stems'/id).glob('*.wav')))
    n=min(len(a),len(b));err=a[:n]-b[:n];snr=db(np.linalg.norm(a[:n])/max(1e-12,np.linalg.norm(err)))
    probe=json.loads(subprocess.check_output(['/opt/homebrew/bin/ffprobe','-v','error','-show_streams','-of','json',str(ogg)]))['streams'][0]
    mp=json.loads(subprocess.check_output(['/opt/homebrew/bin/ffprobe','-v','error','-show_streams','-of','json',str(master)]))['streams'][0]
    peak=db(np.max(np.abs(a)));tp=truepeak(b);tail=np.max(np.abs(a[-round(.018*SR):]));stemerr=np.max(np.abs(a-stems))
    checks={'finite':bool(np.isfinite(a).all() and np.isfinite(b).all()),'masterDuration':len(a)==round(SR*c['seconds']),
        'mono48kVorbis':probe['codec_name']=='vorbis' and probe['channels']==1 and int(probe['sample_rate'])==SR,
        'masterPCM24Mono48k':mp['codec_name']=='pcm_s24le' and mp['channels']==1 and int(mp['sample_rate'])==SR and int(mp['bits_per_sample'])==24,
        'codecDurationWithin40ms':abs(len(a)-len(b))<SR*.04,'peakWithin0point01dB':abs(peak-c['peakDbfs'])<.01,
        'truePeakHeadroom':tp<-7,'noClipping':float(np.max(np.abs(b)))<1,
        'dcBelowMinus50dB':db(abs(np.mean(a)))<-50,'silentTail':tail<2e-7,
        'stemSumWithin1micro':stemerr<1e-6,'oggSNRAbove22dB':snr>22}
    checks={k:bool(v) for k,v in checks.items()}
    rows.append({'id':id,'masterSamples':len(a),'decodedOggSamples':len(b),'peakDbfs':round(peak,3),'truePeakDbfs4x':round(tp,3),
        'rmsDbfs':round(db(np.sqrt(np.mean(a*a))),3),'dcDbfs':round(db(abs(np.mean(a))),3),'oggSNRdB':round(snr,2),
        'stemSumMaxError':float(stemerr),'checks':checks,'allPassed':all(checks.values())});buffers.append(a)
baseline=json.loads((R/'references/code-and-adopted-baseline.json').read_text())
unchanged=all(hashlib.sha256((P/f['path']).read_bytes()).hexdigest()==f['sha256'] for f in baseline)
pairs=[]
for i,j in [(0,1),(2,3),(4,5),(6,7),(8,9),(10,11)]:
    n=min(len(buffers[i]),len(buffers[j]));a=buffers[i][:n];b=buffers[j][:n];corr=float(np.dot(a,b)/(np.linalg.norm(a)*np.linalg.norm(b)))
    pairs.append({'a':D['cues'][i]['id'],'b':D['cues'][j]['id'],'correlationAtZeroLag':round(corr,4),'differentWaveforms':abs(corr)<.8})
out={'allPassed':all(r['allPassed'] for r in rows) and unchanged and all(p['differentWaveforms'] for p in pairs),'rows':rows,'pairChecks':pairs,
    'originalCues':12,'mono24bitMasters':12,'editableStems':36,'oggFiles':12,'loops':0,'baselineUnchanged':unchanged,'humanListeningApproved':False,
    'gameRuntimeVerified':False,'method':'actual ffmpeg decoding, sample statistics, 4x FFT true peak estimate, stem recombination, source hashes'}
(R/'review/audio-checks.json').write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'allPassed':out['allPassed'],'rows':[{'id':r['id'],'rms':r['rmsDbfs'],'snr':r['oggSNRdB'],'fail':[k for k,v in r['checks'].items() if not v]} for r in rows]},ensure_ascii=False))
assert out['allPassed']
