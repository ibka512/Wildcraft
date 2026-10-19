> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6AS 受伤与短跳的原生暂停契约

2026-10-19。接续正式dev66/a09690d5。只为已采用动画建立真实暂停资格，不新增技能、伤害类型、免疫帧、生产依赖或存档格式。Hurt与HOP分别验证，不能把已释放攻击的暂停白名单扩大到短跳。

Hurt：通过实际server hurtServer产生真实损血，观察实际提交Frame与同一AvatarRenderState的hurt/PARTIAL，先断言未暂停样本一致，再在5TPS/10FPS开启真实PauseScreen。原正式dev66主JAR逐SHA保持下两次实际失败27s/18s（源.5，对应应得.5149995/.5241669）。最小修复只保留原生已冻结partial，六刻到期/方向混合/当前native根骨盆腿头/伤害与击退不变。两体型×左右主手×empty/carry×四实际来袭方向=32项；每方向有非零fraction覆盖，全皮肤2e-5、世界刻/真实位置速度/真实血量在暂停中保持。举盾暂停合法放盾，原48组合guard专项另验。

HOP：真实锁定与X/方向键产生单次原HOP，观察实际提交和实际服务端收到的DODGE/CANCEL。先最小一项Pause观察，再两体型×RL×empty/carry/prior-guard×三方向×Pause/Inventory=72项。合法取消必须保留、确认实际服务端CANCEL和无迟到源/额外hit-wear-Fuse；原native阶段若真实仍存活，才要求源与其age/duration/partial一致，不伪造持续、不强造已取消姿态。移动、六刻/八精力、原碰撞/受击取消与完美闪避/ReactionTime保持，GUI中不能获得新格挡或奖励。

修改后独立datagen/build/125服务端、只读采用资产核对，及Hurt/Hop/Reaction/DodgeFeedbackModes/Jump/AirMelee/Melee/FirstPersonSword八类相关回归。所有失败、实际执行包/metadata/源快照、完整日志与PNG保留；最终同一生产JAR经全部实际验证后才正式dev67/tag。本机混合许可资源与含数据JAR不上传，原历史、真实数据和其他美术四WD/untracked保持。下一复杂取收/独立弓时钟与C7人工仍另列，不能声称整个C6完成。
