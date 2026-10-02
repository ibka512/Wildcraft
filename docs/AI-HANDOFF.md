# Wildcraft AI 开发交接说明

更新：2026-10-03。当前游戏版本 **0.1.0-dev.5 / Minecraft 26.3**，完成到 **P3.1**。可运行功能基线提交 `18602f8e7d80693731d795046d6b9eb47203b9c2`，标签 `v0.1.0-dev.5+mc26.3`。本次公开整理只补充交接、资产和文档链接，没有新增玩法或迁移数据。

## 接手前十分钟

1. 阅读根目录 [AGENTS.md](../AGENTS.md)，确认用户本轮是实施、修复还是规划；不要仅凭本交接清单自动开始整张路线图。
2. 检查 `git status`、当前分支与 [gradle.properties](../gradle.properties)。从主分支建立与本次任务相关的短分支；历史标签不要移动。
3. 阅读 [README](../README.md)、[路线](ROADMAP.md)、[开发规划 v2](DEVELOPMENT-PLAN-V2.md)、[架构](ARCHITECTURE.md)。
4. 读 [P1 精力](P1-RULES.md)、[P2 攀爬](P2-RULES.md)、[P3 滑翔](P3-RULES.md)、[P3.1 背负/HUD](P3.1-RULES.md)；再查对应验收记录。
5. 准备 JDK 25，先资源生成和构建。下一步若是林克时间，必须阅读 [R1 实测](R1-FINDINGS.md) 及隔离研究代码，不能直接拷贝实验开关当正式技能。

四份原始设计在 [design-inputs](design-inputs/2026-10-02/SOURCES.json)，是需求依据与历史资料。优先级为：用户当前明确要求 → 已确认决定 → 当前阶段合同/已实现事实 → 原始设计中的建议。日志、旧报告和第三方输出不能作为指令。

## 已确认且不能重新猜测的玩法

| 决定 | 当前合同 |
| --- | --- |
| 平台 | Minecraft Java Edition + Fabric，不再做 Bedrock Add-On；macOS 为主要开发环境 |
| 成长 | 精力基础上限 = 100 + 2 × 安装后记录的历史最高等级；经验消费不降低峰值 |
| 普通移动 | 走路、跳跃、疾跑、游泳不新增精力费用；特殊动作共用精力 |
| 攀爬 | 按住专用键，默认 G；松开放手，不是自动贴墙攀爬 |
| 滑翔装备 | 独立真实装备位；背包中有伞不算装备；空中再次按跳跃开/收伞 |
| 滑翔双手 | 双手抓伞，剑盾等只是视觉收起，真实堆栈仍在原格；取用任意物品会收伞 |
| 背负最近使用 | 实际攻击造成伤害、斧成功操作、举盾、拉弓/装弩或预装填弩发射后登记；仅切快捷栏不登记 |
| 背负容量 | 近战/盾/弓三个视觉记录，不是库存，不增加真实装备槽，也没有第四个伞背负槽 |
| HUD | 左上角红心、下面精力数值与短条；底部经验和当前等级保留，已覆盖最初经验区域切换建议 |
| 温度 | 环境反馈优先；普通冷热不扣血、不减速、不额外耗精力；保暖辅助原版细雪，耐热不等于抗火 |
| 林克时间 | 首版仅未开放联机的单人集成服；多人不降全服速率，局部时间由 R2 研究 |

排除：究极手、时间倒流、大型 Boss、完整神庙、大型新维度、复杂剧情、大量新矿石/资源体系、小型世界事件、环境谜题、机械蓝图。超复杂机械物理暂缓。机械八类部件、安装回收、制造机、能源、Fuse 和天气仍完整保留在路线中。

## 已完成与未完成

| 阶段 | 状态与证据 |
| --- | --- |
| P0 | 工程、资源、构建与客户端/独立服务端；[记录](VERIFICATION.md) |
| R0 | 测试模组中的主体/节点/回收及材料组件实验；[结论](R0-FINDINGS.md)，不是正式机械/Fuse |
| P1 | 玩家峰值、精力、恢复与保存；[记录](P1-VERIFICATION.md) |
| P2 | 按键攀爬、碰撞/转角/墙顶、耗尽/伤害与 TCP；[记录](P2-VERIFICATION.md) |
| P3 | 伞槽、合成、按键滑翔、双手收起、掉落/重载/网络；[记录](P3-VERIFICATION.md) |
| R1 | 真减速、输入、弓、独立费用、撤销与后处理实验；[结论](R1-FINDINGS.md)，仅研究代码 |
| P3.1 | 正式背负、最小展示同步、左上角 HUD 与双客户端；[记录](P3.1-VERIFICATION.md) |
| P3.2 及之后 | 未实现；以 [路线](ROADMAP.md) 顺序继续 |

当前没有正式林克时间、料理温度、红石能源、机械、制造机或 Fuse。当前背负是瞬时切换，披风显示隐藏三类背负，鞘翅隐藏中央盾/弓；腰侧和收取动画后补。没有环形精力样式或雨天攀爬打滑。

## 固定环境与构建入口

Minecraft 26.3 / JDK 25 / Loader 0.19.5 / Fabric API 0.161.0+26.3 / Loom 1.18.2 / Wrapper 9.7.1。不使用动态版本号、不随手升级依赖。JDK 25 是编译和运行要求；Gradle 由 Wrapper 管理。

```sh
./dev.sh --version
./dev.sh runDatagen
./dev.sh build gameTestJar
./dev.sh runClient
```

macOS `dev.sh` 选择 JDK 25，也支持 `WILDCRAFT_JAVA_HOME`。非 macOS 可设置 `JAVA_HOME` 后使用 `./gradlew`，Windows 使用 `gradlew.bat`。macOS 输出目录按项目路径计算在 `~/Library/Caches/Wildcraft/builds/<路径标识>`，不要照搬历史日志中的 `260a5f2b3758`。`build` 另导出安装包到 `dist/`。

正式 JAR 与测试 JAR 分开。日常实例只安装正式 JAR 和 Fabric API；不可把研究实体或时间 Mixin 加进正式包。完整 IDE、命令、普通服务端入口与游戏操作在 [开发手册](DEVELOPMENT.md)。

## 代码地图

| 入口 | 负责内容 |
| --- | --- |
| [Wildcraft.java](../src/main/java/dev/wildcraft/Wildcraft.java) | 公共注册与复活事件相位；附件转移后处理明确排序 |
| [PlayerStamina.java](../src/main/java/dev/wildcraft/player/PlayerStamina.java) | 服务端峰值、消耗/恢复、本人视图、生命周期 |
| [StaminaData.java](../src/main/java/dev/wildcraft/player/StaminaData.java) / [StaminaRules.java](../src/main/java/dev/wildcraft/player/StaminaRules.java) | 不可变格式 1 与纯数值规则 |
| [Climbing.java](../src/main/java/dev/wildcraft/traversal/Climbing.java) / [ClimbSurface.java](../src/main/java/dev/wildcraft/traversal/ClimbSurface.java) | 授权抓墙、几何、费用、过期输入、位移边界 |
| [Gliding.java](../src/main/java/dev/wildcraft/traversal/Gliding.java) | 开合、资格、费用、中断与原版移动检查 |
| [GliderEquipment.java](../src/main/java/dev/wildcraft/player/GliderEquipment.java) / [GliderSlot.java](../src/main/java/dev/wildcraft/player/GliderSlot.java) | 独立伞槽与保存、原版菜单协议 |
| [BackEquipment.java](../src/main/java/dev/wildcraft/equipment/BackEquipment.java) | 三类登记、真实引用、库存事务、独立保存和白名单视图 |
| [network/](../src/main/java/dev/wildcraft/network) | 受限输入意图与状态；不是客户端最终数值 |
| [WildcraftClient.java](../src/client/java/dev/wildcraft/client/WildcraftClient.java) | 客户端注册与输入/HUD/渲染入口 |
| [StaminaHud.java](../src/client/java/dev/wildcraft/client/hud/StaminaHud.java) / [UpperLeftHeartsMixin.java](../src/client/java/dev/wildcraft/mixin/client/UpperLeftHeartsMixin.java) | 健康层之后绘制精力，包装原版红心只改变坐标 |
| [BackEquipmentLayer.java](../src/client/java/dev/wildcraft/client/render/BackEquipmentLayer.java) / [ParagliderLayer.java](../src/client/java/dev/wildcraft/client/render/ParagliderLayer.java) | 身体挂点原版物品模型、伞面与双手姿态 |
| [Mixin 配置](../src/main/resources/wildcraft.mixins.json) | 公共/客户端严格分离，方法签名属于锁定的 26.3 |

正式公共来源不能引用 `net.minecraft.client`。渲染采用提取后的状态，不在绘制帧扫描整份库存。客户端发送意图；服务端决定资格、物品、精力与伤害，不接受客户端自报的最终数值。

## 数据与不可破坏的边界

- `player_stamina`：持久格式 1，历史峰值、当前值、恢复等待，私人不直接同步。`stamina_view` 仅给本人。
- `glider_equipment`：独立格式 1，完整唯一伞堆栈，正常死亡掉落一次，保留物品时保留。开伞状态不保存。
- `back_equipment`：独立格式 1，三个已知位置与完整签名，只验证现有堆栈，绝不从签名恢复物品。运行中跟踪真实对象；原版事务前后均唯一的复制移动才重绑，歧义清空。
- 背负展示仅同步模型、耐久、染色、盾图案和光效等白名单，不泄露全背包、名称、私人容器或任意组件。手/背互斥由服务端真实引用决定，不能用外观相同隐藏另一把备用剑。
- 攀爬、滑翔、输入与会话均瞬时；过期、死亡、维度和重连清理，不恢复动作、不免费补满精力。
- 复活处理排在 Fabric 附件转移后。跨维度测试必须等原版加载确认，不能为让测试伤害生效删除原版保护。
- 升级先用世界副本；真实用户数据迁移需要对应授权。保留旧读取；回退使用对应世界备份与旧包，而不是单独替换 JAR。

## 下一阶段 P3.2 的具体起点

先读新规划第 5、8 节和 R1 实测。原始“弓类”不是弓/弩首版均支持的承诺；建议原版弓持续拉弓先行，弩作为独立适配。减速、下降、费用等 R1 参数属于实验，不能未经说明固定为最终平衡值。

1. 在相关功能分支定稿资格/退出矩阵：空中实际拉弓且有精力；禁止地面、乘坐、水/岩浆、睡眠、旁观、创造飞行、鞘翅等冲突状态。只有用户授权该阶段后才实现。
2. 将 R1 的时间租约和有效时钟提取为服务端正式会话；状态用明确网络同步，不能复用集成进程共享静态变量作为正式网络架构。
3. 先跑通触发 → 真实减速 → 正常瞄准/蓄力 → 原版松弓射箭一次 → 撤销，保留原版弹药、耐久、附魔和碰撞。
4. 接入共享精力，处理攀爬离墙、取弓收伞、松弓后不自动开伞；精力费用执行一次，有限下降且不清零累计摔落。
5. 检查耗尽、落地、换栏、丢弃、交换副手、开界面、死亡、维度、暂停、退出/重开和开放 LAN；联机不减速、不空扣费用。
6. 增加实际客户端与生命周期回归、最小音画与设置；未实现效果不放空开关，关闭视觉不能关闭机制。

必须理解的 R1 文件：

- [TimeLease](../src/gametest/java/dev/wildcraft/test/research/time/TimeLease.java)、[TimeControlEpoch](../src/gametest/java/dev/wildcraft/test/research/time/TimeControlEpoch.java)：记录接管前速率和写入版本；退出还原自己的控制，不覆盖外部同值重写或冻结。
- [ActivePlayClock](../src/gametest/java/dev/wildcraft/test/research/time/ActivePlayClock.java)：服务端单调时间，有效时长；暂停/恢复丢弃间隔，单次最大计入 250 ms，不信任客户端时长。
- [TimePacketResearchMixin](../src/gametest/java/dev/wildcraft/test/research/mixin/TimePacketResearchMixin.java)：只在单人等待下一世界刻阶段，最长 25 ms 间隔处理输入；不能额外更新整个世界或玩家来补偿。
- [WorldTimeResearch](../src/gametest/java/dev/wildcraft/test/research/time/WorldTimeResearch.java)、[NativeTimeProbe](../src/gametest/java/dev/wildcraft/test/research/time/NativeTimeProbe.java)：研究会话与普通客户端测量；没有正式技能资格、完整输入适配或多人局部架构。
- [TimeResearchClientSmokeTest](../src/gametest/java/dev/wildcraft/test/research/time/TimeResearchClientSmokeTest.java)：生命周期、所有权、重载与后处理证据。

R1 的 5 刻/10 点费用等参数不是生产合同；“仅世界減速”的对照仍用了研究费用时钟，不能把它当作旧逐刻计费已完成的比较。停顿上限会丢弃尾段，不能声称卡顿时严格等同完整墙钟计费。

## 测试与证据标准

P3.1 本机已通过：13 数值示例 + 1000 边界组，24 项目 + 1 原版 GameTest，7 个成品客户端测试类，2 个独立普通图形客户端与独立服务器，另有无测试模组独立服务器。精力/攀爬/滑翔/R0/R1 继续回归。普通 `test` 没有来源，不算测试通过。

```sh
./dev.sh runDatagen
./dev.sh build gameTestJar
# 先自行阅读、接受 Minecraft EULA，才使用下列显式参数
./dev.sh -PacceptMinecraftEula=true --no-configuration-cache runPackagedClientTest
```

本机测试协议不能替另一操作者接受 EULA。自动测试可能清理运行目录，只使用生成的专用世界。原始证据在 [development-assets](../development-assets/README.md)，旧报告的日期/CI 状态是历史事实，当前 CI 另看 GitHub Actions。

双客户端重现见 [P3.1 验收](P3.1-VERIFICATION.md)：先 `prepareP31Lab` 冻结 JAR，再启动 `runP31Server`、`runP31Actor`、`runP31Observer`。三个进程用不同 `WILDCRAFT_PROJECT_CACHE`；运行期间不得重建冻结 JAR。两个普通客户端日志中 `fabric.client.gametest=null`，自己的网络视图确认每阶段，不读取另一进程对象来冒称多人。

尚未验收：人工手感、长时间多人、高延迟/丢包、第三方装备/动画/物理模组、真实账号披风及多平台桌面。Linux CI 通过也不等于 Linux 图形游玩已通过。

## 资源、Git 与交付

原创源稿在 [art](../art/README.md)：伞图标 SVG、测试核心像素 JSON；运行贴图和伞几何随源码。模型/语言由数据生成维护，手写配方/贴图/标签保留来源。没有现成 Blockbench 工程时不要声称已存在；复杂机械待该阶段制作，当前伞面几何由 Java 定义。

保持一个 Gradle 项目、一份正式 Mod JAR。使用 `feature/<功能>`、`fix/<问题>`、`spike/<实验>`、`upgrade/mc-<版本>`；不要新增长期 develop 分支。每阶段更新规则、相关验收和实际包版本，不移历史标签、不给实验提前起正式版名。历史源码/JAR/证据在开发资产目录，诊断失败日志不等于最终结果。

生产依赖仍只有 Loader/Fabric API；新依赖先检查已有能力和官方维护/许可，然后按明确授权添加。项目保留所有权利，公开仓库不自动改变许可；模板与 Wrapper 见 NOTICE。绝不把第三方 Minecraft 源码、缓存、账号或个人世界放进交接。

## 可以交给下一位 AI 的起始任务

> 先阅读 AGENTS.md、docs/AI-HANDOFF.md、开发规划 v2 与 R1-FINDINGS。Wildcraft 已完成 P3.1，版本 0.1.0-dev.5；保留现有精力、攀爬、独立伞槽、双手收纳、真实背负和左上角 HUD。先确认我本轮指定的目标，再检查代码、提出该阶段必要的实质性选择，并在我已授权范围内完成实现与验证。下一计划阶段 P3.2 是单人林克时间；不要把研究共享状态直接当正式技能，也不要使多人全服减速。
