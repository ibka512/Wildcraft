> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6AW 七材质长矛披风腰侧验收

2026-10-19，本机dev71，source 5ba762e9c539a6bb05913b6f34258511c4673586，tag v0.1.0-dev.71+mc26.3。只真实visible cape/盔甲全空/静止平地，真实主矛/空副手或盾，两体型和真实左右手。Iron完整16取收，另7材质×2skin×2arm=28配对/56取收（carry依组合选择）；不是全材料全条件穷举。

新每体型8段，369→377第三人称/4FP；旧369与几何绑定逐值、其余四JSON逐SHA保持。原source32根/脚/头/绑定与raw握点失败报告保持；沿原已验证每采样wrist+.0625/盾+.125和真正32px native(.70,.30,.5)/原1.7一次适配，原22骨/物品旋转和尺度保持。生产只SpearRig/CapeMixin两个class变化，总337；不动物理/输入/库存/伤害/费用/use/block/wear/Fuse/计时。

实际native材质模型、最终native generated-quads握位/轴覆盖衔接，source full矩阵2e-5/轴dot.99999/hilt-shield palm .015保持。真实source skin/袖、原生cape texture/model/body矩阵、真实single held/back owner、原生独立头/脚通过；人工查看netherite/SLIM/right原模型/Fuse前视图，NPC真实登记后侧移，ready下一击前真移回/等客户端同步。

原SpearAttackAudit实际piercing1/stabs1/nativePackets0/CounterAttackAudit0、wear1/Fuse31/100精力保持，not-ready真正取消source而不造伤害或费用；原25ticks readiness恢复后实际第二piercing/stab2/wear2/Fuse30保持。不是按剑的Counter1或固定SWING猜测。实际main kinetic使用与专用DEFEND真正OFF_HAND盾分别立即接回原路；pause/10FPS/5TPS原.34/.38、GUI关闭不重播/新torch/原hurt/spare/真实ELYTRA回退通过。旧背部/已接四类腰侧和第一人称、盾/装备行为保持。

首切片2m45s/85验收图clean。最终11类11m28s/409验收PNG全部clean：SpearWaist、原SpearTransition、Trident/Mace/Sword/Axe waist、HeavyMelee、FirstPersonSword、Equipment、BackEquipmentSmoke、Guard。实际11metadata/主源test/源SHA/全部PNG和log冻结，材料draw与stow各28张、native not-ready/ready截图确实存在；不计world icon/AppleDouble，非新full48。本批无新增游戏失败。

独立datagen5秒/68文件零写零移除，build9秒/125必需服务端全部通过，45用途/4底部/2107源记录/57运行副本通过。主包23112416字节 SHA256 `60cb0d67b55001ddfb456f2698a38d8d61b358ce9f35a7b615a043e371d28aab`，sources22635134字节 SHA256 `ea53ce053fdc48b0214d6580a6db61de416938999551c3659f0fbde8b18bb553`。包/许可/7私有资源CRC-SHA仅本机安装包/dev71/final，工程local-only入口。完整清单development-assets/Wildcraft-dev71-local-manifest.json，外层C6AW/RESULT.json。

仍未完成主盾/弓弩腰侧、副盾真合法事件、盔甲鞘翅/复杂/其他正式FP/C6整体/C7人工。无依赖/迁移/上传，四美术WD/untracked与全部历史保持；下一C6AX只有三份原32source资格和计划，尚无72运行通过。
