# GUI换装取消回归：C6K/dev.35候选

2026-10-13，完整15类回归重跑中，未据此提前宣称dev.35验收。首次14类回归在原EquipmentClientTest第77行失败：GUI interruption never replays the old draw。原动作时限与断言未放宽。

## 稳定复现与根因

新独立EquipmentGuiClientTest用真实背包屏幕，在同一个客户端回调里打开再关闭，保证两次角色提取之间确实发生菜单过渡。旧代码日志：打开前真实Settle progress=.12782401，关闭后仍为.1474082，Visual同一reference=1/owner=1，直接复现保留旧draw。此测试夹具登记已有真实库存长矛用于视觉状态初始化；不声称测试了实际攻击登记。实际攻击登记/真实快捷栏/GPU由原EquipmentClientTest继续验收。

BackTransitions只在AvatarRenderState提取中观察屏幕。菜单可能在两次提取之间打开关闭，或GUI中不提取该角色，导致完全漏掉中断。此稳定复现无需更改同步包或等待时限，足以证明取消应在菜单事件消费。

## 最小修复与已执行检查

BackTransitions.initialize注册已存在Fabric ScreenEvents.AFTER_INIT，仅非暂停屏幕为本机玩家清理Motion；不影响其他玩家或物品库存/归属。暂停屏幕仍保留进度及冻结时钟。原render观察仍作为实际使用/攻击/探索等互斥路径。

同一复现改后after=null，green实际15秒成功。补充真实tick rate 1、原生carried-index同步：Native selected=0而权威Visual仍owner3时打开关闭菜单，实际提取后等待owner1，首个被观察帧无取消旧reach，随后实际GPU draw计数0。v3 18秒通过，未添加为了假设的网络状态机、存档数据或新依赖。

第一次补充测试误直接调用private ensureHasSentCarriedItem，编译失败；复用项目已有CarriedItemAccess Invoker后v2、加强首个实际提取帧的v3均通过，不新增生产接口。保留原红/绿、编译失败与1TPS补充日志，不能把失败吞掉。旧完整回归仍需实际重新通过，暂停和5TPS1.2秒原检查保持。

证据development-assets/verification/guard-rig/diagnostics，原14类失败全日志和截图另在diagnostics/full-v1。清晰盾源专项v2已实际成功28秒/9图；它修的是测试目标遮挡（击中后同方向移到7.5格），不修改游戏判定。

## 完整v2发现延迟归属的第二条实际竞态

独立v3通过但完整15类v2在其加强后的“首个实际归属帧不应取出”断言失败，说明初始化仅清理Motion不足，保留失败全日志/截图diagnostics/full-v2。没有把独立偶然通过当作问题已解决。

最终候选改为在非暂停菜单实际初始化及移除时消费当前显示，并记下当前真实主/副手和选槽、同ref应当到达的owner。只要实际持物/槽和ref不变，对应迟到owner更新也消费为完成；真正物品状态/槽改变或ref变化就清除此候选，后续合法动作仍按原规则。此为客户端显示会话，既有库存/保存/公开接口不变；不按额外延时堵同步。

v4实际22秒通过：快速菜单开闭、1TPS原生carried同步延迟、首个实际观察帧与GPU零draw，以及菜单后新一次真实切换仍产生draw。暂停屏幕保留冻结；原Equipment 1.2秒/5TPS与完整15类仍须v3真实重跑，当前未归档正式dev.35。

## 最终实际验收

完整15类v3 8分6秒/496图全部通过，原EquipmentClientTest GUI取消、暂停与1.2秒5TPS条件原样通过；独立datagen/build/125服务端及45用途/4底部/2107源/57副本通过。本机dev.35归档，根因/红绿及两次完整失败仍保留作为诊断历史；没有按失败次数改变旧断言。
