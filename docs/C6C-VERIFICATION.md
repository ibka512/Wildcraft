# C6C实际验收 / dev.26

2026-10-07北京时间4:54定时开始，单次一批。C6C本机dev.26已完成，feature/first-person-grip；125项服务端/12类成品客户端回归通过，91张正式截图。核心近战第一人称真实皮肤握持基础：剑单手，斧准备/强击与三叉戟/长矛/重锤仅空副手参与支撑；五类实际按键轻强/原生一次伤害与恢复、空中/下落皮肤臂、实际左右主手、袖子及受控标准/纤细、盾/火把占手、GUI/原生使用/第三人称互斥通过。握点沿原生模型变换，手臂与武器/Fuse共享矩阵，保留原生一次结算；第三人称占用副手保留持物姿态。无新依赖、存档迁移或新增采用ID（仍43）。当前没有新玩家动作源稿，32项正式剪辑、取收/驾驶/环境、设备输入与人工长期平衡仍待后续；C6整体未完成，多人暂缓。最后已核对公开dev.18，本批未上传。

资源生成、正式构建、125项服务端旧GameTest及12类成品客户端实际通过：Grip、Spear、Weapon、Reaction、Guard、Melee、Combat、Input、Survival、Focus、CoreArt、BackEquipment。正式JAR SHA-256：`fd2de1c8aed91d84103f6c07779b25e6fd7b8fc9fa377db474930907641b78bb`；源码JAR与main/client逐字节一致，成品无测试探针或游戏源代码。采用库完整性另运行 verify-adopted-assets.py，43采用ID/30第二批版本/1779原始文件记录/48运行副本保持。

新增GripClientTest使用真实原生按键→生产意图→权威阶段→成品GPU skin臂提交探针，五类准备与释放均验证真实SWING皮肤臂、原生一次Player.attack或长矛一次PiercingWeapon.attack/逐目标stab、原版耐久、一次材料消耗与费用、恢复结束无残留。真实跳跃出手及传送到测试高处后自然下落出手验证AIR_SWING/DROP_SWING skin臂。真实左右主手同步（原生setMainArm）及三叉戟special模型/长矛握点通过；左主手加真实副手火把、右主手盾/火把不借用手臂，第三人称占手姿态保持。GUI取消、原生USE立即接管、第三人称无第一人称臂泄漏均通过。

受控标准/纤细与左右手采样沿真实原生模型解析及皮肤纹理，以实际提交到collector的ModelPart和矩阵检查袖子可见性、有限值、父矩阵恢复及物品组件不改动；不可见无肤臂、旁观者/激流/瞄准镜/错误物品与副手主动作资格隔离通过。该受控几何矩阵检查不等于真实外部皮肤账号/所有替换资源包或人工全设备验收。15张新增成品图与76张旧回归图共91张；查看最终五类与左右长柄、占手图确认手臂在视野中握着武器。第一人称不新增盔甲皮肤臂；原有第三人称四槽盔甲/头部/披风/鞘翅等沿旧回归。

[合同](C6C-SPEC.md)、[握点/制作边界](animation/FIRST-PERSON-GRIP.md)、[最终证据](../development-assets/verification/grip/README.md)、[正式包](../development-assets/wildcraft-0.1.0-dev.26+mc26.3.jar)、[源码JAR](../development-assets/wildcraft-0.1.0-dev.26-sources.jar)、[阶段源码ZIP](../development-assets/Wildcraft-C6C-source.zip)、[清单](../development-assets/Wildcraft-C6C-manifest.json)/[校验](../development-assets/Wildcraft-C6C-SHA256SUMS.txt)。本机标签v0.1.0-dev.26+mc26.3，旧阶段固定，本批未上传GitHub。

诊断单列：首次编译把26.3的PoseStack.rotate误写成mulPose(Quaternionf)，修正后资源生成通过；首轮完整测试虽通过，视觉图显示旧摄像机原点准备旋转将手臂移出视野，改为原生握点定位/围绕握点变换；定位后isolated候选通过并查看实际图。后续完整候选89图通过，再增加实际SWING与真实空中/下落提交覆盖，最终只采用最后一轮91图。旧左右主手模型fixture只翻主手标志，没有同步右/左手实际物品，现同步真实左右持物字段才能验证占用支撑手的合同。不是改生产规则规避测试；不将候选/旧图混为最终证据。

复现：先`./dev.sh --no-daemon --no-configuration-cache runDatagen`，再`./dev.sh --no-daemon -PacceptMinecraftEula=true --no-configuration-cache build runGripClientTest`，然后`python3 tools/verify-adopted-assets.py`。新操作者自行接受EULA；gripOnly=true只隔离新客户端，不能替代最终12类回归，不并发改写运行中测试JAR。

本批仅客户端程序握持/占手，持久格式、伤害/费用/同步合同无迁移，新玩家剪辑源稿未交付、未新增采用编号。完整32项正式剪辑、取收/驾驶/环境、硬件与长期人工战斗平衡尚未完成；无实际阻塞。定时任务只执行这一次，不使用额度重置或发布仓库。
