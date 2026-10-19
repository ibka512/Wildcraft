> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6AS 受伤暂停修复与短跳菜单资格

2026-10-19。本机dev67已完成本批验收与成品核对：32受伤暂停、72短跳菜单及八类相关回归同一生产JAR全部通过。正式源码/tag见ACTIVE及本机清单，旧失败和所有历史包保持。

## 真实旧版失败与修复

两次独立运行主JAR与正式dev66逐SHA相同，实际hurtServer伤害与实际提交AvatarRenderState：冻结hurt3时源进度.5，而原生fraction .08999705/.14500153对应应得.5149995/.5241669，分别27s/18s真实失败。未暂停同一Frame与native hurt/fraction数学一致，实际源时间恰好等于清零fraction后的值；当前root/pelvis/腿/head保护由既有before/after审计区分，证据支持清零fraction为根因，不是同步旧视图或累计旧基底。

最小修复只在HurtRig保留原生已冻结partial；六刻hurt、方向连续混合、真实伤害/击退/物理和当前native姿态保护保持。生产336class，恰好HurtRig一类变化；HopRig、MeleeControls及服务端class逐SHA相同。七私有JSON、四艺术WD逐SHA保持，没有新动画数据。

首次候选32矩阵30s在恢复后health==19失败；最小post诊断27s读取health19.833334、hurt0、skin和hands正确，证明原生自然回血已经发生。只修改恢复后测试允许[19,20]，暂停中真实一血损失与血量/位置/速度冻结原断言保持；没有关闭回血或修改任何游戏机制。DEBUG诊断已从源码删除，失败日志保留。此失败不是暂停公式仍错误。

旧暂停检查只验证反复读取的姿态稳定，且通过手工`createRenderState(...,1)`观察，未与实际提交的原生冻结fraction比较，所以稳定但错误的整刻姿态可通过。新增GPU提交观察在原调用边界比较时间一致性，补上这一检查缺口，无需新增生产计时抽象。

## 32项受伤暂停

两体型×左右主手×empty/carry×四实际来袭方向，5TPS/10FPS真实PauseScreen。完整1分54秒/32PNG，源时间误差在浮点记录精度下0，重复实际提交的全皮肤矩阵2e-5、世界刻/真实位置速度/血量冻结均通过。每方向有实际非零fraction：

| 来袭方向索引 | 实际项数 | 冻结fraction范围 |
| --- | ---: | --- |
| 0 | 8 | 0.0549978–0.25999677 |
| 1 | 8 | 0.03999722–0.34999704 |
| 2 | 8 | 0.054997325–0.25999713 |
| 3 | 8 | 0.10999715–0.2899964 |

本矩阵实际记录的剩余hurt均为3，不冒称逐个hurt计数与所有运动姿态的精确暂停矩阵。原正常专项另验六刻到期、48方向/装备组合及实际移动/空中/攻击并存。恢复后自然落地与原回血、skin/剑盾GPU、零新增attack/cost/wear/Fuse通过；原有native根骨盆腿与头保护、臂长不变。

## 72项短跳菜单资格

最小Pause探针23秒/1PNG：实际phase0/sourcefalse，服务端CANCEL2、其中从HOP取消1，原单次DODGE/八精力保持。随后两体型×RL×empty/carry/prior-guard×三方向×Pause/Inventory=72项，完整3分30秒/72PNG通过。实际全部phase0、sourcefalse，每项服务端快照至少一次cancelFromHop；最终收到的CANCEL、原单次DODGE/入场八精力、原有界方向/落地、原盾释放与无late hit/wear/Fuse逐项通过。Pause部分可能收到多次原CANCEL，不能误称只有一个取消包。

本批不修改HopRig或输入白名单；不是为动画持续而新增暂停免取消。72项没有真实存活的暂停HOP，故仅验收合法取消，不冒称活跃HOP的精确冻结fraction已获资格，旧代码保持。最小探针1图不混入最终72图。

## 相关回归与公共检查

八类Hurt/Hop/Reaction/DodgeFeedbackModes/Jump/AirMelee/Melee/FirstPersonSword完整8分50秒、263PNG实际通过。连同32受伤与72短跳图，共367张最终截图、9个不同客户端类；HurtRigClientTest只计一次，独立HopPauseClientTest另计。最终三组实际metadata、主/测试JAR与完整日志均冻结，生产JAR逐SHA一致。

原受伤48组合、当前native根腿/头/方向/臂长保护、移动/空中/攻击并存、blocked/GUI/换物/盔甲和六刻到期均通过；原短跳36普通组合及三方向/墙/八费/盾恢复/受击取消保持。完美闪避与手动/关闭模式、原时间租约和一次反击/Fuse、普通跳高/落地/摔落、空中剑斧、原近战/挖矿及第一人称16输入组合/8源/真实皮肤袖层/握点与暂停原生进度均通过。没有新增技能窗口或免取消规则。

独立datagen6秒、68文件零写入/零移除；build9秒、125项服务端全部实际通过。采用资产45用途/4底部变体/2107源记录/57运行副本通过。两体型仍各337第三人称/4第一人称剪辑，资源和原存档格式/依赖保持。

## 保存与范围

证据根`[原开发机外置卷]/Wildcraft/开发接续记录/2026-10-19-C6AS`，旧红例、测试修正反例与最小绿例分开保留；完整日志、PNG/逐SHA与实际主/测试JAR/metadata/源快照按组冻结。严格归档器只接受完整成功及唯一案例/类数一致，不合并失败或单项探针到最终数量。

旧正式红例、首矩阵反例与最终三组实际执行包分别冻结；恢复血量诊断的中间测试JAR未单独冻结，仅保留真实日志/PNG，不事后拼装原包。

原32Gi工程/同UUID、原件/所有历史/真实数据/其他美术四WD与untracked保持；测试仅外层新专用目录。混合许可源/派生资源/含数据JAR仅本机，无上传GitHub、新生产依赖、真实迁移或待审13FP/另6采用。

这不是新full48、整个C6完成、长时间性能或C7人工生存手感验收。复杂/腰侧/副手盾取收、其余正式第一人称及人工长期玩法仍未完成。下一C6AT-NEXT-BATCH，弓使用先按原生use独立资格，不套近战或hurt公式。

## 本机成品

主JAR和sources的ZIP CRC、七份内嵌JSON SHA与sources内HurtRig Java逐字节核对通过。两份包仅本机，标签`v0.1.0-dev.67+mc26.3`对应源码提交以清单为准。

- `wildcraft-0.1.0-dev.67+mc26.3.jar`：22240791 bytes；SHA256 `0b0b22d02f41ed4276b52e5cc26238ba5844d003d54a8213f69de9fb9c2e0ae8`。
- `wildcraft-0.1.0-dev.67+mc26.3-sources.jar`：21764333 bytes；SHA256 `8d65fa824c22e5c6907042c8ddb2f8941e85a5368b8f59b8aab17e702901cdf0`。

安装目录`[原开发机外置卷]/Wildcraft/开发接续记录/本机安装包/dev67/final`，清单`development-assets/Wildcraft-dev67-local-manifest.json`。旧C7 v03仍dev62，不能重标为dev67。
