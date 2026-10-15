> 2026-10-14当前正式52/28ec9176；候选53已renderer/267资源，真实50图专项通过（含loaded arrow/rocket、实际F和accepted instant use修复），未正式53；下述源准备是历史，状态见C6AB进度与后续实际验收。

# C6AB弩背部取收源准备

2026-10-13，正式51/e98c616b保持，候选52/d23f9271正在完整35类验证；本项仅源资格与真实native模型坐标调查，无53版本/转换/renderer启用。已采用crossbow-transitions/v01（APPROVAL adopted=true，2026-10-10），32源 .32秒default back/waist/empty或shield/左右真实持弩手/两身形；源rootMotion与inputLocked为false、drawProgress0。不能将未游戏集成的源STATUS描述当新审稿授权，也不能导入源示例挂点/参考mesh。

源骨22、根/脚/头/绑定逐剪辑检查通过。main握点作者Blender腕局部(0,0,-.125)=>GLB current wrist(0,.125,0)，默认back16 raw grip最大.0017890876，均<.002，通过源握点资格（与mace的raw握点失败不同）。源资格不等于真实模型握点/GPU或原生装填射击通过。native carry盾源沿用axe .0625掌握位与原handle，须在实际模型中单独校正，不能把main弩握位改成.0625。

作者nativeRawGripAnchor(.40625,0,-.40625)是A09参考几何中心坐标，不能直接作为MC生成模型[0,1]握点。A09几何范围x/z±.5、厚度y±.03125，核对原版16px standby贴图右下握把及A09 Blender Z/up后，主握把native raw候选为(.90625,.09375,.5)，MC原crossbow generated model第三人称scale .9一次、display rotation/translation均保留；源参考已烘焙.9但只导入关节/reference，不导mesh，所以实际native model不能再乘.9。原source local平面rawx/rawy/rawz对应GLBreference X/Y/Z，main准星/弩臂真实原生方向必须GPU验证，不套剑对角轴或三叉戟翻面。该raw映射已检查纹理/原A09提取脚本，但仍须真实quad submit验证，尚非最终验证。

先main真实RANGED2 reference/owner1-3/原instant归属/.32 Settle/default flat stationary unarmored/capeless；其他武器、off弩、侧腰、复杂/盔甲/正式第一人称保持原路。真未装填/已装填弩模型与Fuse/UV/glint、原装填完成一次ammo消费、原一次射箭/wear/背负登记、GUI/use/hurt/新物/实际owner退出逐项验证；不用Inventory交换造事件，不改原生装填/射击时间/物理/库存/伤害，也不假造back动作。无依赖/真实迁移/上传，混合许可源和包含数据包仅本机。

补充：back16×121数值检查已实际通过main raw(.90625,.09375,.5)/.9一次映射，保留原source reference平移旋转和22 skin关节，握点误差<.002；无需把main .125掌握位强行改成.0625。数值提案不是实际native ItemModel/SubmitNode/GPU/装填射击通过。私有draft只local-only/dev53，不在classpath；运行中的52资源/源码保持。

原逻辑边界核对：FusionItems.host不接受弓/弩，Fuse作用在箭和真实携盾，不能为美术取收扩大弩本体Fuse资格。BackEquipment.displayStack明确省略CHARGED_PROJECTILES（nested projectiles）；已装填弩背负仍沿原sanitized standby表示，不扩同步allowlist或迁移数据。真实手持加载态与原存档/库存中的装填数据完整保持，验证时分别说明，不能声称已装填箭在背部被完整同步。原BackCrossbowMixin是在原use consumesAction后登记（装填起始也可登记），必须遵守其真实事件而非自造“射击后才登记”规则。
