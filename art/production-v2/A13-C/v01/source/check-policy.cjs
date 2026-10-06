const fs=require('fs'),path=require('path');const R=path.resolve(__dirname,'..');
const read=rel=>JSON.parse(fs.readFileSync(path.join(R,rel)));global.MIX=read('exports/focus-mix-spec.json');global.S01=read('references/adopted-s01-design.json');global.SoundBudget=require('../references/s01-policy.js').SoundBudget;const {FocusEdges,plan}=require('./mix-policy.js');
const checks=[],check=(name,pass)=>checks.push({name,pass});
const equal=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
for(const scene of MIX.scenarios)for(const distance of [0,2,8,14]){
 const a=plan(scene.id,'current',distance),b=plan(scene.id,'proposed',distance);check(`background-identical:${scene.id}:${distance}`,equal(a.events.filter(e=>e.kind!=='focus'),b.events.filter(e=>e.kind!=='focus')));
 check(`focus-enter-same-exit-compensated:${scene.id}:${distance}`,a.events.filter(e=>e.kind==='focus').every((e,i)=>e.id==='focus_enter'?e.gain===b.events.filter(e=>e.kind==='focus')[i].gain:e.gain===.22&&b.events.filter(e=>e.kind==='focus')[i].gain===.28));
 check(`s01-budget:${scene.id}:${distance}`,[1,3].every(at=>b.events.filter(e=>e.kind==='s01'&&e.at===at).length<=6));
 check(`focus-only-cut:${scene.id}:${distance}`,b.events.filter(e=>e.kind==='focus').every((e,i,arr)=>i===arr.length-1||Math.abs(e.at+e.cutAfterSeconds-arr[i+1].at)<1e-9));
}
check('crowded-eight-resolves-six',plan('crowded').events.filter(e=>e.kind==='s01'&&e.at===1).length===6);
check('distance14-no-s01-still-focus',plan('crowded','proposed',14).events.filter(e=>e.kind==='s01').length===0&&plan('crowded','proposed',14).focusEvents===2);
check('rapid-four-edges-not-five-samples',plan('rapid').focusEvents===4);
check('zero-background-keeps-focus',plan('rain_combat','proposed',2,0).events.filter(e=>e.kind!=='focus').every(e=>e.gain===0)&&plan('rain_combat','proposed',2,0).events.filter(e=>e.kind==='focus').every(e=>e.gain>0));
for(const reason of ['paused','menu','alive','connected']){
 const e=new FocusEdges(),options={[reason]:reason==='alive'||reason==='connected'?false:true};check('silent-'+reason,e.accept(true,options)===null);
 if(reason!=='connected')check('no-replay-after-'+reason,e.accept(true)===null);
}
const edge=new FocusEdges();check('first-confirmed-enter',edge.accept(true).id==='focus_enter');check('duplicate-no-replay',edge.accept(true)===null);check('exit-once',edge.accept(false).id==='focus_exit');edge.reset();check('reset-new-session-enter',edge.accept(true).id==='focus_enter');
const plans=Object.fromEntries(MIX.scenarios.map(s=>[s.id,{current:plan(s.id,'current'),proposed:plan(s.id,'proposed')}]));
// JSON null represents an uncapped final cue duration; consumers use decoded file duration.
fs.writeFileSync(path.join(R,'exports/scenario-plans.json'),JSON.stringify(plans,null,2)+'\n');
const result={allPassed:checks.every(c=>c.pass),cases:checks.length,checks,gameRuntimeVerified:false,scope:'Audition plans, adopted S01 budget and duplicate/silent focus edges; not a replacement runtime implementation'};
fs.writeFileSync(path.join(R,'review/policy-checks.json'),JSON.stringify(result,null,2)+'\n');if(!result.allPassed)throw Error(JSON.stringify(checks.filter(c=>!c.pass)));
console.log(JSON.stringify({allPassed:result.allPassed,cases:checks.length,scenarios:8}));
