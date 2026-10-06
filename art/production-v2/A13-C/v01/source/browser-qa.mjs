const t=await taskSpace(2),p=t.page('p1');
const results=await p.evaluate(async()=>{
 const checks=[];const check=(name,ok,details)=>{checks.push({name,passed:!!ok,details});if(!ok)throw Error(name);};
 check('No automatic replay after stop',!window.reviewState().playing&&window.reviewState().nodes===0);
 check('Decode original own and local native buffers',await window.decodeAll()===22);
 const states=[];
 for(const scene of ['quiet','rain','snow','combat','machines','crowded','rain_combat','rapid']){
  document.getElementById('scene').value=scene;
  for(const profile of ['current','proposed']){
   await window.playReview(profile);const s=window.reviewState();states.push(s);
   check(`${scene}/${profile} actual scheduling`,s.playing&&!s.loading&&!s.errors.length,s.lastSchedule.map(e=>({id:e.id,gain:e.actualGain,span:e.span})));
   check(`${scene}/${profile} original pitch`,s.lastSchedule.every(e=>e.rate===1));
   const f=s.lastSchedule.filter(e=>e.kind==='focus');
   check(`${scene}/${profile} one focus voice`,f.every((e,i)=>!i||f[i-1].at+f[i-1].span<=e.at+1e-6));
   check(`${scene}/${profile} correct gains`,f.every(e=>Math.abs(e.actualGain-(e.id==='focus_enter'?.22:profile==='current'?.22:.28))<1e-6));
   if(scene==='crowded')check(`${profile} S01 batch budget six`,s.lastSchedule.filter(e=>e.kind==='s01'&&e.at===1).length===6);
   if(scene==='rapid')check(`${profile} duplicate state no replay`,f.length===4);
   window.stopReview();check(`${scene}/${profile} all sources cancelled`,window.reviewState().nodes===0&&!window.reviewState().playing);
  }
  const [a,b]=states.slice(-2).map(s=>s.lastSchedule.filter(e=>e.kind!=='focus'));check(`${scene} backgrounds identical`,JSON.stringify(a)===JSON.stringify(b));
 }
 document.getElementById('scene').value='crowded';document.getElementById('distance').value=14;await window.playReview('proposed');check('Distance14 preserves focus only',window.reviewState().lastSchedule.length===2);window.stopReview();
 document.getElementById('distance').value=2;document.getElementById('background').value=0;await window.playReview('proposed');check('Background muted; focus unaffected',window.reviewState().lastSchedule.every(e=>e.kind==='focus'||e.actualGain===0));window.stopReview();
 document.getElementById('background').value=1;
 const a=window.playReview('current'),b=window.playReview('proposed');await Promise.all([a,b]);check('Latest async request wins',window.reviewState().lastPlan.profile==='proposed'&&window.reviewState().playing);window.stopReview();
 const loading=window.playReview('current');window.stopReview();await loading;check('Stop during async preparation prevents late play',!window.reviewState().playing&&window.reviewState().nodes===0);
 document.getElementById('scene').value='quiet';updateLabels();return {checks,allPassed:checks.every(c=>c.passed),buffers:window.reviewState().buffers,states};
});
const fs=await import('node:fs/promises');await fs.writeFile('/Volumes/仕事/Wildcraft/开发环境/project/art/production-v2/A13-C/v01/review/browser-checks.json',JSON.stringify(results,null,2)+'\n');console.log({checks:results.checks.length,allPassed:results.allPassed});
await p.selectOption('loc=css:#scene','rain_combat');await p.click("loc=css:button[data-profile='proposed']:not([data-mode])");await p.waitForFunction(()=>window.signalLevel()>0.01,undefined,{timeout:5000});console.log(await p.evaluate(()=>({signal:window.signalLevel(),schedule:window.reviewState().lastSchedule.length})));await p.waitForFunction(()=>!window.reviewState().playing,undefined,{timeout:10000});console.log(await p.evaluate(()=>({afterNaturalEnd:window.reviewState().nodes,status:document.getElementById('status').textContent})));await p.selectOption('loc=css:#scene','quiet');console.log(await p.snapshot());
