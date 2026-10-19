# Wildcraft 开发接手说明

更新2026-10-19。公开代码版本0.1.0-dev.76-public，来源本机停线1a14a9c5/正式dev76 fc24678e；独立基于公开main c0bf3f7c筛选提交，没有合并私有分支/标签。公开提交SHA以GitHub main与PUBLIC-VALIDATION-dev76.json为准。

## 当前状态与公开区别

本机dev63补普通剑击第一人称，dev64–67修复暂停采样并限定验证，dev68–72补披风腰侧，dev73–74补原生鞘翅，dev75–76补空主手副盾DRAW。八类主装备是剑、斧、三叉戟、重锤、七材质原生长矛、主盾、弓、弩；站立静止、其他盔甲空等资格仍有范围限制。

公开包含运行时代码、测试与转换工具，但不带本机每体型401个第三人称/4个第一人称剪辑、七份JSON、原源或含数据JAR。CombatRig与FirstPersonSword资源缺失时回到既有原生路径。公开包后缀-public，不与本机私有包混用名称/哈希/视觉结论。副盾仅真实main Bow正常损坏后同reference恢复到空主手副盾，不能以库存交换、F键或GUI迟到重播制造事件。off STOW/占用main/复杂盔甲/其他正式FP仍未开放。

## 已确认行为

1. Java/Fabric/macOS；MC26.3、JDK25、Loader0.19.5、API0.161.0+26.3、Loom1.18.2、Gradle9.7.1。
2. 历史最高经验绑定精力上限；花经验不降峰值。左上红心、其下精力。
3. G按住攀爬；伞在独立装备位，空中跳跃键开收伞；取用物品收伞，手部隐藏不删除物品。
4. 真攻击、举盾、拉弓后登记背负；同一库存引用，不新增库存。
5. 料理独立锅与有效实际时间；暂停/离线不计，死亡清空。温度以反馈为主。
6. 有界材料Fuse、固定红石能源/有限电池、节点安装/停机回收、制造结果保存不重抽、风天气互动均保留。
7. 锁定、独立举盾、轻强击、侧后跳、完美格挡/闪避/短林克时间和反击按已定判定结算。不同近战种类分开处理，native spear kinetic和mace smash不得全局套剑算法。
8. 不做究极手、倒流、Boss、完整神庙、大新维度、复杂剧情、大量新资源、小事件、环境谜题或机械蓝图；超复杂机械物理暂缓。

## 代码入口

| 目录 | 职责 |
| --- | --- |
| src/main/java/dev/wildcraft/player、traversal | 峰值/精力、伞装备、攀爬与滑翔权威逻辑 |
| combat、focus、network、equipment | 战斗、时间租约、网络校验、真实背负归属 |
| cooking、temperature、fuse | 料理及时间、温度反馈、材料原数据与次数/原生结算 |
| energy、mechanics、fabrication、weather | 固定供电/有限电池、机械安装与乘坐、制造与风天气 |
| src/client | 输入、菜单、HUD、原生渲染及可选源动画适配 |
| src/gametest、src/rulesTest | 独立验证及隔离研究，不自动加入正式功能 |
| tools、art | 来源验证/转换工具和可公开原件/运行索引 |

公共玩法放main，不能引用客户端类。持久化与瞬时动作状态分开；真实死亡、维度/保存重开按各模块原事件合同处理，不直接copy/mark绕过。

## 构建与复现

使用锁定JDK25与Wrapper，顺序执行：

```sh
python3 tools/verify-public-snapshot.py
python3 tools/verify-adopted-assets.py
./gradlew runDatagen
git diff --exit-code -- src/main/generated
./gradlew build gameTestJar
python3 tools/verify-public-snapshot.py --artifacts
```

macOS可用dev.sh；个人配置与缓存路径无需复制原开发机。build包含125项服务端测试，test NO-SOURCE不算通过。独立runGameTest需操作者自行接受EULA后显式传acceptMinecraftEula。图形检查可运行runCoreArtClientTest，以及runFirstPersonSwordClientTest -PfirstPersonSwordFallback=true检查缺资源回退。普通实例只安装主JAR与已有Fabric API，不能安装测试/源码JAR。

需要私有动画的Rig专项无法在纯公开克隆复现，属于缺必要资源而非已通过。先取得具体素材与许可，再按当前转换工具和规格配置；不能用空JSON、虚拟姿态或复制旧图冒充。runC7NativeRenderBaseline默认指向本机历史dev62冻结包，不适合裸公开克隆直接运行；可通过c7PerformanceMainJar指定自己构建的主JAR、c7PerformanceFixture提供自动生成的可弃世界、c7PerformanceRunDir指定全新隔离目录，严禁传个人世界。普通客户端探针没有fabric.client.gametest，提供原料不代表人工生存。

## 验证事实与下一步

[当前公开验证](PUBLIC-SNAPSHOT-2026-10-19.md)与[机器回执](PUBLIC-VALIDATION-dev76.json)记录本次真正执行内容；本机530图/125与历史full48分开。普通客户端短测、120/10FPS有限驾驶和逐帧窗口检查见C7-DEV76两份验收；其中新guard低FPS未跑，长期性能/自然地形/人工平衡未完成。

下一开发从C7驾驶窗口低FPS验证、受控地形/转向与单人生存试玩开始；此公开同步任务本身不授权新增玩法、生产依赖、数据迁移或未来公开发布。先读实际任务及AGENTS，不把旧阶段全路线当本轮执行指令。原生伤害/库存/物理/费用/use/block/wear/Fuse和技能时长保留，不为动画改结算；不擅自开放复杂盔甲/副盾STOW或采用待审13FP/另6项。

## 美术与许可

可公开用途台账仍见art/ADOPTED-ASSETS；[最新目录状态](../art/RUNTIME-STATUS-2026-10-19.md)和JSON登记96目录条目、90有限运行用途与6参考条目。它们不是96模型，也不保证全部剪辑启用。项目原LICENSE/NOTICE保持，没有擅自改MIT/CC0；公开源码不授权受限动画再分发。

公开更新继续从干净公开父提交选择性同步，不推送本机整条历史/标签。不要提交真实世界、凭据、缓存、游戏文件、AppleDouble或含私有数据产物。历史规格中的本机包/原源/证据路径只用于解释旧结果，不是公开下载，不为补链接上传受限文件。
