# C6D实际验收 / dev.27

2026-10-07。C6D本机dev.27已完成，feature/equipment-transitions；125项服务端/13类成品客户端回归通过，123张正式截图。同引用五类近战及盾/弓/弩的第一/第三人称取收程序基础已接入，真实皮肤/袖子/左右手/标准纤细/占手/侧腰与盔甲遮罩；空手回程不生成出手副本，原生换装缓存旧物品抑制，实际攻击/使用/GUI/探索互斥，暂停冻结与5TPS实际进度通过。自定义攻击与反击先同步原生携带槽，立即换装攻击正确归属。无新依赖、存档迁移或美术采用ID（仍43）；完整32项正式源稿、取收美术/驾驶/环境、设备输入与长期人工平衡仍待后续，C6整体未完成，多人暂缓。最后核对公开dev.18，本批未上传。

实际运行资源生成、正式构建、125项服务端旧GameTest与13类成品客户端：Equipment、Grip、Spear、Weapon、Reaction、Guard、Melee、Combat、Input、Survival、Focus、CoreArt、BackEquipment。正式JAR SHA-256：`a861794531977d45f6b95240a043f0163c3f3921ca1a7185c3ac4ce730757c2a`。源码JAR与main/client逐字节一致，正式包无测试探针或Minecraft游戏源代码；采用台账核对43 ID/30第二批版本/1779原始记录/48运行副本通过。只更新当前运行实现哈希与本机版本，冻结源稿及旧阶段证据不改。

新增Equipment实际按键/登记：五类真实攻击后登记，每类同一引用的空手收回/取出与第三人称原生身体提交；之后真实弓/盾使用登记与弩装填，原生快捷栏取收。空手收回不提交已在背部的旧缓存物品，双手直接转移与不同记录不虚构动画。副手盾来回测试是专用世界原有真实引用的受控库存转移，观察的是实际同步后生产GPU回程；不冒称这一步是鼠标库存拖拽。左惯用手及实际占用火把、换到新火把、鞘翅腰侧/盔甲、快切终止与相同外观备用矛隔离通过；普通换槽不登记。五类反复取收耐久/Fuse次数和精力不改变。

立即换装后真实ATTACK进入正确长矛准备，服务端已选槽4与SPEAR类型验证；使用/GUI立即接管且不补播，实际暂停冻结进度，专用世界原生tick rate 5下以实际时间完成，再恢复20TPS。原生携带槽同步调用先于自定义准备/松开/按帧反击；旧C5/C6B的一次原生伤害/穿刺/反击回归保留。原生地图/进食/攀爬/滑翔保持既有优先路径；探索生命周期在旧Input/Survival/BackEquipment回归验证，未新增多人。

受控矩阵另检查标准/纤细、左右手、实际皮肤/袖子、空/占副手、真实原生模型握点、有限提交矩阵及父矩阵恢复、组件/数量不改；原生头部与其他占用手保持，实际Humanoid盔甲setup使用同一遮罩一次。不可用/瞄准镜/旁观/激流资格隔离与不同token/直接手间交换不制造取收。受控骨架不是全部账号皮肤/第三方模型或硬件验收。

新增32张取收图与91张旧回归图共123张，实际查看第一人称剑/三叉戟/矛、盾/副手/回程、第三人称肩后与鞘翅腰侧。此为程序动作基础，无新的编辑器动作源稿，不能宣称完整32剪辑或正式取收美术完成。人工手感/长期平衡、驾驶/环境、设备/外部替换模型仍待后续；无实际阻塞。

诊断与最终证据分开：首编译按旧Minecraft字段访问screen，改为锁定26.3的gui.screen()；初版测试仅在服务端设选槽7、客户端/原生携带缓存仍0，导致客户端持剑、服务端空手，改为原生热栏输入初始化。候选五类/远程链路通过后立即换装攻击等待失败，补齐原生槽位同步顺序。初版截图原生抬手缓存把肤臂压出视野，取出补偿降低高度及三叉戟握点，第三人称目标移离镜头；暂停测试先等实际归属/提交回执后打开菜单，避免暂停在归属消息到达前。隔离通过不是最终验收；首轮完整门槛已通过，最后校正背部方向与激流/睡眠互斥后再次完整回归，最终仅取最后运行图与日志，diagnostics候选图可能含旧残留，只作诊断。

复现：先`./dev.sh --no-daemon --no-configuration-cache runDatagen`，再`./dev.sh --no-daemon -PacceptMinecraftEula=true --no-configuration-cache build runEquipmentClientTest`，随后`python3 tools/verify-adopted-assets.py`。equipmentOnly=true仅新类隔离，不能代替13类回归；专用运行目录初始化测试设置并清理旧截图，不触及个人世界。EULA仅本机已有授权，新操作者自行接受。

[规格](C6D-SPEC.md)、[制作/接入](animation/EQUIPMENT-TRANSITIONS.md)、[证据](../development-assets/verification/equipment/README.md)、[正式包](../development-assets/wildcraft-0.1.0-dev.27+mc26.3.jar)、[源码JAR](../development-assets/wildcraft-0.1.0-dev.27-sources.jar)、[源码ZIP](../development-assets/Wildcraft-C6D-source.zip)、[清单](../development-assets/Wildcraft-C6D-manifest.json)/[校验](../development-assets/Wildcraft-C6D-SHA256SUMS.txt)。本机v0.1.0-dev.27+mc26.3；不上传/发布、不迁移存档。
