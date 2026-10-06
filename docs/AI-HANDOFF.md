# Wildcraft AI 开发交接说明

> 品牌dev.18已接入并公开，main/Release交接标签为handoff-dev18-2026-10-06。本批只改A14双版本、Mod128与设置页原生20、README展示，游戏规则/保存/依赖不变；入口见[品牌验证](BRAND-ICONS-VERIFICATION.md)。旧公开快照/标签/包不移动。


更新：2026-10-06。当前公开开发交接版 **0.1.0-dev.18 / Minecraft 26.3**，GitHub默认分支`main`，交接标签`handoff-dev18-2026-10-06`。游戏标签`v0.1.0-dev.18+mc26.3`固定品牌更新提交`d3a0033`；正式JAR已更新品牌图标，玩法和保存格式保持。P10.2单人取得入口、原生循环和显示收尾已完成，最终证据见[P10.2合同](P10.2-SPEC.md)/[验收](P10.2-VERIFICATION.md)。保留dev.16风天气、dev.15第二批29项美术和既有玩法；下一步人工单人生存、定向平衡与B6/机械底部美术。多人R2/P10.1暂缓。

当前本机入口见 [LOCAL-DEVELOPMENT](LOCAL-DEVELOPMENT.md)，其他机器按 README 克隆并安装自己的 JDK 25，不需要本机磁盘映像。后续执行以 [NEXT-DEVELOPMENT-PLAN](NEXT-DEVELOPMENT-PLAN.md) 和 [ROADMAP](ROADMAP.md) 为准；旧规划 v2 的历史完成状态不可覆盖当前事实。

## 接手前十分钟

1. 阅读根目录 [AGENTS.md](../AGENTS.md)，确认用户本轮是实施、修复还是规划；不要仅凭本交接清单自动开始整张路线图。
2. 检查 `git status`、当前分支与 [gradle.properties](../gradle.properties)。从主分支建立与本次任务相关的短分支；历史标签不要移动。
3. 阅读 [README](../README.md)、[路线](ROADMAP.md)、[开发规划 v2](DEVELOPMENT-PLAN-V2.md)、[架构](ARCHITECTURE.md)。
4. 读 [P1 精力](P1-RULES.md)、[P2 攀爬](P2-RULES.md)、[P3 滑翔](P3-RULES.md)、[P3.1 背负/HUD](P3.1-RULES.md)、[P3.2 林克时间](P3.2-RULES.md)；再读 [P4A 温度](P4A-SPEC.md) 及对应验收记录。
5. 准备 JDK 25，先资源生成和构建。当前林克时间由正式服务端会话和附件同步实现；R1 仍留作历史对照，不使用研究共享静态状态作为正式网络。

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
| P3.2 | 正式单人林克时间、有效时间费用、原版一次射箭、缓降保留摔落与画面设置；[记录](P3.2-VERIFICATION.md) |
| 核心美术 v1 | 交付的 14 编号资源已接入；[规格](CORE-ART-INTEGRATION.md)、[验收](CORE-ART-VERIFICATION.md) |
| P4A | 本机环境温度完成；[规格](P4A-SPEC.md)、[验证](P4A-VERIFICATION.md) |
| P4B | 本机料理完成，见 [规格](P4B-SPEC.md) / [验收](P4B-VERIFICATION.md) |
| P5 | 本机能源与有限电池完成，见P5-SPEC/VERIFICATION |
| P6 | 主体、六节点、风扇/电池、实际吸附安装/拆卸、启停/乘坐、保存与回收闭环；见[P6规则](P6-SPEC.md) / [完整验证](P6-VERIFICATION.md) |
| P7 | 八类部件、载重、有限燃料/冷却/供电、原版碰撞和保存；[规则](P7-SPEC.md) / [验收](P7-VERIFICATION.md) |
| P8 | 制造机真实输入输出、一次结果、搬运和保存、共享菜单；[合同](P8-SPEC.md) / [验收](P8-VERIFICATION.md) |
| P9 | 本机dev.14完成，71服务端/16类客户端/双普通客户端及独立服务端；[合同](P9-SPEC.md) / [完整验收](P9-VERIFICATION.md) |
| P9.1 | 同正式dev.14，72服务端/完整17类客户端组合回归通过；[验收](P9.1-VERIFICATION.md) |
| 第二批美术 v2 | dev.15 接入 29 项采用资产，F05-10 v02 为当前；[记录](SECOND-ART-VERIFICATION.md) |
| P10 | dev.16单人风与当地天气首版；[验收](P10-VERIFICATION.md) |
| P10.2 | dev.17单人取得入口、原生闭环、轮子与HUD收尾；[验收](P10.2-VERIFICATION.md) |
| 后续单人阶段 | 人工试玩/定向平衡、B6及底部美术，再做候选版；R2/P10.1暂缓 |

当前没有多人林克时间；正式Fuse已完成本机dev.14，P9.1组合也完成，R2研究暂缓，P10单人首版已实现，后续优先单人生存平衡。当前背负按服务端真实归属立即切换，再执行 140–200ms 局部落定；攻击/使用不延迟。披风或鞘翅下改用侧腰布局；五类真实物品仍保持原有尺度和库存归属。没有环形精力样式或雨天攀爬打滑。

## 固定环境与构建入口

Minecraft 26.3 / JDK 25 / Loader 0.19.5 / Fabric API 0.161.0+26.3 / Loom 1.18.2 / Wrapper 9.7.1。不使用动态版本号、不随手升级依赖。JDK 25 是编译和运行要求；Gradle 由 Wrapper 管理。

```sh
./dev.sh --version
./dev.sh runDatagen
./dev.sh build gameTestJar
./dev.sh runClient
```

macOS `dev.sh` 选择 JDK 25，也支持 `WILDCRAFT_JAVA_HOME`。非 macOS 可设置 `JAVA_HOME` 后使用 `./gradlew`，Windows 使用 `gradlew.bat`。macOS 默认输出在 `~/Library/Caches/Wildcraft/builds/<路径标识>`；设置 `WILDCRAFT_CACHE_HOME` 或检测到本机运行标记后，使用所选 APFS 缓存根下的 `builds/<路径标识>`，不要照搬历史日志中的 `260a5f2b3758`。`build` 另导出安装包到 `dist/`。

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
| [FocusTime.java](../src/main/java/dev/wildcraft/focus/FocusTime.java) / [TimeLease.java](../src/main/java/dev/wildcraft/focus/TimeLease.java) | 自动资格、服务端有效时间、控制写入所有权、共享精力和退出；没有客户端秒数输入 |
| [FocusClient.java](../src/client/java/dev/wildcraft/client/focus/FocusClient.java) | 弓显示预测、原版帧级松弓、追加后处理与声音；设置仅客户端保存 |
| [EnvironmentTemperature.java](../src/main/java/dev/wildcraft/temperature/EnvironmentTemperature.java) / [TemperatureSampler.java](../src/main/java/dev/wildcraft/temperature/TemperatureSampler.java) | 服务端当地环境采样、低频本人同步、生命周期；不保存体温、不产生额外惩罚 |
| [TemperatureHud.java](../src/client/java/dev/wildcraft/client/temperature/TemperatureHud.java) | 帧级平滑、七档回差、心/精力下方读数、独立可关闭色调，林克时间优先 |
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
- `focus_view` 只同步本人 active/session/服务端弓时长，技能与控制租约不保存；`wildcraft-focus.json` 为本机表现格式 1，旧三类玩家保存格式不变。
- `temperature_view` 只有一个有界浮点目标，非持久且仅本人同步；重连/复活/切维度重新采样。`wildcraft-temperature.json` 格式 1 只保存本机边缘色调，不改旧 Focus 格式。
- 攀爬、滑翔、林克时间、输入与会话均瞬时；过期、死亡、维度和重连清理，不恢复动作、不免费补满精力。
- 复活处理排在 Fabric 附件转移后。跨维度测试必须等原版加载确认，不能为让测试伤害生效删除原版保护。
- 升级先用世界副本；真实用户数据迁移需要对应授权。保留旧读取；回退使用对应世界备份与旧包，而不是单独替换 JAR。

## 已完成探索、料理、能源与机械

当前技能合同见 [P3.2-SPEC](P3.2-SPEC.md)，开发默认 5 TPS / 每有效秒 10 精力 / 最多计入 250 ms 单步 / 最长 25 ms 等待轮询。弓首版，弩不触发；只允许未发布且恰有一名真实玩家的集成单人服务端。预先已经慢到 5 TPS 或更低时拒绝接管，避免把世界加速。

正式实现必须保留：

- `focus/ActivePlayClock` 单调时间，暂停/离线不计入，卡顿超出部分舍弃；不是完整墙钟补扣。
- `focus/TimeLease` 记录进入前速率和每次写入版本，外部同值/不同值控制不覆盖。入口拒绝冻结/步进/冲刺，不嵌套。
- `FocusPacketMixin` 只在等待下一世界刻时处理输入/费用，不额外 tick 玩家或世界；`IntegratedFocusMixin` 在暂停 tick 入口和 LAN 发布入口撤销。
- `FocusItemMixin` 只校准使用剩余时间并走一次原版 release；缓降不清零摔落距离。客户端新同步样本修正预测，避免卡顿后永久超前。
- GPU 原创后处理和独立有效开关；关闭画面保留技能、费用和 HUD。默认不启用轻闪/纹理/FOV；核心美术提供两份原创提示音。

P4A和P4B均已完成。独立料理锅、有效实际时间、三类效果与有限细雪辅助见P4B-SPEC/VERIFICATION。P5也已完成，见P5-SPEC/VERIFICATION。P6直接节点操作和P7八类部件均已验收，见P6-SPEC、P7-SPEC与P7-VERIFICATION。

R1 保持独立研究代码；实验开始时主动退出并阻止当前正式拉弓会话，防止两个控制器争夺同一次使用，历史 R1 全套仍回归。普通客户端测量在 [FocusNativeProbe](../src/gametest/java/dev/wildcraft/test/research/time/FocusNativeProbe.java)，`fabric.client.gametest` 必须为 null。静态 NoGravity 场景只测时钟、世界/实体速率、蓄力与原版射箭；真正下降、反复拉弓与落地伤害在独立生命周期场景测。

## 测试与证据标准

P3.2 本机新增有效时间 10 项检查、2 项 GameTest 和 1 个成品客户端测试类，P3.2 当时共 27 GameTest（26 项目 + 1 原版）、8 个客户端测试类；核心美术基线扩展为 28 GameTest、9 个客户端测试类，见 CORE-ART-VERIFICATION.md，以及 4 项普通客户端真实计时。P4A 将本机回归扩展为 32 GameTest、10 类成品客户端，并新增温度纯规则检查及专项客户端；详见 P4A-VERIFICATION。历史 P3.1 的独立双客户端与无测试模组服务端证据保留。精力/攀爬/滑翔/R0/R1 继续回归。普通 `test` 没有来源，不算测试通过。

```sh
./dev.sh runDatagen
./dev.sh build gameTestJar
# 先自行阅读、接受 Minecraft EULA，才使用下列显式参数
./dev.sh -PacceptMinecraftEula=true --no-configuration-cache runPackagedClientTest
```

本机测试协议不能替另一操作者接受 EULA。自动测试可能清理运行目录，只使用生成的专用世界。原始证据在 [development-assets](../development-assets/README.md)，旧报告的日期/CI 状态是历史事实，当前 CI 另看 GitHub Actions。

双客户端重现见 [P3.1 验收](P3.1-VERIFICATION.md)：先 `prepareP31Lab` 冻结 JAR，再启动 `runP31Server`、`runP31Actor`、`runP31Observer`。三个进程用不同 `WILDCRAFT_PROJECT_CACHE`；运行期间不得重建冻结 JAR。两个普通客户端日志中 `fabric.client.gametest=null`，自己的网络视图确认每阶段，不读取另一进程对象来冒称多人。

P3.2 真实计时重现：创建名为 R1Realtime 的可丢弃测试世界后运行 `runP32NativeClient`。任务在独立 P32Realtime 副本上运行，不读写原世界；固定 26.3 选项格式带 version 5023，避免缺版本引发无障碍初始引导。具体前置与限制见本阶段报告。

尚未验收：人工手感、长时间多人、高延迟/丢包、第三方装备/动画/物理模组、真实账号披风及多平台桌面。Linux CI 通过也不等于 Linux 图形游玩已通过。

## 资源、Git 与交付

原创源稿在 [art](../art/README.md)：伞图标 SVG、测试核心像素 JSON；运行贴图和伞几何随源码。模型/语言由数据生成维护，手写配方/贴图/标签保留来源。核心美术原件在 `art/approved-v1`，含 Blockbench / Blender / Aseprite；运行伞网格和四肢数据由 `art/tools` 烘焙，重生成不修改原件。新增公开的 `climb_visual` / `back_equipment_visual` 是非持久化外观附件，不含精力、库存槽位或保存签名；原格式 1 和客户端设置不变。八类有限机械已实现，超复杂机械物理暂缓。

保持一个 Gradle 项目、一份正式 Mod JAR。使用 `feature/<功能>`、`fix/<问题>`、`spike/<实验>`、`upgrade/mc-<版本>`；不要新增长期 develop 分支。每阶段更新规则、相关验收和实际包版本，不移历史标签、不给实验提前起正式版名。历史源码/JAR/证据在开发资产目录，诊断失败日志不等于最终结果。

生产依赖仍只有 Loader/Fabric API；新依赖先检查已有能力和官方维护/许可，然后按明确授权添加。项目保留所有权利，公开仓库不自动改变许可；模板与 Wrapper 见 NOTICE。绝不把第三方 Minecraft 源码、缓存、账号或个人世界放进交接。

## 当前授权、版本与下一任务

用户授权按推荐路线持续开发，当前先完成单人体验，不重复询问既有选择。本次明确授权上传GitHub、更新README/已采用资产/交接文档，2026-10-06再次明确“更新github”，范围为dev.18开发交接，不代表稳定发行或自动授权未来每次发布。新增生产依赖、真实数据迁移和破坏兼容仍需对应授权。任一有效Codex额度窗口剩余≤5%先保存停止，不自动消耗额度重置；重新读取工具，不使用历史数字。

下一步按[NEXT-DEVELOPMENT-PLAN](NEXT-DEVELOPMENT-PLAN.md)：人工单人试玩及定向平衡、B6风提示与底部机械外形、之后P11候选检查。不能将测试布置场景写成人工从零生存，不继续R2/P10.1，不凭理论重调成长或电池。

- 机械公共入口`mechanics/MachineEntity`、`MachineNodes`、`MechanicsContent`、`MachineBodyItem`、`MachineToggle`；客户端`MachineRenderer`/`MachinePresentation`。服务端重算眼睛命中，创造也真实转移一件；R请求只读取真实乘坐主体。主体格式1保存6个完整堆栈、所有者、开关及原版实体状态；跟踪视图只包含部件、电量、燃料、冷却与工作位。缺失目的区块不扣资源或推进冷却，主体仍为单碰撞盒。
- 制造入口`fabrication/FabricatorEntity`、`FabricatorBlock`、`FabricatorItem`、`FabricatorMenu`；客户端`FabricatorScreen`。格式1保存3槽、已经决定的完整结果与进度；未完成结果不公开。原版Container和BlockEntityData须同时携带，创造放置也转移一件，不能引入复制或重抽。
- Fuse正式系统在`fuse/`，保存和原子事务边界见[架构](ARCHITECTURE.md)、[P9](P9-SPEC.md)、[P9.1](P9.1-VERIFICATION.md)。原材料完整快照与有限次数保留；未知材料回退不等于任意第三方效果兼容。
- 当前正式JAR为[dev.18](../development-assets/wildcraft-0.1.0-dev.18+mc26.3.jar)，SHA-256：`daaa39c4395511671a8ae9d1dcb5f0eca0ea399fe2d9b0ce4ef1ff47f15a869c`。旧包、历史标签与研究资料不覆盖。

## 第二批美术后续入口

运行资源与原件分别在 src/main/resources、art/production-v2；导入工具 tools/import-second-art.py 需要 Pillow，普通构建无需美术编辑器。新增 ArtSnapshot 不保存库存或私有制造结果，既有数据格式不迁移。首次接手先看[当前资产台账](../art/ADOPTED-ASSETS.md)和SECOND-ART-VERIFICATION的准确范围；R2 候选依然未注册、未验证，不因美术整合而算已完成。

## 当前验证范围

[P10.2验收](P10.2-VERIFICATION.md)：83项服务端、21类成品客户端完整回归；另有实际GUI1/2/3中英专项和无测试Mod服务端保存退出。完整日志、XML和148张截图见[verification/p102](../development-assets/verification/p102/README.md)。这些是dev.17 macOS实测，不是本次文档更新重新运行的测试。

人工长期单人生存、从零原料采集、续航手感、第三方模型和多平台图形仍未完成；Linux CI结果单独看[Actions](https://github.com/ibka512/Wildcraft/actions)。原稿审稿与实际游戏验证分别记录。第二批29项采用并接入、30份版本保留，当前F05-10 v02，运行与原件关联见[台账JSON](../art/ADOPTED-ASSETS.json)。

## P10 接手入口

公共 `weather/WindRules` 按世界种子、维度和世界刻计算有界水平风；`WindSystem`只读已加载列及原版降水，不保存风数据，`network/WindView`只同步本人临时视图。`Gliding.movement`叠加风和雨雪下降；`MachineEntity.tick`仅让离地翼受风，风先于供电稳定器，有限资源和原速度/目的区块检查保留。`client/weather/WindHud`在料理下方显示相对视角箭头/短文案，是B6之前的临时反馈。

复现：先`./dev.sh --no-configuration-cache runDatagen`，再`build`；本机EULA已由所有者接受，当前机器可以用`-PacceptMinecraftEula=true runGameTest runWindClientTest`；其他操作者须自行接受。纯规则`verifyWindRules`纳入`check`。完整客户端保留既有历史回归，没有新增多人专项；本阶段不代表R2或多人林克时间完成。

P10.2已跑原生循环并记录费用，侧轮显示已修正；下一批仍需人工生存和自然地形记录，再定向调参。底部穿插、腰侧与第三方模型继续检查。雷击能源未定稿，不作为已实现功能；不升级依赖或改持久格式。

## P10.2/dev.17 接手要点

- 14条原料配方都走原版匹配器验证，8条新增advancement通过真实inventory_changed解锁，不用award命令。弹簧旧图案与指南针相同，已改为`II ` / ` R ` / ` II`，仍4铁＋1红石；旧图案仍应产出指南针。
- 料理锅标题右侧蔬/暖/凉/力（V/W/C/S）悬停说明，`CookingGuide`读公共`CookingRecipes.ALL`的7配方，没有独立第二份规则；语言由数据生成维护。服务端菜单/食材判定/保存均不变。
- `MachineRenderer.partPoint`仅将侧轮外观下移0.075，预览和安装同用，地面不做装饰roll。`MachineNodes`、碰撞盒、动力、安装命中和存档未改。底部部件仍可能穿地，见[美术修正依据](../art/production-docs/P10.2-MECHANICAL-ART-REPAIR.md)。
- `SurvivalClientTest`只提供原版原料与场景。合成/料理/装备/攀爬/滑翔/射箭/Fuse/充电/安装/驾驶/回收/制造/重开均用真实游戏路径；站点间传送、经验20与停止后的测试场景复位不代表自然探索。
- 完整回归和GUI专项分开留证，GUI必须核对实际`Window.getGuiScale()`，不能把小窗将选项3自动夹为2算成实际3。后者用1280×960窗口支持真实1/2/3。
- [数值基线](SINGLEPLAYER-BALANCE.md)/[逐项成本](SINGLEPLAYER-RECIPE-COSTS.csv)记录当前参数；本轮没有凭理论改成长或电池。下一批先做[人工试玩单](SINGLEPLAYER-PLAYTEST.md)或用户明确的新任务。B6可制作，见[合同](../art/production-docs/B6-WIND-ART-BRIEF.md)，正式资产仍未制作/采用。
- 不新增多人工作。本次dev.17为公开开发交接版，不能写成P11稳定候选或人工平衡已完成。

## 公共下载与历史证据

[ASSET-DOWNLOADS](ASSET-DOWNLOADS.md)列出当前工程与原始第二批ZIP，第一批完整交付继续从dev.7历史Release下载。根README、AGENTS和本文件是当前接手入口；旧设计、原稿和阶段报告保留历史身份。历史manifest中源码/文档哈希按对应游戏标签检验，不覆盖旧清单使它匹配新文档。当前全部公开文件快照见[dev.18清单](../development-assets/PUBLIC-ASSET-MANIFEST-dev18.json)，以交接标签为准。

仓库保留所有权利，许可与NOTICE不变；不上传个人世界、账号、游戏/反编译源码、依赖缓存、协议接受文件、AppleDouble或编辑器运行配置。

## dev.18 品牌更新与当前验收

A14 当前 v02：精细版用于 README，简单版用于 Mod 128px 与设置页原生 20px。原始两张 PNG、清理派生、导出脚本和来源清单保留；总计仍 43 个采用编号。macOS 重新通过数据生成、构建、83 项服务端检查与一类核心美术客户端专项，证据见 [品牌验证](BRAND-ICONS-VERIFICATION.md)。没有重跑完整 21 类客户端或长期人工生存。该报告记录发布前本机状态；当前公开状态以本文件和下载说明为准。
