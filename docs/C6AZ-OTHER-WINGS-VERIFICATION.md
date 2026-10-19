> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6AZ 其他七类原生鞘翅兼容验收

本机dev74，2026-10-19，源码c7f7d776839a43500f5e20be27f3a2f634aee3c2，标签v0.1.0-dev.74+mc26.3。基于正式dev73剑/原生翼处理，新增斧、三叉戟、重锤、七材质长矛、主盾owner1、主弓和主弩。仅真实CHEST ELYTRA、其余三盔甲槽空、静止平地、两种皮肤与真实左右主手；近战/弓弩另一手空或盾，主盾另一手空或剑。新增7×16=112基本取收，与原剑共128。长矛另保留7材质×2skin×2arm=28配对/56取收，carry按组合选择，不冒称全材料全条件穷举。

生产仅七TransitionRig门禁增加真实chestELYTRA例外与WingsMixin显式family扩展，338classes中的八class变更。原Sword与其他生产class保持。仍沿原WAIST/同reference/即时owner，原.34/.38近战与.32盾弓弩真实时间；原输入、原生物理/库存/伤害/费用/计时/use/block/wear/Fuse保持，无代理堆栈或迟到GUI事件。副盾owner2继续拒绝。

七份原401第三人称/4FP资源逐SHA保持，几何绑定与作者源不改，不新增采用ID。翼层保留原版push/Z+.125/equipmentRenderer/pop，只有真正合格waist源frame乘body一次；独立测试观察原生ElytraModel实际提交矩阵/texture优先级、combined glint与非foil、原state折叠及pop无泄漏。不同family在gametest audit中声明，不往生产加测试开关。三种实际纹理路由（默认、cape fallback、elytra override优先）与foil有无均进入真实绘制。

沿各已有真实native模型、各自raw握位/尺寸/最终quads或special model审计，不把长兵器套剑.23坐标：斧原wrist .002阈值、三叉戟/重锤专属模型、七种native长矛尺寸/readiness/一次piercing-stab/wear-Fuse与nativePackets0、主盾真实blue/glint/block wear7/Fuse31、弓原20tick一次arrow/ammo/wear、弩真实装填/instant loaded/firework保持。原pause/10FPS/5TPS/独立头/GUI/use/hurt/item/armor复杂退出继续验证；剑自然jump/native elytra flight与原滑翔伞回归通过。不称精确拔入鞘、连续双手实握或所有动作的翼支持。

首编译12秒红来自四个复制测试的private progress方法引用仍指旧Waist类；未进入游戏、失败源码与日志保留compile-v1-source。仅改新测试的本类方法引用，编译2秒绿；无生产修复或阈值放宽。首七类实际结果见SEVEN-V1-RESULT.json，最终实际两组7+23共30类7 classes 8m 31s + 23 classes 22m 9s/1181验收PNG clean：
- dev.wildcraft.test.AxeWingsRigClientTest
- dev.wildcraft.test.TridentWingsRigClientTest
- dev.wildcraft.test.MaceWingsRigClientTest
- dev.wildcraft.test.SpearWingsRigClientTest
- dev.wildcraft.test.ShieldWingsRigClientTest
- dev.wildcraft.test.BowWingsRigClientTest
- dev.wildcraft.test.CrossbowWingsRigClientTest
- dev.wildcraft.test.SwordWingsRigClientTest
- dev.wildcraft.test.SwordWaistRigClientTest
- dev.wildcraft.test.AxeWaistRigClientTest
- dev.wildcraft.test.TridentWaistRigClientTest
- dev.wildcraft.test.MaceWaistRigClientTest
- dev.wildcraft.test.SpearWaistRigClientTest
- dev.wildcraft.test.ShieldWaistRigClientTest
- dev.wildcraft.test.BowWaistRigClientTest
- dev.wildcraft.test.CrossbowWaistRigClientTest
- dev.wildcraft.test.AxeTransitionRigClientTest
- dev.wildcraft.test.TridentTransitionRigClientTest
- dev.wildcraft.test.MaceTransitionRigClientTest
- dev.wildcraft.test.SpearTransitionRigClientTest
- dev.wildcraft.test.ShieldTransitionRigClientTest
- dev.wildcraft.test.BowTransitionRigClientTest
- dev.wildcraft.test.CrossbowTransitionRigClientTest
- dev.wildcraft.test.SwordTransitionRigClientTest
- dev.wildcraft.test.HeavyMeleeRigClientTest
- dev.wildcraft.test.FirstPersonSwordClientTest
- dev.wildcraft.test.EquipmentClientTest
- dev.wildcraft.test.BackEquipmentClientSmokeTest
- dev.wildcraft.test.GuardClientTest
- dev.wildcraft.test.GlideRigClientTest

非full48。不计世界icon/AppleDouble。冻结actual metadata/主包/源码/测试JAR/源SHA在外层2026-10-19-C6AZ/regression-execution-package，两组每张PNG及日志SHA在RESULT.json。旧ELYTRA-only负例按明确新范围改为真实ELYTRA+helmet并等头槽同步；复杂盔甲仍原路，旧证据保留，无重设golden。

独立datagen7秒68零写、build9秒125必需服务端与采用资产45/4/2107/57通过。原七资源与四其他美术WD逐SHA保持。主包23506175字节 SHA256 `4cdfdcc11d2521412b2db5d0e5724dc953d6c74242b085eb4153b9e64e3763ca`，sources23027938字节 SHA256 `c026cdbfb93d75c0f2ec9b2ec5f523a18354dc7f9e3f70c8c6c3435d3ec0af86`，CRC/SHA和对应源码已核；包/许可/七私有资源仅本机安装包/dev74/final，清单见development-assets/Wildcraft-dev74-local-manifest.json。

副盾真实合法source事件仍只有研究计划/测试草稿，不开放owner2，不伪造F/main-off/库存或延迟菜单事件。复杂盔甲、其他动作在ELYTRA下的全范围、其他第一人称、C6整体与C7人工长期生存未完成。C7-v03试玩zip仍dev62。本批无新增依赖/真实迁移/GitHub上传/待审13FP或另6采用；原件/历史/真实数据/其他四美术WD与untracked保持。
