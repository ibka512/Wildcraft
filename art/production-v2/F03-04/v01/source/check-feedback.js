const fs=require('node:fs'),path=require('node:path'),F=require('./feedback-engine.js');
const R=path.resolve(__dirname,'..'),C=JSON.parse(fs.readFileSync(path.join(R,'exports/fuse-feedback-presets.json'),'utf8'));
const cases=[],assert=(v,n)=>{if(!v)throw Error(n);};
function check(name,e,accepted,count){const s=new F.Emitter(C),r=s.emit(e,0);assert(r.accepted===accepted,name+' accepted');if(count!==undefined)assert(r.spawned===count,name+' count');cases.push({name,accepted:r.accepted,spawned:r.spawned,reason:r.reason});return s;}
for(const type of Object.keys(C.presets)){
 const p=C.presets[type];check(type+' confirmed',F.fixture(type),true,p.count);check(type+' model-only',F.fixture(type,'melee','model-only'),true,p.count);check(type+' refused',F.fixture(type,'melee','refused'),false,0);check(type+' inactive',F.fixture(type,'melee','inactive'),false,0);
 check(type+' reduced',F.fixture(type,'melee','confirmed',{particleMode:'reduced'}),true,Math.ceil(p.count*.5));check(type+' minimal',F.fixture(type,'melee','confirmed',{particleMode:'minimal'}),true,0);check(type+' near-camera',F.fixture(type,'melee','confirmed',{distance:.4}),true,0);
 const emitter=new F.Emitter(C);emitter.emit(F.fixture(type),0);for(let tick=0;tick<=12;tick+=.25){for(const a of emitter.sample(tick)){assert(a.alpha>=0&&a.alpha<=1,'alpha bounded');assert([...a.position,...a.offset,a.size].every(Number.isFinite),'finite');assert(Math.hypot(...a.offset)<=.350001,'radius bounded');}}
 assert(emitter.sample(p.lifetimeTicks).length===0,type+' clear at lifetime');
 const dupe=new F.Emitter(C);const event=F.fixture(type);dupe.emit(event,0);assert(dupe.emit(event,0).spawned===0,'duplicate suppressed');assert(dupe.particles.length===p.count,'no duplicated particles');cases.push({name:type+' duplicate',passed:true});
 const a=new F.Emitter(C),b=new F.Emitter(C);a.emit(event,0);b.emit(event,0);assert(JSON.stringify(a.sample(2))===JSON.stringify(b.sample(2)),'deterministic fixture seed');
}
for(const type of ['fire','ice','elastic']){
 for(const channel of ['melee','shield-melee','arrow-entity'])check(type+' '+channel,F.fixture(type,channel),true,C.presets[type].count);
 for(const channel of ['arrow-block','shield-ranged','shield-partial'])check(type+' '+channel,F.fixture(type,channel),false,0);
 for(const [name,channel,extra] of [['dead','melee',{targetAlive:false}],['empty-swing','melee',{damageTaken:0}],['no-effect','melee',{effectApplied:false}],['partial-block','shield-melee',{fullBlock:false}],['not-living','shield-melee',{livingDirectAttacker:false}],['arrow-refused','arrow-entity',{damageAccepted:false}],['nonliving-arrow-target','arrow-entity',{livingTarget:false}]])check(type+' '+name,F.fixture(type,channel,'confirmed',extra),false,0);
}
for(const type of ['fuse','split']){check(type+' uncommitted',F.fixture(type,'action','confirmed',{committed:false}),false,0);check(type+' keypress-only',F.fixture(type,'action','confirmed',{channel:'local-keypress'}),false,0);}
for(const [name,extra] of [['bad-sequence',{sequence:NaN}],['old-epoch',{epoch:'other'}],['future-time',{tick:1}],['expired',{tick:-41}],['bad-anchor',{anchor:[NaN,0,0]}],['bad-distance',{distance:NaN}],['unknown-result',{result:'arbitrary'}]])check(name,F.fixture('fire','melee','confirmed',extra),false,0);
const dense=new F.Emitter(C);let spawned=0;for(let i=0;i<1200;i++){spawned+=dense.emit(F.fixture('fire','melee','confirmed',{sequence:i+1,origin:'origin-'+i%4}),0).spawned;assert(dense.particles.length<=48,'shared cap');for(const o of ['origin-0','origin-1','origin-2','origin-3'])assert(dense.particles.filter(p=>p.origin===o).length<=12,'origin cap');}
assert(spawned===48,'dense cap reached');assert(dense.sample(8).length===0,'dense cleanup');assert(dense.emit(F.fixture('fire','melee','confirmed',{sequence:1}),8).spawned===0,'old replay after particles expire');dense.reset('new');assert(dense.emit(F.fixture('fire'),0).spawned===0,'old epoch rejected after reset');
function points(type,tick,direction=1){const s=new F.Emitter(C);s.emit(F.fixture(type,'melee','confirmed',{direction}),0);return s.sample(tick);}
const mean=(v,f)=>v.reduce((a,b)=>a+f(b),0)/v.length;
assert(mean(points('fire',4),p=>p.offset[1])>mean(points('fire',1),p=>p.offset[1]),'fire rises');
assert(mean(points('fuse',5),p=>Math.hypot(...p.offset))<mean(points('fuse',0),p=>Math.hypot(...p.offset)),'fuse converges');
assert(mean(points('split',4),p=>Math.hypot(...p.offset))>mean(points('split',0),p=>Math.hypot(...p.offset)),'split scatters');
assert(mean(points('elastic',4),p=>p.offset[0])>0&&mean(points('elastic',4,-1),p=>p.offset[0])<0,'elastic source direction');
assert(Object.values(C.presets).every(p=>!p.loop&&!p.flash&&!p.screenFilter&&!p.trail),'no unrequested loops/flash/filter/trail');
const out={asset:'F03-04',allPassed:true,eventCases:cases,eventCaseCount:cases.length,timeSamplesPerPreset:49,totalTimeSamples:245,highFrequencyEvents:1200,highFrequencyMaximumLive:48,maximumPerOrigin:12,memory:'particle array <=48; ordered connection sequence scalar; no delayed queue',deterministicFivePresets:true,movementDirectionsVerified:true,invalidDisplaysRejected:true,notMinecraftRuntimeVerified:true};fs.writeFileSync(path.join(R,'review/feedback-checks.json'),JSON.stringify(out,null,2)+'\n');console.log({allPassed:true,eventCases:cases.length,timeSamples:245,highFrequencyEvents:1200,maxLive:48});
