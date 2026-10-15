# C6B实际验收 / dev.25

2026-10-07。C6B本机dev.25已完成，feature/native-spear；125项服务端/11类成品客户端回归通过，76张正式截图。七种原版长矛接入18刻/16精力蓄力、原生最小恢复与距离/墙截断/多目标贯穿、逐目标耐久/Fuse、实际使用背负与有界程序突刺姿态。保留原版速度攻击/接触冷却/卸骑与副手真实堆栈归属；C5实际时间帧反击和原生STAB包零泄漏审计通过。原生手持/GUI不同矛尖的融合位置分别处理。无新依赖、存档迁移或美术采用ID（仍43）。C6整体仍未完成：完整动画源稿/第一人称双手握持、驾驶/环境、设备配置/真实手柄待后续；多人暂缓，公开仍dev.18，本批未上传。

资源生成、正式构建、125项服务端GameTest（新增8项）、11类成品客户端均实际通过：Spear、Weapon、Reaction、Guard、Melee、Combat、Input、Survival、Focus、CoreArt、BackEquipment。正式JAR SHA-256：`797a55cf5fe5737676d275f257a432edb834e029d76d3f189317f8819f032bd3`；源码JAR与main/client文件逐字节核对，成品无测试探针或原游戏源码。美术原件/采用库完整性另运行 verify-adopted-assets.py。

8项新增服务端使用真实生存玩家，覆盖七类/第三方组件隔离、直线两目标/旁观者/最小最大范围/墙、原生一次贯穿与逐目标stab/耐久/真实伤害Fuse、18刻蓄力/一次费用/恢复、原生攻击恢复/替换/使用取消、附魔/原ItemStack组件保存/分离回收/背负，以及原版静止无速度奖励、受控速度卸骑/接触冷却、副手使用只消耗真实融合长矛并记录背负。受控速度由原生setKnownMovement注入，不声称做了实机奔跑速度平衡。

新增客户端真实原版按键队列 → 生产输入意图 → 权威出手，单/双目标轻强击、无重复原生STAB包，原生USE取消近战，C5实际时间下恢复原生攻击强度后帧反击一次且不泄漏攻击包；标准/纤细/左右手/四槽盔甲与头部、GUI/第一/第三人称/固定展示的材料矛尖位置通过。反击机会用直接合格reward fixture，实际赚取闪避机会沿C5旧回归再次通过；不把fixture当自然闪避测试。9张新增截图及67张旧回归截图共76张。原生速度的完整运动表现/长期人工战斗平衡、完整第一人称双手握持和制作动画源稿未验收。

[合同](C6B-SPEC.md)、[最终证据](../development-assets/verification/spear/README.md)、[正式包](../development-assets/wildcraft-0.1.0-dev.25+mc26.3.jar)、[源码JAR](../development-assets/wildcraft-0.1.0-dev.25-sources.jar)、[阶段源码ZIP](../development-assets/Wildcraft-C6B-source.zip)、[清单](../development-assets/Wildcraft-C6B-manifest.json)/[校验](../development-assets/Wildcraft-C6B-SHA256SUMS.txt)。旧阶段包不变，未上传GitHub。

复现：`./dev.sh --no-daemon --no-configuration-cache runDatagen`，然后 `./dev.sh --no-daemon -PacceptMinecraftEula=true --no-configuration-cache build runSpearClientTest`，再 `python3 tools/verify-adopted-assets.py`。新操作者自行接受EULA；spearOnly=true只隔离新增客户端，不代替11类最终回归，禁止并发改写运行中的测试JAR。

诊断单列：初次客户端测试编译误用了keybinding命名空间；首次服务端延时蓄力的全局调用探针被并行用例覆盖，改为释放前同一服务端回调内重置探针再计数。既有C5客户端在新反击机会首次同步的一帧直接注入按键，触发输入系统对机会前边沿的隔离；用未按键的一帧先观察机会再注入真实新按键，保留原生产规则。采用库首次核对仍是dev.24运行文件哈希，最终登记本批实际代码哈希后再核对原件与采用ID。中途磁盘映像未挂载，恢复已注册卷后旧构建守护进程停滞，重启本批进程并改用单次守护模式。所有候选/失败证据不能作为最终包验收。C6整体、B6/机械底部、完整动作与真实设备仍未完成；无新持久格式或依赖，美术采用仍43。
