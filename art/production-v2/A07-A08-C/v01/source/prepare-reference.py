"""Gather adopted art and runtime evidence. No writes outside this art draft."""
from pathlib import Path
import csv, hashlib, json, re, shutil
R = Path(__file__).resolve().parents[1]
P = R.parents[3]
for name in ['source', 'exports', 'review', 'references']:
    (R / name).mkdir(parents=True, exist_ok=True)
def dump(p, d):
    p.write_text(json.dumps(d, ensure_ascii=False, indent=2) + '\n')
inputs = [
    'art/approved-v1/A07-滑翔握持与姿态-v1/source/gliding-pose-rig.blend',
    'art/approved-v1/A07-滑翔握持与姿态-v1/deliverable/grip-pose-spec.json',
    'art/approved-v1/A08-攀爬动作-v1/source/climbing-motion-rig.blend',
    'art/approved-v1/A08-攀爬动作-v1/deliverable/climbing-motion-spec.json',
    'art/approved-v1/A10-背负装备收取过渡-v1/deliverable/transition-spec.json',
    'art/production-v2/A09-C/v01/adoption.json',
    'art/production-v2/A09-C/v01/source/waist-placement-v01.blend',
    'art/production-v2/A09-C/v01/source/reference-geometry.json',
    'art/production-v2/A09-C/v01/exports/waist-layout.json',
    'art/production-v2/A09-C/A09-C-披风鞘翅侧腰摆放-v01-审稿包.zip',
    'src/client/resources/assets/wildcraft/animations/character-poses.json',
    'src/client/java/dev/wildcraft/client/render/CharacterPoses.java',
    'src/client/java/dev/wildcraft/client/render/BackTransitions.java',
    'src/client/java/dev/wildcraft/client/render/BackEquipmentLayer.java',
    'src/client/java/dev/wildcraft/client/render/GliderHands.java',
    'src/client/java/dev/wildcraft/client/render/GliderPose.java',
    'src/client/java/dev/wildcraft/client/render/ParagliderMesh.java',
    'src/client/java/dev/wildcraft/mixin/client/ParagliderAvatarMixin.java',
    'src/client/java/dev/wildcraft/mixin/client/ParagliderHandsMixin.java',
    'src/main/java/dev/wildcraft/traversal/Gliding.java',
    'src/main/java/dev/wildcraft/traversal/Climbing.java',
    'src/main/java/dev/wildcraft/equipment/BackEquipment.java',
    'development-assets/wildcraft-0.1.0-dev.14+mc26.3.jar',
]
dump(R / 'references/baseline.json', [{'path': p, 'sha256': hashlib.sha256((P/p).read_bytes()).hexdigest()} for p in inputs])
for src, dst in [
    ('src/client/resources/assets/wildcraft/animations/character-poses.json', 'runtime-clips.json'),
    ('art/approved-v1/A07-滑翔握持与姿态-v1/deliverable/grip-pose-spec.json', 'adopted-grip.json'),
    ('art/approved-v1/A08-攀爬动作-v1/deliverable/climbing-motion-spec.json', 'adopted-climb.json'),
    ('art/approved-v1/A10-背负装备收取过渡-v1/deliverable/transition-spec.json', 'adopted-settle.json'),
    ('art/production-v2/A09-C/v01/exports/waist-layout.json', 'adopted-waist.json'),
    ('art/production-v2/A09-C/v01/references/adopted-back-layout.json', 'adopted-back.json'),
    ('art/production-v2/A09-C/v01/references/adopted-fuse-layout.json', 'adopted-fuse.json'),
]:
    shutil.copyfile(P / src, R / 'references' / dst)
shutil.copyfile(P / 'art/production-v2/A09-C/v01/source/reference-geometry.json', R / 'source/reference-geometry.json')
# Extract only geometry of the existing native first-person frame, not game source.
text = (P / 'src/client/java/dev/wildcraft/client/render/ParagliderMesh.java').read_text()
section = text.split('QUADS =')[1].split('private static final Vector3f[]')[0]
quads = re.findall(r'\{([^{}]+)\}', section)
frame = []
for index, row in enumerate(quads):
    values = [float(v.replace('F','').strip()) for v in row.split(',')]
    if len(values) != 20 or index < 114:
        continue
    frame.append({'name': 'VIEW_FRAME_'+str(index), 'vertices': [[values[i]*16,24-values[i+1]*16,values[i+2]*16] for i in [0,5,10,15]], 'faces':[{'indices':[0,1,2,3],'color':[.32,.24,.15]}]})
assert frame
dump(R / 'references/first-person-frame.json', frame)
scenarios = [
    ('climb_glide','攀爬 → 开伞','up','glide','继承当前攀爬姿态，300ms内到采用握持；伞即时出现'),
    ('release_empty','抓墙 → 空手放开','hold','empty','200ms用完整放手参考，结束对齐原版空手姿态'),
    ('climb_turn','上攀 → 横移','up','left','120ms局部衔接；循环相位仍由实际位移决定'),
    ('glide_close','持伞 → 空手收伞','glide','empty','伞即时消失，空手肢体200ms回原版'),
    ('glide_sword','持伞 → 取剑','glide','item','伞与持伞覆盖即时清除，仅保留140ms手部物件落定'),
    ('glide_bow','持伞 → 拉弓','glide','bow','原版双手拉弓立即优先，盾收起，无自定义手部落定'),
    ('glide_block','持伞 → 举盾','glide','block','盾与原版挡格立即优先，无自定义手部落定'),
    ('rapid_glide','收伞 → 重开 → 拉弓','glide','rapid','每次从当前呈现姿态重算；动作即时打断'),
    ('mantle_restore','登顶 → 站立','mantle','empty','只恢复视觉肢体，不加入根运动或新翻越能力'),
    ('loss_reset','持伞 → 状态失效','glide','reset','死亡/隐身/旁观/断开等测试输入即清空参考，不播退出'),
    ('blocked_climb','上攀 → 停住','up','hold','阻挡时相位停止；120ms转抓墙参考，恢复需实际位移'),
    ('same_spare','持伞 → 相同备用剑','glide','spare','备用剑在手、登记剑仍背负；两者是不同真实引用'),
]
spec = {'asset':'A07-A08-C','version':'v01','date':'2026-10-06','adopted':False,'integrated':False,'gameRuntimeVerified':False,
        'purpose':'Bounded visual joins and immediate action takeover; reference review only',
        'newProductionModels':0,'newTextures':0,'newAuthoredClipSets':0,'localJoinTemplates':3,
        'durationsMs':{'open':300,'emptyExit':200,'climbSwitch':120},
        'curve':'smoothstep x*x*(3-2*x), reference limb morph only; item settle retains adopted (1-t)^3',
        'rotation':'shortest-path normalized quaternion slerp between baked Euler frame rotations; linear pivot positions',
        'climbPhase':'actual displacement * 18; this review supplies labelled synthetic displacement, not time-driven gameplay',
        'releaseResample':'entire adopted release 0..18 in existing 200ms visual window; outgoing snapshot in first half, native pose in final half',
        'immediateOverrides':['attack','use_item','bow','shield','identity_loss','death','spectator','invisible','disconnect','dimension_change'],
        'firstPerson':'existing steady GliderPose hands/frame while open, native item takeover on close; 70 degree vertical reference, no camera change',
        'inventory':'three display references, no new slots; owner changes atomically, registered/spare are distinct',
        'scenarios':[{'id':i,'label':n,'from':a,'to':b,'eventSeconds':.65,'durationSeconds':1.8,'reviewNote':note} for i,n,a,b,note in scenarios]}
dump(R / 'exports/transition-spec.json', spec)
with (R/'exports/scenario-map.csv').open('w',encoding='utf-8-sig',newline='') as f:
    w=csv.DictWriter(f,fieldnames=['id','label','from','to','eventSeconds','durationSeconds','reviewNote']);w.writeheader();w.writerows(spec['scenarios'])
# Audit evidence independent of the proposed join engine.
clips=json.loads((R/'references/runtime-clips.json').read_text());issues=[]
for variant,data in clips.items():
    arr=data['release'];delta,index,limb,axis=max((abs(arr[n+1][j][k]-arr[n][j][k]),n,j,k) for n in range(len(arr)-1) for j in range(4) for k in range(3,6))
    issues.append({'variant':variant,'clip':'release','largestRawEulerStepDegrees':delta*180/3.141592653589793,'betweenFrameIndices':[index,index+1],'limb':limb,'axis':axis,'runtimeWindowFrames':6,'sourceLastFrameIndex':len(arr)-1})
dump(R/'review/source-audit.json',{'observed':issues,'scope':'Local CharacterPoses and adopted bake inspection; not a fresh game capture','firstPersonUsesSteadyGrip':True,'thirdPersonOpenSeconds':.3,'noGameplayPatch':True})
queue=P/'art/production-docs/ART-DESIGN-PRODUCTION-SCHEDULE-2026-10-04.csv'
with queue.open(encoding='utf-8-sig',newline='')as f:r=csv.DictReader(f);fields=r.fieldnames;rows=list(r)
for row in rows:
    if row['编号']=='A07-A08-C':row['制作阶段状态']='v01制作中；探索动作时序/连续衔接与即时取物参考，待审未接入'
with queue.open('w',encoding='utf-8-sig',newline='')as f:w=csv.DictWriter(f,fieldnames=fields);w.writeheader();w.writerows(rows)
print(json.dumps({'scenarios':len(scenarios),'baselineFiles':len(inputs),'firstPersonFrameQuads':len(frame),'sourceAudit':issues},ensure_ascii=False))
