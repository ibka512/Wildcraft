# C5 实际验收 / dev.23

2026-10-07，C5本机dev.23已完成，feature/perfect-dodge-counter；111项服务端/9类成品客户端回归通过，55张正式截图。僵尸/尸壳前摇6–9刻内实际侧后跳离开原生攻击范围才触发，单跳单次；无无敌帧。共用时间租约，短林克时间1.5有效实际秒、手动确认0.65秒，只付闪避8精力；一次按帧反击沿原生瞄准/范围/伤害/耐久/Fuse路径，不增加反击伤害。攻击恢复按有效实际时间，其他运动/受伤间隔仍原版世界刻。空中/下落攻击及两技能自动/手动/关闭设置、H确认均已接入。无新依赖/存档迁移/采用ID；公开仍dev.18，本批未上传。下一C6。

资源生成、正式构建、111项必需GameTest（新增8项）及9类成品客户端实际通过：Reaction、Guard、Melee、Combat、Input、Survival、Focus、CoreArt、BackEquipment。规则数值/输入/温度/风检查通过。正式包SHA-256：`49dae61d6565718a7a20e9d2fa4b188d61f24f47c14a3f4a9f8ce19d2f50a2ad`。

新增服务端覆盖临近威胁实际位移落空、停住仍原生命中、过早离开无奖励、非法位移取消、无威胁及伪反击/确认、非单人不减速、真实下落倍率上限/上升和地面排除、原生耐久/Fuse一次及旧偏好边界。客户端受控原生目标准备→实际绑定后跳→原生碰撞与移动包→奖励→帧反击通过；原版当前视角包先于意图，原生点击队列实际注入，测试专用攻击调用次数计为1，重复反击包无再次伤害/消耗。

手动/关闭弓和闪避、滤镜0不关闭技能、有效实际时间内武器恢复及超时、菜单/暂停/真实受伤/换维度/换装备/外部12TPS保持均通过。第一/第三人称空中/下落状态、标准/纤细/左右手/四槽盔甲和头部瞄准遮罩通过。旧自然AI前摇/完美盾/普通盾/真实箭、C3攻击/挖掘/侧后跳、锁定/独立防御、统一输入/探索、单人生存原料闭环/保存、空中弓及GPU表现、核心美术与背负通过。

[合同](C5-SPEC.md)、[日志及截图](../development-assets/verification/reaction/README.md)，55张正式截图。候选、失败和点击审计前完整跑图单列；计时日志包含网络回执与后续等待，不是性能基准。受控威胁、直接生命周期探针、控制下落场景与自然AI回归分开，未冒称全部人工体验。

[正式包](../development-assets/wildcraft-0.1.0-dev.23+mc26.3.jar)、[源码JAR](../development-assets/wildcraft-0.1.0-dev.23-sources.jar)、[完整阶段源码](../development-assets/Wildcraft-C5-source.zip)、[清单](../development-assets/Wildcraft-C5-manifest.json)/[校验](../development-assets/Wildcraft-C5-SHA256SUMS.txt)。本机标签v0.1.0-dev.23+mc26.3；公开仍dev.18。旧阶段冻结材料保持。

复现：先`./dev.sh --no-configuration-cache runDatagen`，再`./dev.sh -PacceptMinecraftEula=true --no-configuration-cache build runReactionClientTest`，最后`python3 tools/verify-adopted-assets.py`。新操作者自行接受EULA；按-P反应专项参数reactionOnly=true可隔离Reaction类，不能替代本次九类回归。不要并发构建改写运行中的测试JAR。

无新依赖、存档迁移或新美术采用；正式JAR无测试/原游戏源码，生产源码JAR与当前main/client逐字节核对。未验证真实控制器/Steam Deck、长期人工生存/战斗平衡、新独立服务端/多人。程序动作不等于完整动画资源；下一[C6](C6-NEXT-BATCH.md)，B6/机械底部与人工试玩仍待办。复杂物理、Boss/神庙/新维度等排除项不变。
