# Wildcraft 开发接手说明

更新：2026-10-15。当前公开代码版本 **0.1.0-dev.62-public**，从本机 `c554ed95bb8c0c137ab5f07bf710af9fb8861566` 选择可公开源码、资源和技术文档，基于原公开main `81e1aa0b07ba426e35d664b23720fa6a16b91300` 提交。两个编号分别描述本机来源与公开父提交，不能据此向公开仓库索取整条私有分支。

## 先理解版本区别

本机dev62使用337个许可受限剪辑/335选取，曾执行完整动画验证；这些资源及含数据安装包只本机。公开源码保留相关运行时代码，但不带这些动画数据，CombatRig在找不到资源时回退到既有原生表现。公开构建标识追加-public，不能冒充同名私有JAR的字节或画面结果。

本次公开更新包含C1–C6战斗/输入/握持代码、F07风天气显示、四类底部资源修订及C6AN/C7测试代码。完整C6、正式第一人称源、复杂装备蒙皮、人工长期生存与平衡、长期性能仍未完成，多人暂缓。

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

```sh
./gradlew runDatagen
./gradlew build gameTestJar
python3 tools/verify-adopted-assets.py
python3 tools/verify-public-snapshot.py --artifacts
```

macOS可改用./dev.sh，或设置JAVA_HOME/WILDCRAFT_JAVA_HOME指向JDK25。不要复制原开发机磁盘映像或硬编码其缓存目录。IntelliJ按Gradle项目导入；无新生产依赖，无数据迁移。

build执行现有规则检查；独立服务端runGameTest需操作者接受Minecraft EULA后显式提供-PacceptMinecraftEula=true。客户端需可用桌面。C6源动画GPU专项、C7真实攀爬/滑翔等会依赖私有源或本机正式JAR，公开缺素材不能声称复现通过；可以执行不依赖私有资源的原生回退与服务端行为检查。

C7普通客户端runC7NativeRenderBaseline还要求显式提供已生成的可丢弃测试世界和本机冻结JAR，禁止使用真实存档。它没有fabric.client.gametest标志，专用metadata/Mixin不启旧研究时钟；与加速客户端GameTest的帧率统计不同。

## 已执行的历史验证与当前未完成项

| 范围 | 本机已执行事实 | 不代表什么 |
| --- | --- | --- |
| dev62/C6AM | 125服务端；完整47类47m29s/1893PNG；21专项；datagen/build/资产检查通过 | 不是全部动画、正式FP或公开资源版重新图形验收 |
| C6AN | 实际占手/原生回退64case/128图；完整48类51m6s/2021图 | 不是把盔甲/鞘翅重新适配到源骨架 |
| C7组合 | 固定源码三类59s/22图：保存重开、死亡、XP/伞/料理/Fuse/背负、动作取消、两电池分离、回收和制造领取 | 不是从零采集、人工手感或长期性能 |
| C7 native基线 | 普通客户端82s；0/4/16停机空主体各15s，约20TPS，实际像素2560×1564/render16 | 16主体末FPS30尚未排除闲置限帧，不能定为性能回归或验收通过 |

本次公开快照的独立执行结果只登记在PUBLIC-SNAPSHOT-2026-10-15.md，不把上表历史数字重新贴成此次CI结果。

下一步先读取C7-NATIVE-RENDER-BASELINE.md，复核隔离客户端实际renderDistance、inactivityFpsLimit及ThrottleReason，保留原始基线后做最小对照。之后按SINGLEPLAYER-PLAYTEST记录正常资源获取、自然地形、战斗/续航手感和长期性能，再定向修复与平衡。不要无变化重复full48，不凭理论扩大电池或改变成长。

## 资产、许可与历史资料

可公开45个用途ID、四类底部变体、30份第二批交付版本；verify-adopted-assets核对2107原件记录/57运行副本。原台账和交付清单保留原始哈希，不把待审第一人称13组或另6项自动采用。

本机受限源、派生动画、截图/完整验证包、含数据JAR及新私有Git历史未上传；旧dev18及更早公开下载仍有效。阶段报告中本机相对链接或绝对路径是历史证据位置，不保证公开克隆可取得。PUBLIC-ASSET-SNAPSHOT.json说明当前公开范围；LICENSE/NOTICE保持现有所有权利保留条款。

用户的新任务优先于历史说明。当前公开更新不授权以后新增依赖、迁移真实数据或公开发行受限素材；交接文档本身也不授权自动完成整张路线。
