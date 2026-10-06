const $=id=>document.getElementById(id);
let ctx,master,analyser,token=0,nodes=[],timer,raf,playing=false,loading=false,errors=[],lastPlan=null,lastSchedule=[],start=0,duration=5;
const buffers=new Map(),pending=new Map();
function status(s){$('status').textContent=s;}
function audioContext(){if(!ctx){ctx=new AudioContext();master=ctx.createGain();master.gain.value=Number($('master').value);analyser=ctx.createAnalyser();analyser.fftSize=2048;master.connect(analyser);analyser.connect(ctx.destination);}return ctx;}
async function load(id){
 if(buffers.has(id))return buffers.get(id);if(pending.has(id))return pending.get(id);
 const p=(async()=>{let bytes;if(AUDIO[id]){bytes=Uint8Array.from(atob(AUDIO[id]),c=>c.charCodeAt(0)).buffer;}else{if(location.protocol==='file:')throw Error('此场景需要本机试听服务读取原版缓存；请改选安静、机械、六声预算或快速切换。');const item=NATIVE.find(a=>a.id===id);if(!item)throw Error('找不到试听声音');const r=await fetch(item.url).catch(()=>{throw Error('本机原版声音缓存读取失败，请重试或改选自有音效场景。');});if(!r.ok)throw Error('本机原版声音缓存不可用，请改选自有音效场景。');bytes=await r.arrayBuffer();}const b=await audioContext().decodeAudioData(bytes);buffers.set(id,b);return b;})();
 pending.set(id,p);try{return await p;}finally{pending.delete(id);}
}
function stop(message='已停止，可重新对照。'){
 token++;clearTimeout(timer);cancelAnimationFrame(raf);loading=false;playing=false;
 if(ctx){const t=ctx.currentTime;for(const n of nodes){n.gain.gain.cancelScheduledValues(t);n.gain.gain.setValueAtTime(n.gain.gain.value,t);n.gain.gain.linearRampToValueAtTime(0,t+.006);try{n.source.stop(t+.012);}catch{}}}
 nodes=[];$('progress').value=0;status(message);$('playing').textContent='等待播放';
}
function pulse(){if(!playing)return;const elapsed=ctx.currentTime-start;$('progress').value=Math.max(0,Math.min(1,elapsed/duration));$('playing').textContent=elapsed<0?'准备播放':`${Math.min(duration,Math.max(0,elapsed)).toFixed(1)} / ${duration.toFixed(1)} 秒`;raf=requestAnimationFrame(pulse);}
async function play(profile,mode='mix'){
 stop('正在准备声音…');const mine=token;loading=true;errors=[];
 try{audioContext();await ctx.resume();if(token!==mine)return;
 let p=MixPolicy.plan($('scene').value,profile,Number($('distance').value),Number($('background').value));
 if(mode==='background')p={...p,events:p.events.filter(e=>e.kind!=='focus')};
 if(mode==='enter'||mode==='exit')p={...p,duration:1,events:[{id:'focus_'+mode,kind:'focus',at:.1,gain:MIX.profiles[profile][mode],cutAfterSeconds:Infinity}]};
 await Promise.all([...new Set(p.events.map(e=>e.id))].map(load));if(token!==mine)return;
  loading=false;lastPlan=p;lastSchedule=[];master.gain.setValueAtTime(Number($('master').value),ctx.currentTime);start=ctx.currentTime+.06;duration=p.duration;
  for(const e of p.events){const b=buffers.get(e.id),s=ctx.createBufferSource(),g=ctx.createGain();s.buffer=b;s.playbackRate.value=1;g.gain.value=e.gain;s.connect(g);g.connect(master);
   const span=Math.min(b.duration,Number.isFinite(e.cutAfterSeconds)?e.cutAfterSeconds:b.duration,p.duration-e.at);if(span<=0)continue;
   const n={source:s,gain:g};nodes.push(n);lastSchedule.push({...e,span,actualGain:g.gain.value,rate:s.playbackRate.value});s.onended=()=>{s.disconnect();g.disconnect();nodes=nodes.filter(v=>v!==n);};s.start(start+e.at,0,span);
  }
  playing=true;status(`${MIX.scenarios.find(s=>s.id===p.scene).title} · ${profile==='current'?'原方案':'建议方案'}${mode==='background'?' · 仅环境声':''}`);pulse();
  timer=setTimeout(()=>{if(token===mine)stop('试听结束。可切换方案，在相同场景再次比较。');},(duration+.15)*1000);
 }catch(e){if(token!==mine)return;errors.push(String(e.message));stop(e.message);}
}
for(const b of document.querySelectorAll('[data-profile]'))b.addEventListener('click',()=>play(b.dataset.profile,b.dataset.mode||'mix'));
$('stop').addEventListener('click',()=>stop());
for(const id of ['scene','distance','background'])$(id).addEventListener('change',()=>{stop('设置已更新。点击 A 或 B 开始比较。');updateLabels();});
$('master').addEventListener('input',()=>{if(master)master.gain.setValueAtTime(Number($('master').value),ctx.currentTime);updateLabels();});
for(const id of ['distance','background'])$(id).addEventListener('input',()=>{stop('设置已更新。点击 A 或 B 开始比较。');updateLabels();});
function updateLabels(){$('masterOut').textContent=Math.round(Number($('master').value)*100)+'%';$('distanceOut').textContent=$('distance').value+' 格';$('backgroundOut').textContent=Math.round(Number($('background').value)*100)+'%';$('sceneNote').textContent=MIX.scenarios.find(s=>s.id===$('scene').value).note;}
window.reviewState=()=>({playing,loading,token,context:ctx?.state||'not-created',nodes:nodes.length,buffers:buffers.size,pending:pending.size,errors,lastPlan,lastSchedule,master:master?.gain.value||0});
window.signalLevel=()=>{if(!analyser)return 0;const a=new Float32Array(analyser.fftSize);analyser.getFloatTimeDomainData(a);return Math.max(...a.map(Math.abs));};
window.playReview=play;window.stopReview=stop;window.decodeAll=async()=>{audioContext();await Promise.all([...Object.keys(AUDIO),...NATIVE.map(a=>a.id)].map(load));return buffers.size;};
window.addEventListener('pagehide',()=>stop('已离开试听页。'));document.addEventListener('visibilitychange',()=>{if(document.hidden)stop('页面隐藏，试听已停止。');});updateLabels();
