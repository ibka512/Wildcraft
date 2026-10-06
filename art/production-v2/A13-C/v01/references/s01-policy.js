/* Reference only: not connected to Minecraft. Confirmed events come from caller. */
class SoundBudget {
  constructor(){this.seen=new Map();this.voices=[];this.cooldowns=new Map();this.epoch=0;}
  reset(){this.seen.clear();this.voices=[];this.cooldowns.clear();this.epoch++;}
  remove(source){this.voices=this.voices.filter(v=>v.source!==source);}
  accept(e,now=0){
    this.voices=this.voices.filter(v=>v.end>now);
    for(const [k,t] of this.seen)if(now-t>5000)this.seen.delete(k);
    for(const [k,t] of this.cooldowns)if(t<=now)this.cooldowns.delete(k);
    if(!e.confirmed)return {accepted:false,reason:'未确认成功'};
    if(!Number.isFinite(e.distance)||e.distance<0||e.distance>=e.limit)return {accepted:false,reason:'超出参考距离'};
    const key=[this.epoch,e.dimension,e.source,e.operation,e.id].join('|');
    if(this.seen.has(key))return {accepted:false,reason:'重复确认'};
    this.seen.set(key,now);
    while(this.seen.size>256)this.seen.delete(this.seen.keys().next().value);
    const ck=[this.epoch,e.dimension,e.source,e.id].join('|');
    if(now<(this.cooldowns.get(ck)??-Infinity))return {accepted:false,reason:'来源间隔'};
    const sourceKey=[e.dimension,e.source].join('|');
    const sameSource=this.voices.find(v=>v.sourceKey===sourceKey);
    let victim=sameSource;
    if(victim&&victim.priority>=e.priority)return {accepted:false,reason:'来源已有等或高优先级声音'};
    const remaining=this.voices.filter(v=>v!==victim);
    if(remaining.filter(v=>v.id===e.id).length>=2)return {accepted:false,reason:'同事件上限2'};
    if(remaining.length>=6){
      const low=[...remaining].sort((a,b)=>a.priority-b.priority||a.at-b.at)[0];
      if(low.priority>=e.priority)return {accepted:false,reason:'全场上限6'};
      victim=low;
    }
    this.voices=this.voices.filter(v=>v!==victim);
    this.cooldowns.set(ck,now+(e.cooldownMs??150));
    const voice={...e,sourceKey,at:now,end:now+e.seconds*1000};this.voices.push(voice);
    return {accepted:true,gain:e.volume*Math.max(0,1-e.distance/e.limit),replaced:victim?.source??null};
  }
}
if(typeof module!=='undefined')module.exports={SoundBudget};
