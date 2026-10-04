# 开发环境、运行与操作手册

Minecraft Java Edition 的 Fabric Mod。当前本机开发版本为 **P4A 环境温度**，版本 `0.1.0-dev.8`，包含核心美术 v1；公开 main/Release 仍为 dev.7，本次尚未上传。

当前可用：七档环境温度、当地雨雪/水/热源修正、平滑读数及独立可关闭边缘色调；空中拉弓自动单人慢动作、有效实际时间扣精力、原版一次射箭与保留摔落、F8 画面设置；按实际使用登记的近战/盾/弓背负、左上角红心与其下方精力显示、独立滑翔伞装备位、合成与装备保存、空中跳跃键开收伞、双手持伞与取用物品自动收伞，以及按键攀爬、历史最高等级、精力保存与恢复、中英显示和管理员验证命令。测试核心 `wildcraft:test_core` 继续用于物品与资源回归。

完整开发顺序见 [开发路线](ROADMAP.md)，三项新设计的范围、衔接和验收见 [开发规划 v2](DEVELOPMENT-PLAN-V2.md)，系统边界见 [架构计划](ARCHITECTURE.md)，精力规则见 [P1 规则](P1-RULES.md)，研究结论见 [R0 结论](R0-FINDINGS.md)。攀爬操作见 [P2 规则](P2-RULES.md)，滑翔操作见 [P3 规则](P3-RULES.md)，背负与界面见 [P3.1 规则](P3.1-RULES.md)，本次验证见 [P3.1 验证记录](P3.1-VERIFICATION.md)。历史验证保留为 [P3](P3-VERIFICATION.md)、[P2](P2-VERIFICATION.md)、[P1](P1-VERIFICATION.md) 与 [P0](VERIFICATION.md)。

攀爬与滑翔已接入实际精力消耗；R0 机械与 Fuse 样例继续只在测试模组中运行。料理、温度、正式机械与 Fuse 按后续路线推进。

新版路线的 R1 单人时间边界实验和 P3.1 正式背负/界面均已完成本机验证，见 [R1 结论与证据](R1-FINDINGS.md) 与 [P3.1 规格](P3.1-SPEC.md)。林克时间已经接入正式模组，见 [规格](P3.2-SPEC.md)、[操作](P3.2-RULES.md) 和 [本机验收](P3.2-VERIFICATION.md)。

## 固定环境

| 项目 | 版本 |
| --- | --- |
| Minecraft | 26.3 |
| JDK | 25 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.161.0+26.3 |
| Fabric Loom | 1.18.2 |
| Gradle Wrapper | 9.7.1 |

本机使用 Homebrew OpenJDK 25 和 IntelliJ IDEA 2026.2.3。`dev.sh` 显式选择 JDK 25。其他机器可设置 `WILDCRAFT_JAVA_HOME` 指向自己的 JDK 25。

当前本机工程已迁入外置盘内的 APFS 磁盘映像，入口见 [本机开发说明](LOCAL-DEVELOPMENT.md)。一般将源码放在 ExFAT 外置盘时，macOS 的附属文件会干扰 Gradle 清理，并被 Minecraft 误识别为资源，因此 macOS 默认缓存位于 `~/Library/Caches/Wildcraft/`；可用 `WILDCRAFT_CACHE_HOME` 指向其他 APFS 位置，本机已使用盘内开发环境的 `cache/`。源码仍在项目目录，`build` 会把可安装的成品导出到项目的 `dist/`；缓存可以重新生成。

`dev.sh` 还会清理已确认是 AppleDouble 格式的 Wrapper 和 IDE 运行配置附属文件，避免 IntelliJ 误认多个配置。外置盘上首次导入前，先运行一次 `./dev.sh --version`。临时开发世界也位于运行目录，不把个人存档放到缓存中。

## 在 macOS 开发

在项目目录中运行：

```sh
./dev.sh --version
./dev.sh runDatagen
./dev.sh build
./dev.sh runClient
```

首次构建需要联网下载 Gradle、Minecraft 和 Fabric 依赖。之后可使用缓存。

`./gradlew` 是标准入口；直接使用它时，需要先让 `JAVA_HOME` 指向 JDK 25。Windows 使用 `gradlew.bat`。

资源生成输出在 `src/main/generated`。物品模型与语言文件由生成器维护，不手改生成文件。测试核心的原始像素图保存在 `art/source/test_core.json`；滑翔伞的源稿和模型贴图说明见 [art/README.md](../art/README.md)。

资源生成与构建按上面的顺序分两次执行，保证生成后的资源被打入成品包。

## 在 IntelliJ IDEA 打开

1. 选择 **Open**，打开这个项目目录。
2. 按 Gradle 项目导入。
3. 将 Project SDK 和 Gradle JVM 都设为 JDK 25。Homebrew 常见路径：`/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`；以实际安装为准。
4. 等待 Gradle 同步，再使用 Minecraft Client 运行配置，或在终端执行 `./dev.sh runClient`。

本机已完成 Gradle 导入，项目 SDK、语言级别和 Gradle JVM 均为 25。IDE 的 Gradle 用户目录可使用 `~/Library/Caches/Wildcraft/gradle`，与终端共用依赖缓存；其他电脑可使用自己的本机缓存路径。

首次启动 IntelliJ IDEA 可能显示 JetBrains 用户协议；这与 Minecraft EULA 是独立的协议，需要用户自行决定是否接受。

IntelliJ IDEA 统一版的基础 Java 功能可免费使用；本项目不需要 Ultimate 订阅。

## 在游戏中验证

创建一份独立测试世界。面向墙体、靠近后按住 **G**，用 **W/S** 上下移动，**A/D** 沿墙横移，松开 G 放手。停留和移动都消耗精力，耗尽或受击会下落；再次抓墙需要先松开键。开放墙顶会在碰撞允许时辅助翻上去。

默认 G 可在按键设置的 Wildcraft 分类中修改。普通生存玩家即可攀爬，不需要管理员权限；梯子和藤蔓仍按原版操作。核心美术 v1 已有专用攀爬姿态，尚无雨天打滑，完整边界见 [P2 规则](P2-RULES.md)。

用 3 个皮革和 4 个木棍合成滑翔伞，或在测试世界执行 `/give @s wildcraft:paraglider`。打开物品栏，把伞放进人物预览右上方的独立伞槽；Shift 快捷移动也可装备。放在背包中不算装备。

装备后，在空中重新按跳跃键开伞，再按一次收伞。默认空格会跟随原版跳跃键改绑。视角控制方向，W/S 调整前进速度，A/D 侧移。开伞时剑、盾等物品暂时收起并保留原来的堆栈；切换快捷栏、攻击或使用物品会收伞。精力耗尽、受击、落地、卸下装备也会收伞，详见 [P3 规则](P3-RULES.md)。

使用剑/斧实际击中目标、举盾或拉弓/装弩后，该类别登记最近使用的装备。只切换快捷栏不会改记录；换成空手或其他物品后可在第三人称看到背负，其他玩家也能看到。掉落、放入箱子或损坏会清除对应记录。拉弓时盾暂时显示在背上，滑翔时双手仍抓伞，真实物品留在原库存。首版显示披风时隐藏背负，鞘翅隐藏冲突的盾/弓；短落定过渡已接入，完整腰侧布局留到后续。

未开放 LAN 的单人生存世界，空中按住使用键实际拉弓会自动进入林克时间。世界 5 TPS，拉弓保持正常实际时间曲线，精力每有效秒消耗 10。松弓、落地、耗尽和资格丢失退出；暂停和开放 LAN 恢复进入前速率。有限缓降仍受累计摔落伤害；弩不触发。F8 打开独立表现设置，可关闭去饱和/暗角等，不会关闭技能或精力收费。

启用命令后，可继续验证精力历史：

```mcfunction
/experience set @s 30 levels
/experience set @s 5 levels
/wildcraft stamina status
/wildcraft stamina fill @s
/wildcraft stamina consume @s 80
```

历史峰值为 30，上限为 160。消费 80 后，左上角红心下方显示精力数值与短条；落地停止消耗 1 秒后，每秒恢复 20，补满后保留约 2 秒再隐藏。红心沿用原版状态图案，额外红心向下排成多行；原版经验条仍在底部显示当前 5 级。数值是开发参数，尚未进行完整游玩平衡。

可用命令：`/wildcraft stamina status [玩家]`、`/wildcraft stamina consume <玩家> <数值>`、`/wildcraft stamina fill <玩家>`。要求游戏管理员权限；控制台使用明确的玩家目标，例如 `@p`。普通客户端不能通过自定义消息改变精力。

首次安装时只能从当前等级开始记录历史；消费经验不会降低峰值。死亡复活保留峰值并补满精力；重登、切维度和离线不会免费补满。规则和保存格式详见 [P1 规则](P1-RULES.md)。

测试原有物品：

```mcfunction
/give @s wildcraft:test_core
```

测试核心也加入了创造模式的原材料分类，可在创造物品搜索中查找其名称。

检查背包图标、手持和丢到地上的显示；退出并重新进入世界后，物品应仍在背包中。

## 安装构建结果

构建出的游戏 Mod 在 `dist/wildcraft-0.1.0-dev.8.jar`。构建缓存中的 `-sources` 文件用于查看源代码，`-gametest` 文件用于自动验证，均不安装到日常游玩实例。

在独立的 Minecraft 26.3 实例中安装 Fabric Loader 0.19.5，将 Wildcraft 游戏 JAR 与 Fabric API 0.161.0+26.3 放进该实例的 `mods` 目录。客户端和服务端都需要安装。

请使用专门的测试实例和世界。开发客户端的运行目录、日志、缓存与构建输出不进入 Git。服务端需要的公共逻辑与客户端代码包含在同一份游戏 JAR 中，由 Fabric 按环境选择入口。

## 自动运行验证

`build` 包含无额外测试依赖的精力数值检查：13 个示例和 1000 组边界/单调性检查，另有 10 项有效时间检查，并自动运行无图形服务端 GameTest。普通 `test` 任务仍无测试来源，不把它算作验证通过。

游戏测试覆盖精力编码、经验峰值、两名嵌入式连接玩家隔离、真实玩家保存和复活，以及 R0 部件回收、碰撞、乘坐和材料保存。攀爬测试覆盖碰撞形状、墙角与墙顶、消耗、重复输入和过期输入。客户端测试额外覆盖实际按键、升降与横移、开放墙顶、内外转角、天花板、实际伤害、耗尽，以及关闭飞行许可的回环 TCP 攀爬；P0/P1/R0 的显示、存档、复活和物品检查继续回归。移动手感和多人长期运行仍需后续游玩验证。

测试代码在 `src/gametest` 和 `src/rulesTest`，不进入游戏 Mod。

P3 检查还覆盖专用槽装卸、满背包、原版配方、未装备拒绝、手中物品收起、取用物品收伞、按键开收伞、缓降与转向、攀爬衔接、落地、死亡掉落及保留物品、跨维度、世界重开和禁止飞行的本机 TCP 重连。装备保存新增独立格式 1，精力数据格式保持不变；降级使用对应世界备份。

P3.1 新增 5 项服务端测试，当前共 25 项 GameTest（24 项项目测试 + 1 项原版测试）。成品客户端执行 7 个测试类，覆盖同款装备、手背互斥、存档、潜行/游泳、红心多行、界面缩放和原版骑乘条；另用两个独立图形客户端连接独立服务端，验证观察者看到装备变化、滑翔收纳、丢弃、放箱子、跨维度与断线重连。详见 [P3.1 验证](P3.1-VERIFICATION.md)。

先阅读 [Minecraft EULA](https://www.minecraft.net/en-us/eula)。同意协议后，可以明确添加以下参数，允许工具在临时测试目录写入 `eula=true`：

```sh
./dev.sh -PacceptMinecraftEula=true runClientGameTest
./dev.sh -PacceptMinecraftEula=true runPackagedClientTest
./dev.sh -PacceptMinecraftEula=true runGameTest
```

第一个入口使用开发环境；第二个入口使用打包后的游戏 JAR 和独立测试 JAR；第三个运行无图形的服务端游戏测试并生成 XML 结果。入口自动退出，失败返回非零状态。测试使用回环地址和离线测试身份，不使用你的 Minecraft 账号。运行目录是临时数据，测试可能清理它；不要把个人世界放进去。R0 测试世界包含研究注册项，不用于普通游玩实例。

未添加参数时不会自动写入 EULA 接受文件。构建中的原版 GameTest 专用服务端不走普通服务端的 EULA 启动检查；客户端内的普通测试服务端和独立服务端仍需接受协议。GitHub 构建配置检查资源生成、编译、数值与无图形 GameTest，不运行桌面客户端；首次公开远程构建已于 2026-10-03 通过，见 [公开交接记录](PUBLICATION.md)；历史验收中的远程状态不作追溯改写。

## 独立服务端

开发服务端运行入口是 `./dev.sh runServer`，用成品 JAR 验证的入口是 `./dev.sh runPackagedServer`。Minecraft 服务端首次运行要求阅读并同意 Minecraft EULA；仅在同意后，按其提示修改对应本地测试目录中的 `eula.txt`。控制台输入 `stop` 会保存并关闭服务端。

macOS 运行目录位于 `~/Library/Caches/Wildcraft/builds/<项目路径标识>/run/`，其中 `client`、`server`、`clientGameTest` 和 `packaged-server` 相互隔离。改变项目位置后会使用新的缓存与测试目录。

本机验证服务端的 `server.properties` 使用 `server-ip=127.0.0.1`、`online-mode=false`，仅用于本机连接；这些设置和 EULA 文件都不随源码分发。首次运行即使因 EULA 退出，Gradle 也可能显示成功；应以服务端日志中的 `Done ... For help`、实际响应和正常保存为准。

本机离线开发测试使用专门的测试目录与回环地址。面向实际玩家的服务端配置不沿用离线测试配置。

## 项目结构

- `src/main/java`：公共注册和服务端可加载的代码。
- `src/client/java`：客户端入口和资源生成代码。
- `src/main/resources`：Mod 元数据、手写贴图。
- `src/main/generated`：生成的模型、物品显示定义、语言资源。
- `src/gametest`：自动客户端与网络验证，和游戏 JAR 隔离。
- `src/rulesTest`：不依赖游戏启动的精力数值检查。
- `art/source`：美术源数据。
- `docs/ROADMAP.md`：后续开发顺序和验收要求。
- `docs/ARCHITECTURE.md`：模块、客户端/服务端边界、保存与依赖计划。
- `docs/VERIFICATION.md`：工程初始化的历史验证记录。
- `docs/P1-RULES.md`：当前精力行为、保存格式与回退规则。
- `docs/R0-FINDINGS.md`：机械与 Fuse 的实际可行范围。
- `docs/P1-VERIFICATION.md`：R0/P1 的历史运行证据。
- `docs/P2-RULES.md`：当前攀爬操作、参数与保存边界。
- `docs/P2-VERIFICATION.md`：P2 的运行证据与尚未覆盖事项。
- `docs/P3-RULES.md`：滑翔伞装备、双手、操作与保存规则。
- `docs/P3-VERIFICATION.md`：P3 的历史检查与限制。
- `docs/P3.1-SPEC.md`：用户确认的登记、背负和左上角 HUD 合同。
- `docs/P3.1-RULES.md`：背负实际行为、保存与回退。
- `docs/P3.1-VERIFICATION.md`：P3.1 成品和真实双客户端的证据与限制。
- `art/`：原创图标源文件与模型、贴图说明。

## 后续工作

P3.1 已补齐背负与左上角 HUD，P3.2 完成正式单人林克时间，与共享精力、攀爬和滑翔衔接；联机不启用全服减速。P4A 已完成环境温度，下一计划 P4B 完成料理与有限细雪辅助。原有能源、机械、制造机、Fuse 和天气继续保留，完整顺序见 [开发规划 v2](DEVELOPMENT-PLAN-V2.md)。

运行依赖只有 Fabric Loader 与 Fabric API。公开发行许可证尚未选择；模板与第三方工具来源见 `NOTICE.md`。

版本固定在 `gradle.properties`。不跟随快照或每日构建自动升级；Minecraft 升级在独立分支中完成，确认存档、联网与资源兼容后再合并。

P3.2 当前共 27 项 GameTest、8 个成品客户端类，另有 4 项普通客户端计时，见 [P3.2 验证](P3.2-VERIFICATION.md)。技能控制不保存，旧玩家格式保持；设置为本机 `config/wildcraft-focus.json`。


## P4A 环境温度操作

左上角精力下方显示极冷到极热七档环境状态。原版群系、当前位置实际雨雪、浸水、可见营火/火/岩浆影响读数；遮蔽、距离与点燃状态生效。普通冷热不额外扣血、减速或增加精力费用，原版细雪冻结及皮革防护照常。状态会逐渐过渡，读数是环境相对冷热而非摄氏度。

按 F8 在 Wildcraft 画面设置中切换“温度边缘色调”；关闭不隐藏温度读数，也不影响玩法。林克时间主动暂停该色调，保留温度状态。偏好保存在本机 `config/wildcraft-temperature.json`，旧 Focus 偏好保持原格式。

管理员只读诊断：`/wildcraft temperature status`，或指定玩家。温度不保存为体温，进入/复活/切维度重新取样。详见 [规格](P4A-SPEC.md) 和 [验证](P4A-VERIFICATION.md)。专项真实客户端：

```sh
# 仅操作者已阅读并接受 Minecraft EULA 后使用参数
./dev.sh -PacceptMinecraftEula=true --no-configuration-cache runTemperatureClientTest
```

## P6 机械操作

右键方块放置机械主体。停机时手持风扇或已充电的电池，对准六个固定节点右键安装；浅绿色预览表示当前可安装位置，最终由服务端确认所有权和真实物品。每台最多一个电池，随身红石块不能供电。蹲下空手对准节点右键拆下；必须先停机。顶部绿色控制板采用蹲下空手右键启停，空节点不作为开关。

空手站立右键乘坐，A/D转向，机械启停键默认R（可重绑），蹲下离开。电量耗尽没有推力，重力和惯性仍保留。停机、拆空并让乘客离开后，蹲下空手攻击主体回收一件。创造模式安装也转移真实一件；回收需真实库存空间。当前主体轴对齐碰撞、部件外观不独立碰撞，复杂物理后补。
