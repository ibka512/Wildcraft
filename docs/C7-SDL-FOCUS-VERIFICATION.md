> 本机阶段记录；截图、世界、源稿和含私有数据包未公开。本文是历史结果，不是本次公开克隆的执行回执。

# C7 独立窗口焦点复核

2026-10-20，固定正式dev80包，仅本机gametest源集探针。当前系统锁屏，未尝试解锁或修改用户设置。

- 只读NSWorkspace前台为loginwindow/PID177；ioreg明确CGSSessionScreenIsLocked=Yes。
- 观察运行实际完成3个15秒短场：SDL窗口INPUT_FOCUS=false、键盘窗口none，未隐藏/未最小化；引擎active/focused=true。三场分别1800/1798/1797帧全部如此。旧检查错误接受了无原生键盘焦点的样本，不能把这三场算前台性能通过。
- 修复仅测试准入：requireFocus额外同时要求SDL INPUT_FOCUS、真实keyboard窗口为self、非hidden/minimized，并保留原引擎检查；失败保存原生读数。
- 修后实际20秒启动并在第一测量帧拒绝：无完整场，断言Independent SDL focus rejected native sample。它证明锁屏无焦点反例被正确排除；当前没有解锁后的正例，完整前台采样资格仍待补验。

此前六场/360秒的原始JSON、7截图和循环/TPS读数保留，只证明记录到的窗口尺寸/引擎缓存/正常世界调度；前台渲染性能结论撤回。没有证据证明此前也锁屏，但未记录独立SDL/OS真值，不能反向宣称当时处于前台。也不把缓存差异武断归因于Fabric注入。

证据在外层2026-10-20-C7-SDL-FOCUS：PLAN、OS-READBACK、observe/guard日志与结果、两份实际probe执行包和SHA。生产JAR/玩法/物理/费用/保存/素材不变。后续机械普通时间验证是程序输入提供部件观察，不作为前台性能或人工手感结论。
