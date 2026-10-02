# P2 攀爬验收记录

日期：2026-10-02（Asia/Shanghai）。版本 `0.1.0-dev.3`，本轮授权范围是 P2 攀爬，未提前实现 P3 滑翔伞。

## 环境与依赖

沿用 Apple M4 / arm64、macOS 27.0.1、JDK 25.0.4.1、Minecraft 26.3、Fabric Loader 0.19.5、Fabric API 0.161.0+26.3、Loom 1.18.2 和 Gradle Wrapper 9.7.1。没有新增生产依赖，也没有修改玩家保存格式或迁移个人世界。

核对了 Fabric 官方 [按键映射](https://docs.fabricmc.net/develop/key-mappings) 与 [网络通信](https://docs.fabricmc.net/develop/networking) 文档。官方页面当前标注 26.2，实际类名与事件还通过本项目锁定的 26.3 游戏源码和 Fabric API 源码确认；没有用旧页面中的类名代替实际编译验证。

## 实际执行结果

| 检查 | 结果 |
| --- | --- |
| 资源生成 | `runDatagen` 成功，生成中英按键分类、攀爬提示和原有资源 |
| 构建与数值检查 | `build gameTestJar` 成功，13 个数值示例和 1000 组边界/单调性检查通过 |
| 无图形 GameTest | 13 项项目测试 + 1 项原版测试，共 14 项全部通过 |
| 开发客户端 | 实际 Mac 窗口完成初版 P2 升降、横移、停留、墙顶、天花板、受击、耗尽与本机 TCP 验证 |
| 最终成品客户端 | 游戏 JAR + 单独测试 JAR 完成全部最终 P2 流程，另回归 P0/P1/R0；正常退出 |
| 最终成品独立服务端 | 新 Java 进程在 SERVER 环境加载 P2 JAR，输出 `Done`；控制台 `list` 响应，`stop` 保存并退出 |
| 包内容 | 元数据、Mixin、标签、中英语言和 Java 25 字节码完整；不包含测试/研究类、缓存或 AppleDouble 附属文件 |

普通 `test` 仍无来源，未把空任务计为测试通过。最终客户端包含后来加入的内外转角、生命周期和 0 级 HUD 修复；这些最终变化的游戏证据来自成品客户端。

## P2 验证范围

5 项新的服务端 GameTest 覆盖：

1. 完整方块、半砖实际侧面、半砖上方空气、无碰撞植物、接触间隙、开放墙顶、阻挡墙顶与内外墙角的几何判断。
2. 100 次重复输入不会按包重复收费；每服务端刻只消耗一次，停留费用正确，实际移动不能声称停留以降低费用；抓墙时地面接触不允许恢复。
3. 耗尽归零、抓握锁、放手后重抓、伤害回调及非法方向轴拒绝。
4. 合法小步与超量位移限制、原版玩家序列化不保存攀爬状态、重新加载保留精力而不恢复抓墙。
5. 无心跳输入超过 20 刻后自动放手。此测试等待 23 刻，明确配置 40 刻测试时限。

最终成品客户端通过真实按键与原版移动包检查：

- 按住 G 与 W 上墙，S 下降，A/D 横移；停留保持高度并消耗服务端精力，松开恢复下落。
- 实际移动越过开放墙顶并落到顶部；天花板阻止身体穿过；横移连续绕过凸角并转过凹角。
- 实际原版伤害中断抓握，耗尽恢复重力，继续按住不能即时恢复被锁住的抓握。
- 挂墙时死亡、复活和跨维度往返，服务端清理攀爬状态；原版精力死亡规则仍通过 P1 回归。
- 回环 TCP 的 DedicatedServer 对象设置 `allow-flight=false`；上墙后保持 90 刻，客户端与服务端高度差小于 0.15 格，连接未被飞行检查踢出；挂墙时断线后重新连接不恢复抓握。
- 中英文实际画面显示精力与攀爬提示；截图中的玩家当前和历史等级均为 0，精力条仍可见。

测试只用传送准备起始位置和切维度，攀爬步骤本身由按键、预测、原版碰撞与移动包完成。

## 验证中发现并修复

- 新增生命周期处理后，回归暴露复活补满仍可能依赖事件注册顺序。P1 补满和 P2 清理现在使用排在默认阶段之后的明确事件阶段，确保 Fabric 自动附件转移先完成。
- 0 级玩家不会绘制原版经验数字；把精力 HUD 挂在其后会继承隐藏条件。改挂在快捷栏后，实际中文与英文截图确认 0 级可见，HUD 隐藏仍继承原版条件。
- 转角处使用受限的碰撞移动过渡，保持相邻接触面，不直接传送穿过转角。实际内外转角验证通过。
- 实际位移参与费用判断，不能只靠客户端上报的方向轴决定更低费用。

## 保存、边界与日志

持久玩家数据仍为 P1 格式 1。攀爬状态和输入不持久化，原版保存/加载检查确认没有 `wildcraft:climb_*` 数据。规则和默认参数见 [P2-RULES.md](P2-RULES.md)。

本轮使用此前授权的本机 EULA 测试目录。独立服务端仍仅绑定回环地址，复用了 P1 的专用验证世界，没有操作个人存档。网络游戏检查使用框架中的服务端对象，独立 SERVER 类加载检查另启动 Java 进程；未把两者混称为两台电脑的多人验证。

日志保留了官方测试辅助方法弃用提示、离线身份的账号属性/Realms 鉴权提示、测试默认过滤设置提示及 Java/图形驱动的上游警告。构建与完整测试成功不代表日志零警告。最终成品客户端日志没有 `moved wrongly` 或 `moved too quickly` 提示。

尚未覆盖：真实多客户端长期游玩、高延迟网络、其他模组的特殊碰撞、Windows/Linux、个人启动器账号实例和远程 CI。没有添加专用手脚攀爬动画、攀爬跳跃或天气影响；它们的后续边界见路线。P2 参数仍需实际生存游玩平衡。

## 产物与证据

安装包为 `wildcraft-0.1.0-dev.3+mc26.3.jar`，源码包为 `Wildcraft-P2-source.zip`。安装仍需对应 Fabric Loader 和 Fabric API。源码包包含项目、测试与说明，不包含游戏文件、依赖缓存、世界、账号凭据或 EULA 接受文件。

输出目录的 `verification/p2/` 保存最终构建、资源生成、开发客户端、最终成品客户端、独立服务端日志、14 项 GameTest XML 和四张 P2 游戏截图。

- [最终构建日志](../development-assets/verification/p2/build-final.log)
- [成品客户端日志](../development-assets/verification/p2/packaged-client-test.log)
- [独立服务端日志](../development-assets/verification/p2/packaged-server.log)
- [GameTest XML](../development-assets/verification/p2/gametest-results.xml)
- [中文攀爬截图](../development-assets/verification/p2/climbing-wall-zh.png)
- [墙顶截图](../development-assets/verification/p2/climbing-over-ledge.png)
- [外墙角截图](../development-assets/verification/p2/climbing-outer-corner.png)
- [本机 TCP 攀爬截图](../development-assets/verification/p2/climbing-tcp.png)

本轮只建立本地提交与标签 `v0.1.0-dev.3+mc26.3`。旧产物与历史验证保留，未推送或公开发布。完整产物哈希与提交信息见输出目录的 `manifest.json` 和 `SHA256SUMS.txt`。
