# C6A 实际验收 / dev.24

2026-10-07，C6A本机dev.24已完成，feature/long-heavy-weapons；117项服务端/10类成品客户端回归通过，67张正式截图。32项核心动作制作/接入CSV已整理；三叉戟16刻/14精力、重锤26刻/22精力独立蓄力，原生一次伤害/耐久/Fuse与实际使用背负、标准/纤细/左右手/盔甲程序遮罩。三叉戟投掷保持；重锤原生smash不叠加Wildcraft强击/下落加成或费用，保留原生摔落重置。立体三叉戟融合附着按原生模型坐标。未新增生产依赖、存档迁移或采用ID（仍43）。C6整体未完成：原生长矛专用贯穿、完整动画源稿与第一人称双手握持、驾驶/环境、设备配置/真实手柄待后续。公开仍dev.18，本批未上传。

资源生成、正式构建、117项必需GameTest（新增6项）及10类成品客户端通过：Weapon、Reaction、Guard、Melee、Combat、Input、Survival、Focus、CoreArt、BackEquipment。原有数值/输入/温度/风检查通过。正式JAR SHA-256：`669159250e6e935bbfb296b6d8f28ae41715d58d468fa322cacd84185bfd9744`。

新增服务端用真实生存玩家（不依赖会暗中保留创造属性的旧mock）；覆盖具体种类/同步Codec、轻击一次伤害/耐久/Fuse/无费用、不同强击门槛/费用/恢复、墙/实际瞄准/原生范围/同样物品替换取消、原生smash奖励/摔落重置/特殊伤害来源Fuse/背负且不叠加或收费、原生三叉戟投掷保留材料组件与反击原生一次路径。原生长矛与非武器不能被新手势/Fuse入口误接管。新增客户端实际原版按键→生产意图→权威两类阶段，12张新截图；轻/强原生消耗、真实三叉戟USE取消近战后投掷、背负/Fuse/鞘翅侧腰，标准/纤细/左右手的准备/地面/空中/下落和四槽盔甲一次遮罩、头部瞄准均通过。另检查GUI/第一/第三人称/固定展示的原生融合材料层，立体三叉戟附着在真实叉头坐标；普通sprite与special模型分开。

C5短林克时间实际按键、受控原生临近攻击落空、移动包与帧反击一次审计、设置/实际时间恢复及清理通过；C4自然AI/真实箭、C3剑斧/原生挖掘/侧后跳、锁定/独立盾、探索输入、生存闭环/保存、弓时间GPU、核心美术与背负全部旧回归保持。

[合同](C6A-SPEC.md)、[32项动作清单](animation/README.md)、[最终证据](../development-assets/verification/weapons/README.md)。67张正式图来自修正融合坐标后的最后一次完整运行。诊断单列：编译检查中用错FusionVisual字段/原生私有成员；服务端重锤伤害预期误按更高基础值；背负fixture未同步快捷栏选择；融合坐标修正前全通过运行；新模型检查误把缩放因子当实际成品尺寸，已按真实边界检查。不把候选图当最终成品证据。

[正式包](../development-assets/wildcraft-0.1.0-dev.24+mc26.3.jar)、[源码JAR](../development-assets/wildcraft-0.1.0-dev.24-sources.jar)、[阶段源码ZIP](../development-assets/Wildcraft-C6A-source.zip)、[清单](../development-assets/Wildcraft-C6A-manifest.json)/[校验](../development-assets/Wildcraft-C6A-SHA256SUMS.txt)。本机v0.1.0-dev.24+mc26.3；公开仍dev.18，未上传。正式JAR无测试探针/原游戏源码；源码JAR与main/client逐字节核对，旧阶段包固定。

复现：先`./dev.sh --no-configuration-cache runDatagen`，再`./dev.sh -PacceptMinecraftEula=true --no-configuration-cache build runWeaponClientTest`，最后`python3 tools/verify-adopted-assets.py`。新操作者自行接受EULA；weaponOnly=true只隔离新增客户端，不替代10类最终回归。禁止并发构建改写运行中的测试JAR。

无存档迁移/新依赖/新增采用ID；MeleeView瞬态类型byte由同dev.24双端同步，旧源码bool构造/axe()保持。程序姿态只验基础，不冒称完整动画源稿/第一人称双手握持/驾驶/全部硬件或长期人工战斗平衡。激流/忠诚代码路径未改，但未声称本批做了实际激流/忠诚全流程测试；投掷Fuse不新增元素效果。原生长矛贯穿仍原版，下一专门整合。C6整体、B6/机械底部、人工单人试玩尚未完成，无实际受阻项；多人继续暂缓。
