# F06-01 接入说明

本版已采用、未接入，所有候选只存放于art/production-v2/F06-01/v01。尚未写入游戏资源目录或修改Java。

| 美术输出 | 后续目标 | 条件 |
| --- | --- | --- |
| exports/fabricator-atlas-64.png | wildcraft:block/fabricator_atlas纹理 | 用户采用后再按当前生成资源流程接入，保存Aseprite源 |
| exports/fabricator-static-model.json | 当前generated/assets/wildcraft/models/block/fabricator.json | 候选静态模型，使用中性状态；通过生成源维护，避免只覆写生成结果 |
| source/fabricator-v01.bbmodel | 方块源模型 | java_block，显式逐面UV，内嵌64px纹理 |
| source/fabricator-v01.blend | 可编辑几何和渲染参考 | PRODUCTION与REVIEW分组；state驱动仅供审稿，不能视为游戏动画已接入 |
| exports/fabricator-presentation.json | 三态选择约定 | 0/1→idle，2→working，3/4→complete；世界客户端同步尚缺 |
| review/*item-display*.png | 物品栏可读性检查 | 模型派生参考；不注册为独立物品图标 |

当前FabricatorBlock没有facing/state属性，当前Entity没有本地实现的状态更新包/标签或世界展示快照；ContainerData同步属于菜单。setChanged用于保存，不能据此假设周围客户端收到世界状态。

如后续接入世界状态，优先只传递必要的通用阶段，在权威状态到达后选择相应图集区域。不要同步整个保存NBT、pendingResult或隐藏随机产物来驱动这三个符号。完成符号可表示status3或4；只有完成转移后菜单产物槽才展示具体结果。不增加随机重抽、离线计时、电源系统、方块旋转或凹槽交互。

当前资源使用cube_all原型；正式模型接入后，方块放置、破坏、保存、材料/产物持久化、世界三态、物品栏及第一/第三人称持握需实机验证。静态neutral资源可以先验证形体，三态仍须完成同步后再声称已实现。
