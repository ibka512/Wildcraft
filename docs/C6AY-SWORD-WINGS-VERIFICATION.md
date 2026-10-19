> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6AY 剑与原生鞘翅兼容验收

本机dev73，2026-10-19，源码8c9aff5722b9d7befa19e6676dc932a2f72c2af0，标签v0.1.0-dev.73+mc26.3。真实CHEST ELYTRA、其余盔甲空、静止平地，两体型×真实左右主手×空副手或盾16取收组合。原生same-reference归属和.34/.38真实时间不变，不称精确拔剑入鞘或持续双手实握。其他七武器翼兼容尚未开放。

本地26.3实际WingsLayer原pushPose→Z+.125→EquipmentRenderer→pop没有parent.body；新Mixin只对真正equipment_*_waist_* Frame与真实chestELYTRA，在原push后乘源body一次，原native翼模型、纹理优先级、光效、折叠/飞行姿态和其他层矩阵保留。SwordTransitionRig只允许该chest例外，其他盔甲仍拒绝；未改原生物理、输入、库存、伤害、费用、耐久、Fuse、时间和原生飞行/滑翔伞。

真正native翼submit/model/纹理与layer parent观察：矩阵2e-5、原生左右fold值1e-6、texture选择（无cape默认、cape fallback、专用elytra优先于不同cape）、foil与非foil、原pop不泄漏通过。源真实skin/剑盾GPU/脚/独立头和即刻owner沿既有检查。自然按键jump与原生ELYTRA flight均真实退出source，实际截图存在；不设置假flight或冻物理。

首v1客户端29秒超时，v3 probe同29秒重现；真实full35/wing pose46/foil46/model0/back46，原26.3采用combined armorCutoutNoCullGlint(texture)同一基础pass，旧Audit提前return漏计模型/texture。只修测试observer，阈值保持，DEBUG移除。v2只是DEBUG println泛型编译1秒失败，未启动游戏，残留旧JAR明确标记。旧失败包/meta/源/日志在外层C6AY各目录，FAILURES.md保留；生产主包SHA整个诊断保持不变。

首v4实际57秒/29验收PNG通过。最终实际13类11m 27s/520验收PNG clean：
- dev.wildcraft.test.SwordWingsRigClientTest
- dev.wildcraft.test.SwordTransitionRigClientTest
- dev.wildcraft.test.GlideRigClientTest
- dev.wildcraft.test.SwordWaistRigClientTest
- dev.wildcraft.test.AxeWaistRigClientTest
- dev.wildcraft.test.TridentWaistRigClientTest
- dev.wildcraft.test.MaceWaistRigClientTest
- dev.wildcraft.test.SpearWaistRigClientTest
- dev.wildcraft.test.HeavyMeleeRigClientTest
- dev.wildcraft.test.FirstPersonSwordClientTest
- dev.wildcraft.test.EquipmentClientTest
- dev.wildcraft.test.BackEquipmentClientSmokeTest
- dev.wildcraft.test.GuardClientTest

不计世界icon/AppleDouble；非新full48。实际执行包/metadata/源码SHA、每图SHA与日志位于外层2026-10-19-C6AY/regression-execution-package与RESULT.json。最终重跑原披风/背部/滑翔/重武器/FP/装备/格挡，仅旧Sword ELYTRA-only负例依据新明确范围改为真实ELYTRA+helmet并等头槽同步，原历史证据不改、没有重设golden。

独立datagen11秒68文件零写、build12秒125必需服务端与采用资产45/4/2107/57实际通过。相对dev72总337→338，只有SwordTransitionRig变更与新增CombatRigWingsMixin；其余生产classes不变，401第三人称/FP4和七JSON逐SHA保持，四其他美术WD SHA保持。主包23505733字节 SHA256 `e985c519610a48a236acd9e3d52844739d2e109c016689debf78f151af767c68`，sources23027682字节 SHA256 `fe3e79be45e5324b11062eb279e3c37ec5e935f5f0fa13781909d4b3c0079e4a`。JAR CRC/资源SHA/对应源码检查通过；包与七资源/许可仅本机安装包/dev73/final，清单见development-assets/Wildcraft-dev73-local-manifest.json。

仍未完成：其他武器ELYTRA/复杂盔甲、副盾真合法源事件、其他第一人称、C6整体与C7人工长期生存/手感。C7-v03试玩zip仍dev62未替换。下一其他七类草稿先核输入SHA再实施/编译验证；不是已完成。无依赖迁移发布/GitHub上传或待审资产采用。
