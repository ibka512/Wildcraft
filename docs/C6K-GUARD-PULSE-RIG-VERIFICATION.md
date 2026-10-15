# C6K本机dev.35验收

2026-10-13，feature/adopted-art-runtime。真实完美格挡的限定活动盾臂源反馈，以及GUI换装取消竞态修复完成。C6整体未完成，未上传；dev.34旧包保持。

## 实际检查

最终完整runGuardRigClientTest v3：15类成品单人客户端，8分6秒，496截图。源盾专项v1和清晰v2各9图，v2实际28秒；初版图被目标Husk挡住，只改夹具为命中后同方向移到7.5格，保留命中数值与初图诊断。

GuardRig专项真实fresh native front block、标准/纤细左右主手、原版真实皮肤/蓝色glint/Fuse盾和主剑抵达GPU；盾实际磨损/Fuse一次，生命保持20、counterattack0、精力100，无新盾击/费用。当前渲染的活动臂矩阵确实变化，root/身体/双腿/头及另一手与叠加前矩阵一致；8世界刻结束回当前举盾，普通持续格挡继续磨损且不重新奖励/播pulse，非剑主手空手保留旧路。

原GuardClient自然AI前摇/真实格挡/硬直/箭反射、后向不免疫、同外观换盾/GUI/维度/重生与旧双身形/左右手盔甲遮罩通过。人工构造的旧Native模型遮罩检查明确清除新Rig键，继续核对原fallback而非让新源隐藏旧测试。其余HeavyMelee、LockedWalk、Glide（实际108投影5292安全射线）、Climb、Shield、RigRuntime、EquipmentGui、Equipment、CoreArt、Gliding单人、WindArt、Survival、BackEquipment均通过。生存实际制造/保存重开保持。

独立runDatagen成功（68文件不改），build/既有纯规则/runGameTest125/125与资产45用途ID、4底部、2107源记录/57运行副本核对通过。BackTransitions已有登记运行副本的SHA随实际代码改动更新，原美术交付清单/SHA保持，不修改原件。无新生产依赖、存档迁移或公开接口改变。

## 源遮罩与边界

只使用perfect-guard/v01的标准/纤细×左右主手4 pulse分离剪辑（每身形2），.40s/8世界刻；cycle展示不接。资格检查实际root/body/腿/头/另一手固定、活动臂/挂点闭合回起点，数据仅源准备，实际运行另验收。

GuardRig把源pulse相对起始帧的活动臂变化共轭到当前合格raise/hold身体空间，再按当前真实混合权重叠加。没有直接将全身样片覆盖当前身体。头继续独立原生瞄准；旧程序pulse只在新源确实拥有该帧时抑制，第一人称原反馈保持。主剑/副手盾模型仍原生一次提交，染色、glint和Fuse同一对象，源示意装备不装入游戏。

目前只锁定主剑/真实副手盾、地面静止、无盔甲披风/蹲伏默认尺度；移动、主手盾、其他武器/占手、盔甲/复杂状态继续旧程序/原生姿态。既有8刻玩家盾臂反馈、12刻敌方硬直和真实block规则保持，没有引入移动静态全身覆盖或跳中格挡。

## 两条GUI竞态与真实红绿

首次完整14类在原Equipment GUI“不重播取出”失败。独立真实Inventory在两次avatar提取之间开闭，旧draw进度.1278→.1474，同ref/owner稳定复现；只靠渲染观察漏掉取消，改到实际非暂停Screen初始化/移除消费。

单类初绿及1TPS原生carried同步补充曾通过，但完整15类v2又在迟到owner的首个观察帧失败。最终消费同ref、当前真实持物/选槽未变的迟到一次更新，实际物品/槽/ref变化解除此候选；独立v4 22秒首帧/GPU零draw、新一轮真实合法draw，以及完整15类v3原Equipment GUI/暂停/1.2秒5TPS全部原条件通过。没有延长等待或放宽断言。详细根因、编译失误复用既有Invoker及失败证据见C6K-EQUIPMENT-GUI-REGRESSION；诊断日志/两次失败截图保留。

## 本机包与下一项

development-assets/local-only/dev35含JAR/sources、115源剪辑/113可选快照与guard资格报告；仍22蒙皮骨/28含引用骨/12网格1792顶点。两个旧shield_ready未选择。SHA/ZIP已实际检查，元信息Wildcraft-dev35-local-manifest.json。许可混合仅本机，不直接公开包，也未采用13第一人称或另6补齐。

下一C6L已有侧/后跳4变体36动作源资格，尚未实现。已核对原规则HOP起手releaseDefense/stopUsingItem，计划只转换空/携盾24源剪辑，跳前举盾/跳后原控制恢复仍真实验收；源guard不冒称跳中block或改变规则。其他未接核心动作、复杂状态、正式第一人称、真实设备与长期人工平衡仍未完成，多人暂缓。
