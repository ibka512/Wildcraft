const $=id=>document.getElementById(id);
let ctx,master,analyser,generation=0,timers=[],voices=[],playingId=null;
const buffers=new Map();const policy=new SoundBudget();let observations=[];
window.audioQA={played:[],errors:[],maxActive:0};
async function prepare(){
  if(!ctx){ctx=new AudioContext();master=ctx.createGain();master.gain.value=Number($('volume').value);analyser=ctx.createAnalyser();analyser.fftSize=1024;master.connect(analyser);analyser.connect(ctx.destination);}
  await ctx.resume();
  for(const id of Object.keys(AUDIO))if(!buffers.has(id)){
    const bytes=Uint8Array.from(atob(AUDIO[id]),c=>c.charCodeAt(0));buffers.set(id,await ctx.decodeAudioData(bytes.buffer));
  }
  return ctx;
}
function stop(label='已停止'){
  generation++;for(const t of timers)clearTimeout(t);timers=[];
  for(const v of voices){v.gain.gain.cancelScheduledValues(ctx.currentTime);v.gain.gain.setTargetAtTime(0,ctx.currentTime,.003);try{v.node.stop(ctx.currentTime+.012);}catch{} }
  voices=[];playingId=null;policy.reset();$('status').textContent=label;
  document.querySelectorAll('.playing').forEach(x=>x.classList.remove('playing'));
}
function emit(id,gain=1,source=id){
  const token=generation;const buffer=buffers.get(id);if(!buffer)throw new Error('音频尚未准备');
  const node=ctx.createBufferSource(),g=ctx.createGain();node.buffer=buffer;g.gain.value=gain;
  node.connect(g);g.connect(master);const v={node,gain:g,source};voices.push(v);
  const c=DESIGN.cues.find(x=>x.id===id);const title=c?.title??(id==='focus_enter'?'原专注进入':'原专注退出');
  $('status').textContent='正在试听：'+title;playingId=id;
  const card=document.querySelector('[data-card="'+id+'"]');card?.classList.add('playing');
  node.onended=()=>{node.disconnect();g.disconnect();voices=voices.filter(x=>x!==v);if(token===generation){card?.classList.remove('playing');if(!voices.length){$('status').textContent='试听结束';playingId=null;}}};
  node.start();audioQA.played.push(id);audioQA.maxActive=Math.max(audioQA.maxActive,voices.length);return v;
}
async function solo(id){stop('正在准备');const token=generation;await prepare();if(token!==generation)return;emit(id);}
async function sequence(){
  stop('正在准备整组');const token=generation;await prepare();if(token!==generation)return;
  let at=0;for(const c of DESIGN.cues){const timer=setTimeout(()=>{timers=timers.filter(t=>t!==timer);if(token===generation)emit(c.id);},at);timers.push(timer);at+=(c.seconds+.85)*1000;}
}
async function scene(){
  stop('正在准备叠加');const token=generation;await prepare();if(token!==generation)return;
  policy.reset();observations=[];const distance=Number($('distance').value);const mode=$('scene').value;
  const ids=mode==='four'?['mechanic_start','mechanic_start','mechanic_start','mechanic_start']:['cooking_start','charger_start','mechanic_start','fabricator_start','mechanic_install','fuse_join','cooking_complete','fabricator_complete'];
  for(let i=0;i<ids.length;i++){
    const c=DESIGN.cues.find(x=>x.id===ids[i]),source='source'+i;
    const verdict=policy.accept({id:c.id,confirmed:true,operation:'demo'+i,dimension:'review',source,distance,limit:c.maxDistance,seconds:c.seconds,volume:c.gameVolume,priority:c.priority},0);
    observations.push({title:c.title,...verdict});
  }
  // Start only final admitted voices: this batch resolves stealing before any sound starts.
  for(const v of policy.voices)emit(v.id,v.volume*Math.max(0,1-distance/v.limit),v.source);
  if(!policy.voices.length)$('status').textContent='超出参考距离，没有声音';
  $('log').textContent=observations.map(x=>x.title+'：'+(x.accepted?'准入'+(x.replaced?'（替换低优先级）':''):'裁减：'+x.reason)).join('\n')+'\n最终同时播放：'+policy.voices.length+'。参考演示，不是游戏录音。';
}
function attach(){
  for(const button of document.querySelectorAll('[data-play]'))button.onclick=()=>solo(button.dataset.play).catch(error);
  $('all').onclick=()=>sequence().catch(error);$('stop').onclick=()=>stop();$('simulate').onclick=()=>scene().catch(error);
  $('volume').oninput=()=>{if(master)master.gain.gain.setTargetAtTime(Number($('volume').value),ctx.currentTime,.02);$('volumeLabel').textContent=Math.round(Number($('volume').value)*100)+'%';};
  $('distance').oninput=()=>{$('distanceLabel').textContent=$('distance').value+' 格';};
  $('unload').onclick=()=>{stop('模拟卸载：所有声音已停止，事件缓存已清空');$('log').textContent='来源离开/断开时立即取消声音和定时任务；不存在循环声音。';};
  document.addEventListener('visibilitychange',()=>{if(document.hidden)stop('页面离开，试听已停止');});
  window.addEventListener('pagehide',()=>stop());window.addEventListener('error',e=>audioQA.errors.push(e.message));
}
function error(e){audioQA.errors.push(String(e));stop('试听出错：'+e.message);}
window.reviewState=()=>({context:ctx?.state,bufferCount:buffers.size,active:voices.length,timerCount:timers.length,playingId,policyVoices:policy.voices.length,seen:policy.seen.size,errors:audioQA.errors});
window.signalLevel=()=>{if(!analyser)return 0;const a=new Float32Array(analyser.fftSize);analyser.getFloatTimeDomainData(a);return Math.max(...a.map(Math.abs));};
attach();window.reviewReady=true;
