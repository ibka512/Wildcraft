/* Local audition planner. Does not alter Minecraft's sound categories or states. */
(function(){
class FocusEdges{
 constructor(){this.active=false;this.sequence=0;}
 reset(){this.active=false;this.sequence++;}
 accept(active,{paused=false,menu=false,alive=true,connected=true}={}){
  if(!connected){this.reset();return null;}
  if(active===this.active)return null;this.active=active;
  if(paused||menu||!alive)return null;
  return {id:active?'focus_enter':'focus_exit',sequence:++this.sequence};
 }
}
function plan(scene,profile='proposed',distance=2,backgroundGain=1){
 if(!MIX.scenarios.some(s=>s.id===scene)||!MIX.profiles[profile])throw Error('Unknown audition configuration');
 if(!Number.isFinite(distance)||distance<0||!Number.isFinite(backgroundGain)||backgroundGain<0)throw Error('Invalid gain or distance');
 const events=[],log=[],budget=new SoundBudget();
 const native=(id,at,gain)=>events.push({id,at,gain:gain*backgroundGain,kind:'native',source:'native_'+id+'_'+at});
 if(['rain','rain_combat'].includes(scene))for(let i=0;i<6;i++)native(i%2?'rain2':'rain1',i*.8,.22);
 if(scene==='snow')for(let i=0;i<8;i++)native(i%2?'snow2':'snow1',.4+i*.6,.28);
 if(['combat','rain_combat'].includes(scene))for(const at of [1,3]){native('strong',at-.25,.30);native('bow',at-.04,.35);native('bow_hit',at+.10,.30);native('hit',at+.18,.35);}
 if(['machines','crowded','rain_combat'].includes(scene))for(const at of [1,3]){
  const ids=scene==='crowded'?['cooking_start','charger_start','mechanic_start','fabricator_start','mechanic_install','fuse_join','cooking_complete','fabricator_complete']:scene==='machines'?['mechanic_start','charger_full','fabricator_complete']:['mechanic_start','mechanic_install','charger_full'];
  for(let i=0;i<ids.length;i++){
   const c=S01.cues.find(c=>c.id===ids[i]);const verdict=budget.accept({id:c.id,source:'machine'+i,operation:'mix_'+at+'_'+i,dimension:'audition',confirmed:true,distance,limit:c.maxDistance,seconds:c.seconds,volume:c.gameVolume,priority:c.priority},at*1000);
   log.push({at,id:c.id,...verdict});
  }
  // Resolve the entire simultaneous batch before scheduling its final retained voices.
  for(const v of budget.voices.filter(v=>v.at===at*1000))events.push({id:v.id,at,gain:v.volume*Math.max(0,1-distance/v.limit)*backgroundGain,kind:'s01',source:v.source,seconds:v.seconds});
 }
 const samples=scene==='rapid'?[[1,true],[1.12,false],[1.24,true],[1.28,true],[1.36,false]]:[[1,true],[3,false]],edge=new FocusEdges(),focus=[];
 for(const [at,active]of samples){const e=edge.accept(active);if(e)focus.push({...e,at,gain:active?MIX.profiles[profile].enter:MIX.profiles[profile].exit,kind:'focus',source:'focus'});}
 for(let i=0;i<focus.length;i++)focus[i].cutAfterSeconds=i+1<focus.length?focus[i+1].at-focus[i].at:Infinity;
 events.push(...focus);
 return {scene,profile,distance,backgroundGain,events:events.sort((a,b)=>a.at-b.at),log,duration:MIX.referenceSeconds,focusEvents:focus.length,maxFocusVoices:1};
}
const api={FocusEdges,plan};if(typeof window!=='undefined')window.MixPolicy=api;if(typeof module!=='undefined')module.exports=api;
})();
