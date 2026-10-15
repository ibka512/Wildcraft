# C6Z长矛背部源实施进度

2026-10-13从formal50/77ace69a与本机87d3d644继续，当前参数51候选未正式。源helper current wrist .0625/native raw(.70,.30,.5)/(-x,+y) shaft/+Z normal、actual display/local 1.7/1.7/.85保留一次；251总249选、旧243/几何绑定深保持，独立datagen通过。原根脚头/皮肤关节不动，source back .34/.38、原same-reference与new instant owner/原look/foot和action互斥沿前批。

新增限定spear_equipment_真实main nativeSPEAR item redirect（原PIERCING组件所拥有者、不是任意piercing item）；true SubmitNodeStorage/SubmitNodeCollection.submitItem原native quad pose握点/shaft observer、ItemStackRenderState身份嵌套还原、实际source skin/back和hilt-shield/轴矩阵、真实piercing-stab/nativeSTAB packet计数，source不attack。source其他hand是late辅助路径，不称持续实握/完整第一人称。

v1编译/实际native第一次piercing登记/wear1/Fuse31通过，但第一source stow超时，未source gameplay通过。查到BackEquipment.displayStack故意去掉PIERCING_WEAPON，sanitized VIEW仍是真native spear ItemID，却不能用MeleeWeapon.of(VIEW)判断；不扩网络数据/不恢复省略组件。仅stow用nativeSpear allowlist ItemID，draw仍实际held PIERCING组件+原真实reference检查。v2实际回归中，未通过宣称。

七材质actual native models/textures/tips与占手、完整33类/125构建和最终本机包尚待。formal50/历史/其他美术WD保持，无生产依赖/迁移/上传；混合源与数据JAR只本机。

v2 source16/native quad握点轴/原skin/back与20余退出已通过但原立即攻击SWING期待失败；原nativeSPEAR换槽冷却/cannotAttack最小5必须保留。v3按真实not-ready拒绝/source取消zero extra hit、原clock ready再一次nativepierce/stab/wear/Fuse，56秒28图通过；测试final加入真实serial处理等待，不把客户端短暂无源当权威取消。v4进一步28组材质×skin×左右手对（7种真正原生材质，WIDE携盾/SLIM空手），2分23秒84图全部通过：原piercing1/stab1/wear1/Fuse31/zero nativeSTAB-genericPlayerattack-extra cost、second ready hit piercing2/wear2/Fuse30、原skin/model.quads握点轴/source与核心退出通过。未用iron替换材质或烘第二次1.7scale。完整33/125构建/正式51尚待，所有失败与v2部分22图/v3保持。

独立datagen v2 68零写/build125必需服务端/45-4-2107-57实际通过。source51仅main nativeSPEAR/同reference/new owner、原.34-.38 Settle与原3D生成32px模型，实际原native piercing/notReady保护通过，完整33尚待；未formal51/tag，formal50保持。

完整33类25分38秒1131图实际通过，production b0775cf2；限定main七材质back辅助源完成，waist/复杂/持续二手/正式第一人称/C6整体仍未完成，下一Mace只有报告/提案/私有草稿，未runtime。
