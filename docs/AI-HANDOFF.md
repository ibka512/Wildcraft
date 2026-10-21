# Wildcraft 开发接手说明

更新2026-10-21。公开源码版本0.1.0-dev.87-public，来源本机b0e0a07a（候选实现10c655d6）；独立基于公开main 48847f62筛选提交，不推送本机历史/标签。本机正式仍dev86/ac3c95c9，候选87未完成最终构建/125与正式归档。公开版本单独验证，不能因此改变本机状态。

## 当前进度

dev77–82补电池图集、重复居中、浮力不足和受阻提示、火箭80世界刻/弹簧40刻反馈及待机文案；dev83修V融合真实快捷槽同步，84修开伞后迟到DAMAGE同步误收伞，85修原生临时触地标记残留导致过早收伞；86接入四项机械底部本机美术。87候选补第一人称重剑和斧真实皮肤/原生握点及支撑，按原剑12/斧20刻准备、原恢复期采样，不改变12/18精力和原生一次hit/wear/Fuse。

本机86九个不同客户端类/241PNG及125、datagen/build通过；候选87八个不同客户端类/288有效PNG，最终datagen/build125尚未执行。失败七类任务和后来观察器修复分开记录，详见FP-HEAVY-AXE-PROGRESS。源资格、实际玩法、人工手感不可互代。

## 公开资源与复现差异

公开保留45用途ID、4底部变体的既有可公开资产与全部最新相关源码。玩家源/派生动画、四项新机械底部源/9份派生几何均未公开。CombatRig、FirstPersonSword和FirstPersonHeavy缺资源时回到已有表现；公开专用BottomArt.name仅在部件与主体凹舱资源都存在时启用对应底部变体，否则沿旧主体/部件。该防崩回退是公开副本的唯一生产Java适配，本机原工程保持。不得把公开构建或截图当作本机源动画验收。

要复现私有画面，先取得各资源与使用许可：旧动画转换入口在tools/import-player-combat-art.py和tools/import-first-person-sword.py；候选新动作见tools/import-first-person-heavy-axe.py（标准库），机械见tools/import-bottom-additions.py。两份本机采用补充记录在art/reviews/2026-10-21，仅含范围/路径/哈希，不含源数据或新分发许可。其余11组FP和2反手弓未采用。不能用空JSON、参考装备或程序姿态冒称正式资产。公开隐私检查故意拒绝这些私有目录；需要本机复现时使用隔离私有工作副本。

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

锁定Minecraft26.3、JDK25、Loader0.19.5、API0.161.0+26.3、Loom1.18.2、Gradle9.7.1。按以下顺序，数据生成与build分开执行：

```sh
python3 tools/verify-public-snapshot.py
python3 tools/verify-adopted-assets.py
./gradlew runDatagen
git diff --exit-code -- src/main/generated
./gradlew build gameTestJar
python3 tools/verify-public-snapshot.py --artifacts
```

macOS可用dev.sh，设置自己的JDK/APFS缓存即可，不必复制原开发机环境。build执行125服务端检查；test NO-SOURCE不是测试通过。需要EULA的操作者自行接受后显式传-PacceptMinecraftEula=true，旧操作者授权不适用于其他人。

公开图形专项可运行runCoreArtClientTest及runFirstPersonSwordClientTest -PfirstPersonSwordFallback=true。新增公开专用回退专项同时检查无heavy/axe动画时仍有原生持物臂，以及四项缺失底部资源下的真实节点预览、安装、回收。其他需要私有源的Rig专项不能在纯公开克隆直接声称通过。

普通客户端探针runC7NativeRenderBaseline/runC7NativeIndustryBaseline等默认可能指向原开发机的冻结JAR/自动生成世界。使用前读build.gradle和相应规格，通过c7PerformanceMainJar等参数明确传自己构建的包、全新隔离目录与可弃测试世界；严禁传真实存档。脚本提供原料不代表人工从零生存。

## 接续与验收边界

[公开执行记录](PUBLIC-SNAPSHOT-2026-10-21.md)与[机器回执](PUBLIC-VALIDATION-dev87.json)只记录本次实际执行；历史本机报告开头注明身份。旧前台六场性能资格已因锁屏SDL反例撤回，没有解锁正例，不得继续引用为前台/长期性能通过。人工生存表仍空，不填写虚构体验。

下一开发先补候选87最终datagen/build125/源码与包核对，再正式本机归档；稳定客户端结果无变化不重跑。更多材质/袖子/FOV、复杂中断、11FP、2反手弓、副盾STOW/复杂盔甲和C6整体、自然生存/长期性能未完。当前自动任务暂停，本次GitHub同步不自行恢复开发或调度。

公开更新使用干净公开父提交选择性同步，不能推送私有分支历史或标签。保留原生物理/库存/伤害/费用/计时/use/block/wear/Fuse；新生产依赖、真实数据迁移、发布等仍需要对应明确授权。LICENSE/NOTICE保持原有权利边界，公开源码不自动许可私有美术再分发。
