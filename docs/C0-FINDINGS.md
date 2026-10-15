# C0 技术前置记录

2026-10-06，本机dev.19路线的前置实验。基线为dev.18，正式实施入口见[C1规格](C1-SPEC.md)，完整路线见[战斗/输入路线](COMBAT-INPUT-ROADMAP.md)。实验代码全部放`src/gametest`；正式JAR不提供实验战斗、实验动作或测试快捷键。

## 已实际验证的入口

| 问题 | 实验与事实 | 结论/边界 |
| --- | --- | --- |
| 短按/蓄力能否区别 | 测试模组的松开判别器用80ms短按、450ms保持、300ms门槛；重复松开及取消不重复发意图 | 可以形成一次轻击或蓄力意图；未替换原版攻击，未完成人工延迟/手感评估 |
| 身体、瞄准和移动参考系能否分开 | 内置服务端以目标水平向量执行原生`Player.move`前移/横移，保留73°逻辑朝向；原生AvatarRenderState仅改变bodyRot并重新计算头部相对角 | 模型朝向与自由瞄准可分；这不是正式锁定移动/镜头系统，C2仍需距离、遮挡、转向及碰撞专项 |
| 身体区域能否组合 | 原生标准/纤细PlayerModel setupAnim后，仅应用盾臂/躯干样本，腿部、头部及另一臂不被改写 | 原生6部位可承载分层；未引入新动画库，不把此样本算30–35份正式美术 |
| 手柄Mod是否与当前版本启动兼容 | 侧载Controlify3.5.3+mc26.3、YACL3.9.7+26.3-fabric，沿用锁定Fabric API0.161.0+26.3；真实客户端及5类专项通过。绑定API确认climb/fuse/machine_toggle/focus_settings各有1个关联 | 保留原生KeyMapping作为可选桥接可行。未新增生产依赖，未测试真实Xbox/PS/Nintendo/Deck、模拟轴、菜单完整手感或震动 |
| 真减速下输入能否保持响应 | `runInputNativeProbe`在普通渲染/世界调度下运行，无fabric.client.gametest；TimeLease把世界降至5TPS，约2.5秒中298渲染帧/13世界刻，保留8个40ms完整短按；租约关闭后20TPS | 帧输入和世界刻解耦可行；现有原版近战恢复/玩家位移仍受世界调度，C5必须另定反击时钟和权威处理，不能直接复用空中弓资格 |

上述测试均为自动样本，不等于人工动作手感、所有怪物前摇、完整锁定或完美闪避已交付。

## 采用的实施决策

1. 输入采样放渲染帧及客户端刻，保留边沿到刻交付；原版点击队列仍由原版处理一次。
2. 先迁入G、V、R、F8及现有读取路径，再逐步添加C2/C3战斗动作；不把未实现动作暴露成无作用按键。
3. 动画先对现有原生部位施加遮罩，不改方块体型、不引入弯曲肢体。正式遮罩调度与素材在C2以后建立。
4. Controlify只作为测试环境可选侧载。API关联通过后仍需实际设备、上下文共享扳机、动态提示及配置能力验证。新增SDK/PAL生产依赖单独按授权处理。
5. 精准格挡/闪避依赖可读怪物前摇；短时减速反击需要独立动作会话/退出原因，输入可以复用，但不能把FocusTime的bowSeconds直接改名当通用战斗时钟。

## 实验输入与来源

用户原稿见[原始两份文档及哈希](design-inputs/2026-10-06/manifest.json)。研究依赖仅本机缓存，下载来源、版本、SHA-512及许可字段见[controller-source.json](../development-assets/verification/input/controller-source.json)；测试时沿用既有Fabric版本。

Controlify注册API及原生按键的实际输出在[控制器实验日志](../development-assets/verification/input/controller-client.log)。`SteamDeckUtil`在本机日志提示增强驱动检查跳过，因此不声称Steam Deck驱动已验证。离线测试身份的Realms认证失败不是这些实验成功依据，也不表示真实账户登录已测试。

## 重现与未完成

```sh
./dev.sh -PacceptMinecraftEula=true --no-configuration-cache runInputClientTest
# 先运行上项以生成可丢弃的input-client-test/New World测试世界：
./dev.sh -PacceptMinecraftEula=true --no-configuration-cache runInputNativeProbe
# 侧载研究依赖已放本机cache/labs/c0-controller时：
./dev.sh -PacceptMinecraftEula=true --no-configuration-cache runControllerInputProbe
```

测试命令需要操作者自行接受Minecraft EULA。原生探针只复制自动创建的实验世界，不读个人存档。首次使用旧R1路径的失败记录保留在[diagnostics](../development-assets/verification/input/diagnostics/native-fixture-missing.log)，正式任务已改为本阶段测试世界；该失败不算通过。

未完成：正式锁定/独立举盾/闪避/蓄力攻击、怪物前摇/完美判定、减速近战反击、完整新动画、实际手柄与跨平台图形。首批C0入口验证与C1键鼠接入可以交付，不以未知硬件测试阻塞独立的后续键鼠实现。
