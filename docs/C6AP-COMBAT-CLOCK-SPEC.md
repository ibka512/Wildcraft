> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6AP：第三人称近战暂停画面一致性

2026-10-18；基线本机 dev63（源码27f0d086、交接998ba6ae）。用户授权继续本机开发，不公开上传。

## 问题与范围

第一人称普通剑击已使用原生冻结的render partial。第三人称普通剑击与HeavyMeleeRig仍在暂停时将partial强制置零，可能在开暂停菜单时让皮肤与持物回跳到整刻。实际客户端另复现MeleeControls在暂停菜单打开/暂停标志生效的边缘发送CANCEL，导致已经松开且结算的攻击直接消失。只对未开放局域网的单人暂停菜单保留这类攻击的恢复阶段；准备、普通GUI、非暂停服务端保持原取消条件。

按优先级验证：①暂停时归零的fraction导致源采样倒退；②源切换混合权重发生变化；③原生状态同步改变age/phase。测试观察实际提交AvatarRenderState的MeleeView、PARTIAL和FRAME，不调用createRenderState(player,1)伪造小数进度。

## 验收

- 真实按键发起剑/斧普通与强击，单人5TPS、10FPS下打开真实PauseScreen。
- 实际非零冻结partial与当前age/duration推导的源时间一致；普通剑保留源`.65+1.4*t`窗口，其他已支持攻击保留原归一化时段。
- 暂停期间继续绘制，源时间和完整皮肤矩阵冻结；恢复后一次原生hit/wear/Fuse结算。
- 定向矩阵：两体型、左右主手、空副手/真实携盾、剑/斧、普通/强击，共32项。蓄力中打开菜单可能触发既有合法取消，不把取消误称持续蓄力通过。
- 修复后跑现有HeavyMelee、RigRuntime、FirstPersonSword、Melee、Equipment、Guard六类定向回归与125服务端/构建；加上新专项共七类，不重跑无变化full48。
- 追加既有AirMeleeRig专项检查自然跳跃、落地、下落及原有退出；缺新FP数据实际回退。不将旧空中暂停断言当作精确fraction资格。

不改物理、库存、伤害、费用、冷却、服务器时间或网络格式；不新增生产依赖、美术数据、连段或存档迁移。其他美术四个工作区改动保持。若实际复现不成立则不应用推测性修复。

## 可复现入口

`./dev.sh runCombatClockClientTest -PacceptMinecraftEula=true -PcombatClockRunDir=<新的隔离测试目录>` 为单项红/绿检查。增加 `-PcombatClockMatrix=true` 跑32项；`-PcombatClockRegression=true` 跑32项及六类既有定向回归。每次使用独立目录保留失败证明。证据根为 `[原开发机外置卷]/Wildcraft/开发接续记录/2026-10-18-C6AP`。
