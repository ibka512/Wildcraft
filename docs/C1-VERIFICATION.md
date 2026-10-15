# C0/C1 本机验收：统一输入基础 / dev.19

验收日期：2026-10-06。基线为公开dev.18；本机分支`feature/unified-input`，游戏版本`0.1.0-dev.19`。本批完成[C0技术入口](C0-FINDINGS.md)与[C1统一输入](C1-SPEC.md)，后续完整战斗/动画路线见[开发路线](COMBAT-INPUT-ROADMAP.md)。尚未上传GitHub，公开Release保持dev.18。

## 已完成的行为

统一Action输入采样接入攀爬、滑翔伞、Fuse、机械开关和画面设置。原生改绑标识及G、空中跳跃、V/Shift+V、R、F8保持。原版攻击、物品使用和快捷栏的点击队列仍由原版执行；没有改攻击伤害、存档、费用或配方。

帧间按下/松开边沿保留到下一次刻交付，消费者读取不可变快照。打开界面、失焦、暂停、玩家替换或断线清除技能输入；界面关闭后，持续按住的技能需先松开再按，避免误触或重放。滑翔中完整短按取用动作也会收伞，库存仍在原格。

C0仅验证短按/保持/取消、面敌移动参考系、原生模型部位组合、可选Controlify绑定及真实慢世界采样。实验只在测试模组；不把这些样本算作正式新战斗或新美术。

## 实际执行结果

| 检查 | 最终结果与证据 |
| --- | --- |
| 资源生成 | `runDatagen`成功，8秒；[日志](../development-assets/verification/input/datagen.log) |
| 构建及规则 | `build`成功；新输入规则覆盖完整短按、1000次重复边沿、GUI/失焦隔离、原生队列、修饰键和快照；既有精力、时钟、温度、风规则通过；[构建/客户端日志](../development-assets/verification/input/client-build.log) |
| 服务端GameTest | 83项required通过，0失败；[XML](../development-assets/verification/input/server-tests.xml)。此计数包含原有测试，不新增多人验收 |
| 无控制器依赖的成品客户端 | 5类通过：InputClientTest、CombatFoundationClientProbe、SurvivalClientTest、FocusClientSmokeTest、CoreArtClientSmokeTest；最终构建＋测试用时1分46秒 |
| 实际输入与旧玩法 | 改绑→融合→GUI丢弃命令/保持隔离→拆分→攀爬→开伞→短按取用收伞→重新开收伞→设置；单人生存原生制作/取得/料理/驾驶/回收/制造与磁盘重开通过 |
| 可选控制器侧载 | Controlify3.5.3+mc26.3和YACL3.9.7+26.3-fabric下，同5类通过，1分39秒；四个Wildcraft键位均有实际绑定API关联；[日志](../development-assets/verification/input/controller-client.log) |
| 普通客户端真实调度 | 未启用`fabric.client.gametest`，世界5TPS约2.5秒中298渲染帧、13世界刻，保留8个40ms完整短按，结束恢复20TPS；13秒任务成功；[日志](../development-assets/verification/input/native-frame.log) |
| 美术/正式包 | 43项采用资产、30份来源版本校验通过；正式JAR版本19，未混入测试代码、Controlify/YACL或研究快捷键。18张[真实截图](../development-assets/verification/input/screenshots)归档 |

这是本次实际执行的5类专项和相关回归，不是dev.17的21类全量客户端复跑。旧多人及其他历史记录保持原状态。离线测试身份的Realms认证警告不代表账号登录验证，也不改变实际测试成功/失败判定。

## 安装包与源码

- [dev.19正式JAR](../development-assets/wildcraft-0.1.0-dev.19+mc26.3.jar)
- [生产main/client源码JAR](../development-assets/wildcraft-0.1.0-dev.19-sources.jar)：不含完整测试/美术交接，完整当前阶段另见[源码快照](../development-assets/Wildcraft-C1-source.zip)
- [本批manifest](../development-assets/Wildcraft-C1-manifest.json)及[SHA-256校验](../development-assets/Wildcraft-C1-SHA256SUMS.txt)，在工程根目录核验；源码快照包含当前代码、测试、可编辑美术及运行导出、文档、构建入口与本批证据；排除重复审稿ZIP/预览、历史递归包、缓存和世界，完整审稿与历史从仓库读取。SHA256SUMS以当前文件原始字节核验，ZIP内采用Git规范化源码，Windows脚本的换行差异在manifest单列archive_sha256

正式包SHA-256：`5529232da59b6929c6011d85fff5cbfd5745849dd7d5a54dd3be018ab30dab40`。

保持Minecraft26.3 / JDK25 / Loader0.19.5 / Fabric API0.161.0+26.3 / Loom1.18.2 / Gradle9.7.1。正式游玩实例只放正式JAR和既有Fabric API，不安装测试JAR；Controlify/YACL仅本机研究，没有新增生产依赖。暂态输入不持久化，不需要数据迁移；历史包与标签不覆盖。

## 可重现步骤

先选择JDK25并由操作者自行接受Minecraft EULA，再依次运行：

```sh
./dev.sh runDatagen
./dev.sh -PacceptMinecraftEula=true --no-configuration-cache build runInputClientTest
./dev.sh -PacceptMinecraftEula=true --no-configuration-cache runInputNativeProbe
```

Controlify实验额外需要按[来源记录](../development-assets/verification/input/controller-source.json)自行准备两个可选研究JAR到所选缓存根的`labs/c0-controller`，再运行`runControllerInputProbe`。不依赖个人世界，原生调度任务只复制本批自动测试世界。首次旧fixture路径的失败日志保存在[diagnostics](../development-assets/verification/input/diagnostics/native-fixture-missing.log)，不是通过证据。

## 未完成及下一批

没有实际手柄硬件、摇杆/震动、跨平台图形或人工战斗手感验收。正式锁定、独立DEFEND、闪避、蓄力攻击、怪物前摇、完美判定、近战减速反击与完整新动作尚未实施。现有空中弓和输入解耦不能替代这些实现。

下一批C2按既定路线推进目标选择/丢失宽限、面敌移动与自由瞄准、独立举盾/普通格挡及分层基础姿态。B6与机械底部美术、人工长期生存仍待完成；多人暂缓。公开发布须有对应授权，本批只作本机开发交接。
