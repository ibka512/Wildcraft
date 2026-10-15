# C1 统一输入基础规格

2026-10-06，基线dev.18，本机目标dev.19。范围为已确认路线的输入基础和现有玩法接入。新锁定、独立举盾、闪避与攻击规则在C2/C3实施；本批不改原版攻击伤害或战斗费用。

## 已接入动作

`FORWARD/BACKWARD/LEFT/RIGHT`、`JUMP/SNEAK/SPRINT`、`ATTACK/USE/INVENTORY/DROP/SWAP_HAND`、九个快捷栏键，以及`CLIMB/FUSE/MACHINE_TOGGLE/SETTINGS`。动作按注册的原版KeyMapping采样，可被既有改绑及控制器原生键位桥接接入；C1尚不声明独立摇杆/震动SDK已交付。

输入上下文实际解析为`INACTIVE/GUI/NORMAL/AIMING/CLIMBING/GLIDING/MECHANISM/RIDING`。全局动作和锁定战斗的具体消费规则在对应功能出现时加入，避免暴露不起作用的新按键。

## 采样和交付

每渲染帧及START_CLIENT_TICK采样。ActionTracker把按下/松开边沿保留到下次刻交付，完整短按即使已经松开也保留。ActionFrame是不可变快照，几个消费者读取不会互相取走事件；唯一合法消费者由上下文/对应功能决定。

仅统一消费Wildcraft自己的原生点击队列。原版点击队列不读取、不复制；原版仍执行攻击、使用、背包等。每刻最多保留16条同类Wildcraft命令，完整排空原队列，避免异常重复无限积压。

GUI/叠层/暂停/失焦/角色不可操作时技能保持值清空，待发命令丢弃，已持有技能键进入隔离，直到真实松开。方向/潜行/疾跑修饰保持原版习惯。重建玩家及断线清理上一角色状态。

## 现有玩法适配

- ClimbControls从ActionFrame读取G保持与前后/左右方向，原服务端资格、费用、碰撞和心跳不变。
- GlideControls从新的JUMP边沿开收伞，仍要求独立真装备位及实际离地；物品/快捷栏变化或取用动作收伞。按住地面跳跃不能自动开伞。
- FusionPresentation读取FUSE点击与SNEAK修饰，仍发送原FusionIntent，材料/次数/原子事务不变。
- MachinePresentation仅在真实机械乘坐上下文发送MACHINE_TOGGLE，驾驶移动及拆卸仍由既有原版路径执行。
- FocusClient读取SETTINGS命令，渲染帧读取USE释放一次原版弓；滤镜配置和真实精力费用不变。

旧公开静态KeyMapping字段保留别名，调用者和既有测试不用更换按键引用。设置标识和原版改绑文件保持，不新增客户端配置迁移。

## 验收

验证规则边沿/不可变快照/GUI与失焦隔离，再验证真实客户端改绑→融合→GUI不重放→拆分→攀爬→开收伞→设置；回归原生单人生存循环、空中弓箭和原生模型。额外C0原生调度实验必须不带fabric.client.gametest，不能把受控测试刻调度写成真实减速结果。Controlify启动/API绑定和实际控制器设备分别记录。
