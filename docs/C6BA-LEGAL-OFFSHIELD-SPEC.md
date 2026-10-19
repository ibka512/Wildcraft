> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6BA 副盾真实事件：只读假设，未实现或验证

先完成C6AY/C6AZ原生翼兼容正式归档，不提前改变ShieldRig的handOwner1门禁。当前原生BackTransitions.switched只接受同reference 3↔1/2；using/Gliding/Climbing/currentSwing/attack-key/use-key/GUI/flight均真正blocked，不延后重播。已有F或main-off交换有真实中间owner/transfer，不是back事件，禁止伪造。

候选真实路径：先真实OFF_HAND盾格挡登记，真实主手空；用真实装备位滑翔伞起飞，Gliding使已有盾visual owner3，落地释放Gliding后恢复actual off shield owner2。若landing时静止站立、另两类没有事件，可能有合法ground draw2。R/L是盾的实际物理手臂，即mainArm反向，不是mainArm。当前ShieldRig已有按event.handOwner选择物理arm但在前面明确拒绝owner2。

尚未证实：落地视觉同步与GlideRig退出先后、BackTransitions是否blocked或canceledReference压掉事件、feet条件与.32窗口是否足够、原生手持/披风/ELYTRA模型真实掌柄与skin quads是否正确。先用仅gametest源集的被动事件记录，驱动真格挡/装备伞/自然跳起开伞/落地，记录real before-after reference/owner、blocked原因和actual Settle。不改生产clock、库存/输入/延迟或生成正例。

若实际有合法ground 3→2，则只讨论源DRAW/other-empty在已验证形态，真实source22/physical shield arm/main空；stow2→3往往在空中且blocked，不能因为有draw就声称双向完整。主手武器同时取收会有多个Settle，原互斥继续。没有合法事件则保留原路径并记录失败，不为全接入而改玩法。

本阶段首步只将被动probe放入gametest/专用metadata与配置，不修改任何生产class或源动画。actual运行前先typecheck；probe成功只是事件存在性证据，不是副盾源玩法完成。

## 实测后限定实现（候选75，非正式）

dev74冻结生产下，landing v1/v2都未开伞超时；v3采用已有Glide测试真实空中同步等待/新鲜按键后，32秒四例自然落地均有20 draw帧、0静止帧，不能以此开放source。另actual main bow正常磨损损坏：先真副盾格挡登记，再原生main Bow20tick射一箭消耗1ammo、原生最后耐久损坏。四skin-arm组合32秒均70–78个静止合法帧，minProgress0、其他事件0，未替换库存制造3→2。

生产只ShieldTransitionRig允许实际owner2 DRAW、默认背部/无cape或ELYTRA、main真空、无其他盔甲/其他Settle、站立静止与原生use/GUI/hurt/clock边界。真实physical arm=mainArm反向，沿原.32、原22骨/握位和即时native owner，不加源剪辑，七资源401/FP4保持。off STOW和其他未合格形态继续旧路，不变原生物理/伤害/库存/费用/计时。

先四基本源实际skin/native shield/GPU/掌柄/独立头与原一次arrow/ammo/Bow break/Shield wear7-Fuse31，再pause/GUI/重新举盾/torch/native FP观察及普通未损坏Bow、helmet/cape/ELYTRA负例，实际landing仍应不进入静止source。首切片后定向旧Shield back/cape/wings、Bow/Crossbow/装备/Guard/FP/Glide回归，同包必要检查125/datagen/build/资产/七资源与其他四WD保护。clean才正式75，尚不能称副盾所有取收或C6完成。无依赖/真实迁移/上传/待审资源采用。
