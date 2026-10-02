# P3.1 背负与左上角 HUD 验收记录

日期：2026-10-02（Asia/Shanghai）。版本 `0.1.0-dev.5`，Minecraft 26.3。用户确认背负按实际使用登记，并在开发中指定左上角红心、下方精力；本轮完成这两项及其与既有滑翔的衔接。P3.2 正式林克时间尚未实现。

## 环境与结果

沿用 macOS / arm64、Homebrew JDK 25、Fabric Loader 0.19.5、Fabric API 0.161.0+26.3、Loom 1.18.2 和 Gradle Wrapper 9.7.1。入口和 Mixin 签名核对锁定的游戏/API 源码，再实际编译和运行；本轮没有升级技术栈或增加生产依赖。

| 检查 | 实际结果 |
| --- | --- |
| 资源生成 | 成功，已有模型、配方与中英资源未产生语义变更 |
| 构建与纯数值 | 成功，13 个示例 + 1000 组边界/单调性检查通过 |
| 无图形 GameTest | 25 项通过、0 失败：24 项项目测试 + 1 项原版测试，其中新增背负 5 项 |
| 成品图形客户端 | 7 个测试类完成并正常退出：P0、R0、P1、P2、P3、R1、P3.1 |
| 两个独立普通图形客户端 | 新进程 WCActor / WCObserver 连接独立 DedicatedServer，全部 12 个阶段通过并退出 |
| 不安装测试模组的独立服务端 | SERVER 环境仅加载正式 Wildcraft 与 Fabric API；输出 Done，响应 list，stop 保存并退出 |
| 成品内容 | 54 个 Java 25 类、9 个公共和 7 个客户端 Mixin；无研究/测试类、AppleDouble、缓存或游戏文件 |

普通 `test` 无测试来源，没有把空任务记成通过。最终安装 JAR 与双客户端实验冻结 JAR 的 SHA-256 相同：`237025586233ce69418ab8e7ebc99d08e79344b29df3c8732d1cf259339949dd`。

## 服务端所有权与保存

新增五项 GameTest 检查实际近战、盾/弓使用、斧削皮和预装填弩发射；空切快捷栏、无弹药弓和荆棘反伤不登记。检查同款两把剑、对象移动、原版鼠标携带/放置与箱子快捷转移；复制式事务前后只有唯一匹配才接受，无法确认不改绑备用物品。

耐久变化更新签名，损坏与离开所有权清空。展示副本剔除私人数据、容器、名称等，保留允许的模型与光效。原版玩家保存/加载保留独立格式 1 的引用，只绑定保存位置上的现有物品；瞬时会话和展示附件不写入存档。旧精力和滑翔伞格式 1 读取继续通过。

## 成品客户端与左上角界面

实际攻击/举盾/拉弓通过游戏网络路径得到服务端确认；持副手盾拉弓时第一、第三人称盾均收纳到背部，真实库存不变。同款备用剑拿在手中不会误隐藏已登记剑；拿起实际登记剑则隐藏背负。客户端没有收到完整保存签名。

截图与提取状态检查覆盖三类原版模型、自己的第一/第三人称、实际潜行、穿甲、游泳。控制客户端使用宽模型；普通联机客户端的默认 Alex 窄模型亦有截图。可见披风的最低优先规则用受控渲染状态检查，未把它称为真实账号披风服务验证。鞘翅隐藏中央盾/弓，近战可能被翼遮挡，完整腰侧属于后续范围。

界面检查包括中英文低精力、额外最大血量和吸收红心、多行下移、GUI 缩放、大数值缩写、F1、补满后隐藏、底部当前 5 级经验和骑乘跳跃条。红心只改变坐标，状态图案仍由原版方法生成；不是重新实现生命值或伤害逻辑。

重开保存世界、下界来回、保留物品死亡和普通死亡/复活均检查记录是否保留或失效。跨维度后等待原版加载确认才触发伤害，没有修改原版维度保护。

本轮成品测试同时回归原来的精力、攀爬、滑翔、专用槽、满库存、原版点击/创造权限、实际按键和禁止飞行的回环 TCP 流程；R1 仍仅存在于独立测试模块。

## 真正的双客户端验证

这项使用三个独立 Java 进程：一个服务端和两个普通图形客户端；客户端日志明确 `fabric.client.gametest=null`，没有使用 Fabric 受控客户端调度器代替真实观察者。

服务端绑定 `127.0.0.1:25631`，离线测试身份，`allow-flight=false`。客户端实际攻击与使用物品后登记；观察者从自身收到的展示附件确认每阶段，截图保存另一玩家的模型。依次检查：三类背负 → 手握已登记剑 → 原生按跳跃滑翔与双手收纳 → 丢弃剑 → 原版箱子事务转移盾 → 切维度后离开跟踪 → 返回后仅剩弓 → 观察者真正断开 TCP 再连接 → 重新跟踪显示正确。测试准备场景用专用命令和传送；滑翔输入、正常物品操作与观察来自实际客户端。

观察者每阶段确认收到的视图，不读取另一个进程的玩家对象；退出前服务器输出 `TWO INDEPENDENT GRAPHICAL CLIENTS PASSED`。这证明本机双客户端短流程，未扩大为两台电脑、高延迟或长期多人稳定性承诺。

## 重复运行入口

先关闭旧实验进程，按顺序冻结 JAR；普通单进程资源生成和构建仍使用原有入口：

```sh
./dev.sh --no-configuration-cache runDatagen
./dev.sh -PacceptMinecraftEula=true --no-configuration-cache build prepareP31Lab
./dev.sh -PacceptMinecraftEula=true --no-configuration-cache runPackagedClientTest
```

下面三个入口分别在三个终端启动；服务端输出 Done 后启动客户端。不要在它们运行时重新执行 prepareP31Lab 或改写冻结 JAR。

```sh
WILDCRAFT_PROJECT_CACHE="$HOME/Library/Caches/Wildcraft/projects/p31-server" ./dev.sh --no-configuration-cache runP31Server
WILDCRAFT_PROJECT_CACHE="$HOME/Library/Caches/Wildcraft/projects/p31-actor" ./dev.sh --no-configuration-cache runP31Actor
WILDCRAFT_PROJECT_CACHE="$HOME/Library/Caches/Wildcraft/projects/p31-observer" ./dev.sh --no-configuration-cache runP31Observer
```

服务端输入 stop 结束。单独检查无测试模组服务端使用 `runP31StandaloneServer`，端口 25632。实验目录在本机 `~/Library/Caches/Wildcraft/labs/p31`，不是个人世界；准备入口只在明确添加已接受协议的参数后写 EULA。首次运行需要联网下载 Fabric/Minecraft 运行文件；没有把缓存或游戏文件随源码分发。

## 产物、证据与边界

本地安装包 `wildcraft-0.1.0-dev.5+mc26.3.jar`，源码包 `Wildcraft-P3.1-source.zip`。仍需对应 Fabric Loader 与 Fabric API，客户端与服务端均安装正式 JAR；测试 JAR 不用于日常游玩。分支 `feature/equipment-visuals`，本地标签 `v0.1.0-dev.5+mc26.3`；没有推送、发布或部署。

证据在输出目录 `verification/p31/`：

- [构建](verification/p31/build-final.log)、[数据生成](verification/p31/datagen.log)、[25 项 GameTest XML](verification/p31/gametest-results.xml)
- [成品客户端](verification/p31/packaged-client-test.log)、[无测试模组服务端](verification/p31/standalone-server.log)
- [双客户端服务端](verification/p31/two-client-server.log)、[操作者](verification/p31/two-client-actor.log)、[观察者](verification/p31/two-client-observer.log)
- [左上角中文精力](verification/p31/upper-left-stamina-zh.png)、[多行红心与缩放](verification/p31/upper-left-multiple-hearts.png)、[大容量精力](verification/p31/upper-left-large-capacity.png)
- [观察者看到三类背负](verification/p31/two-client-back-equipment.png)、[看到滑翔](verification/p31/two-client-gliding.png)、[重连后正确记录](verification/p31/two-client-reconnected.png)

当前没有环形精力样式、腰侧布局、收取动画、正式林克时间、料理温度、机械或正式 Fuse。没有测试个人账号实例、真实账号披风、高延迟/丢包、长时间多人、其他动画/物理/装备模组、Windows/Linux 或远程 CI。自动化实际客户端检查不替代人工操作手感。日志中的上游图形/JDK 警告保留，成功不表示零警告。

保存与回退见 [规则](wildcraft/docs/P3.1-RULES.md)；只操作独立测试世界，没有迁移个人存档。旧 P3 和 R1 产物、原始设计文档及其历史验证均保留。
