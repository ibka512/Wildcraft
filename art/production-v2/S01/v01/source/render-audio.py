"""S01 original procedural sound project. Edit sound-design.json, then render.
Requires the existing NumPy runtime and FFmpeg; imports no samples.
"""
from pathlib import Path
import json, subprocess, hashlib, math
import numpy as np

R=Path(__file__).resolve().parents[1]
D=json.loads((R/'source/sound-design.json').read_text())
SR=D['sampleRate']; FF=D['ffmpeg']; RNG=np.random.default_rng(D['seed'])
def write_json(p,x): p.write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n')
def wav(p,x):
    p.parent.mkdir(parents=True,exist_ok=True)
    subprocess.run([FF,'-y','-v','error','-f','f32le','-ar',str(SR),'-ac','1','-i','pipe:0','-c:a','pcm_s24le',str(p)],input=np.asarray(x,dtype='<f4').tobytes(),check=True)
def band_noise(n,lo,hi,rng):
    white=rng.standard_normal(n)
    f=np.fft.rfftfreq(n,1/SR)
    response=(1-np.exp(-(f/max(20,lo))**4))*np.exp(-(f/hi)**4)
    x=np.fft.irfft(np.fft.rfft(white)*response,n=n)
    return x/max(1e-9,np.sqrt(np.mean(x*x)))
def envelope(t,attack,decay): return (1-np.exp(-t/attack))**2*np.exp(-t/decay)
MATERIALS={
    'wood':([1,1.61,2.74,4.11],[1,.58,.23,.11],[1,.7,.46,.30]),
    'ceramic':([1,1.47,2.31,3.76],[1,.34,.17,.065],[1,.8,.55,.38]),
    'copper':([1,1.83,2.71,4.18],[1,.27,.13,.04],[1,.77,.50,.3]),
    'latch':([1,1.23,2.63,3.92],[1,.75,.31,.07],[1,.8,.38,.25]),
}
def render(cue):
    n=round(cue['seconds']*SR); stems={k:np.zeros(n) for k in ['impact','body','air']}
    rng=np.random.default_rng(D['seed']+cue['index']*7919)
    for ev in cue['events']:
        start=round(ev['at']*SR); count=n-start-round(D['silentTailSeconds']*SR)
        t=np.arange(count)/SR; kind=ev['kind']; level=ev['level']; hz=ev.get('hz',300); decay=ev.get('decay',.06)
        if kind in MATERIALS:
            ratios,levels,decays=MATERIALS[kind]
            signal=np.zeros(count)
            for ratio,amp,d in zip(ratios,levels,decays):
                phase=2*np.pi*hz*ratio*(t+.004*decay*(1-np.exp(-t/decay)))
                signal+=amp*np.sin(phase)*envelope(t,.0017,decay*d)
            stems['body'][start:start+count]+=level*signal
            click=band_noise(count,230,3600,rng)*envelope(t,.0008,.009)
            stems['impact'][start:start+count]+=level*.13*click
        elif kind=='bubble':
            phase=2*np.pi*(hz*t+hz*ev.get('bend',-1.3)*t*t)
            stems['body'][start:start+count]+=level*np.sin(phase)*envelope(t,.002,decay)
        elif kind=='motor':
            end=ev['endHz']; phase=2*np.pi*(hz*t+(end-hz)/(2*cue['seconds'])*t*t)
            x=np.sin(phase)+.19*np.sin(2.17*phase)+.08*np.sin(3.07*phase)
            stems['body'][start:start+count]+=level*x*envelope(t,.018,decay)
        elif kind=='air':
            x=band_noise(count,ev.get('lo',350),ev.get('hi',2200),rng)
            stems['air'][start:start+count]+=level*x*envelope(t,.01,decay)
        else: raise ValueError(kind)
    edge=round(.004*SR); tail=round(.032*SR); silence=round(D['silentTailSeconds']*SR)
    fade=np.ones(n); fade[:edge]=np.sin(np.linspace(0,np.pi/2,edge))**2
    fade[-silence-tail:-silence]=np.cos(np.linspace(0,np.pi/2,tail))**2; fade[-silence:]=0
    for k in stems: stems[k]=(stems[k]-np.mean(stems[k][edge:-silence]))*fade
    mix=sum(stems.values()); gain=10**(cue['peakDbfs']/20)/np.max(np.abs(mix))
    for k in stems: stems[k]*=gain
    return sum(stems.values()),stems

def main():
    clips=[]; records=[]
    for c in D['cues']:
        x,stems=render(c); name=c['id']; master=R/'source/masters'/f'{name}.wav'; wav(master,x)
        for k,y in stems.items(): wav(R/'source/stems'/name/f'{k}.wav',y)
        out=R/'exports/sounds'/f'{name}.ogg'; out.parent.mkdir(parents=True,exist_ok=True)
        subprocess.run([FF,'-y','-v','error','-i',str(master),'-c:a','libvorbis','-q:a','5',str(out)],check=True)
        clips.extend([np.zeros(round(.55*SR)),x,np.zeros(round(.55*SR))])
        records.append({'id':name,'title':c['title'],'seconds':c['seconds'],'master':str(master.relative_to(R)),
            'ogg':str(out.relative_to(R)),'allStemsShareMixNormalization':True})
    wav(R/'review/S01-12个短音-顺序试听.wav',np.concatenate(clips))
    write_json(R/'review/render-receipt.json',{'seed':D['seed'],'sampleRate':SR,'cues':records,'originalSampleImports':0,'stemsPerCue':3})
    print(json.dumps({'rendered':len(records),'masters':len(records),'stems':len(records)*3,'loops':0}))
if __name__=='__main__': main()
