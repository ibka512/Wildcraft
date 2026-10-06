import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
import vm from 'node:vm';
const R=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..'),read=p=>JSON.parse(fs.readFileSync(path.join(R,p),'utf8'));vm.runInThisContext(fs.readFileSync(path.join(R,'source/machine-presentation.js'),'utf8'));const M=globalThis.WildcraftMachinePresentation,S=read('source/review-scenarios.json'),G=read('references/F05-05-geometry-and-uv.json'),F=read('references/F05-03-geometry-and-uv.json'),glyphs=read('source/status-glyphs.json').glyphs;
const cases=[];
for(const base of S)for(let n=0;n<6;n++){
 const s=M.withNode(base,n),p=M.present(s),expected=s.visible!==false&&!s.feedback&&!s.enabled&&!s.kinds[n]&&s.held>=1&&s.held<=8&&!s.heldSpent&&(s.held!==2||s.battery<0)?s.held:0;assert.equal(p.ghost,expected);assert.equal(p.status,!s.enabled?'off':s.working?'working':'waiting');assert.equal(p.number,s.battery<0?'— / 1000':s.energy+' / 1000');assert.equal(p.ghostEnergy,p.ghost===2?s.heldEnergy:0);assert.equal(s.kinds.filter(k=>k===2).length,base.kinds.filter(k=>k===2).length);assert.equal(s.battery<0?-1:s.kinds[s.battery],base.battery<0?-1:2);
 const a=M.geometry(G,F,s,0),b=M.geometry(G,F,s,7);assert.equal(a.filter(p=>p.ghost).length,p.ghost===1?24:p.ghost===2?16+M.fractions(p.ghostEnergy).filter(f=>f>0).length:0);assert(a.filter(p=>p.ghost).every(p=>p.color[3]===136/255));let changes=[];for(let i=0;i<a.length;i++)if(JSON.stringify(a[i].vertices)!==JSON.stringify(b[i].vertices))changes.push(a[i].name);assert(changes.every(name=>name.startsWith('fan/')&&(name.includes('blade_')||name.includes('rotor_')||name.includes('axle_')||name.includes('hub_')||name.includes('cap_'))));assert.deepEqual(a.filter(p=>p.ghost),b.filter(p=>p.ghost));
 cases.push({scenario:s.id,node:n,ghost:p.ghost,ghostMeshParts:a.filter(p=>p.ghost).length,changedParts:changes});
}
for(const [name,rows] of Object.entries(glyphs)){assert.equal(rows.length,9);assert(rows.every(r=>r.length===9&&/^[01]+$/.test(r)));for(const [other,r]of Object.entries(glyphs))if(other!==name)assert.notDeepEqual(rows,r)}
const spring=M.present(S.find(s=>s.id==='spring-cooldown'));assert(spring.extra.includes('17'));const rocket=M.present(S.find(s=>s.id==='rocket-fuel'));assert(rocket.extra.includes('37 / 80'));const burning=M.present(S.find(s=>s.id==='rocket-off-burning'));assert.equal(burning.status,'off');assert(burning.hint.includes('燃烧中'));
const low=M.present(S.find(s=>s.id==='low-working'));assert.equal(low.status,'working');assert.equal(low.energyLabel,'低电量');const nonZeroWaiting=M.present(S.find(s=>s.id==='waiting'));assert.equal(nonZeroWaiting.status,'waiting');assert.equal(nonZeroWaiting.energyLabel,'');
const remap=M.present(S.find(s=>s.id==='running'),'zh','J');assert(remap.hint.includes('[J]'));assert(!remap.hint.includes('[R]'));
for(const e of [0,99,100,1000]){const s={...S.find(s=>s.id==='running'),energy:e};assert.equal(M.present(s).energyLabel,e===0?'电池已空':e<100?'低电量':'');}
const hiddenBattery={...S.find(s=>s.id==='battery-ready'),visible:false};assert.equal(M.present(hiddenBattery).ghost,0);assert.equal(M.present(hiddenBattery).ghostEnergy,0);
const waitingRocket={...S.find(s=>s.id==='rocket-fuel'),working:false,active:0};assert(M.present(waitingRocket).hint.includes('控制板'));assert(!M.present(waitingRocket).hint.includes('[R]'));
const result={allPassed:true,sixNodeScenarioCases:cases,uniqueProceduralGlyphs:Object.keys(glyphs),heldGhostChargePreserved:true,previewsStationary:true,activeNodeOnlyRotation:true,waitingNotAutomaticallyEmpty:true,lowChargeCanWork:true,rocketFuelWorldTickCapacity:80,springCooldownWorldTickMaximum:40,reboundKeySupported:true,productionGameTested:false};fs.writeFileSync(path.join(R,'review/contract-verification.json'),JSON.stringify(result,null,2)+'\n');console.log(JSON.stringify({allPassed:true,cases:cases.length,glyphs:Object.keys(glyphs).length}));
