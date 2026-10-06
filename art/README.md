# Wildcraft 美术与可编辑资产

2026-10-06，当前dev.17：核心14编号＋第二批29编号已采用且接入，统一状态见[ADOPTED-ASSETS](ADOPTED-ASSETS.md)、[CSV](ADOPTED-ASSETS.csv)、[JSON](ADOPTED-ASSETS.json)。

| 目录 | 用途 |
| --- | --- |
| [approved-v1](approved-v1/README.md) | 核心原件：伞、姿态、背负、精力、专注、品牌与设置；55份精选原件有来源校验 |
| [production-v2](production-v2/README.md) | 第二批29项当前编号、30份交付版本；模型、像素、音效、参考、采用与原始校验 |
| [production-docs](production-docs/README.md) | 历史制作进度、当前合同、待办与机械修正依据 |
| [mechanics-v1](mechanics-v1/README.md)、[fabrication-v1](fabrication-v1/README.md)、[focus](focus/README.md) | 历史可编辑原型；不替代当前采用状态 |
| tools | 核心网格与姿态烘焙，读取原件但不保存修改 |

核心转换见[CORE-ART-INTEGRATION](../docs/CORE-ART-INTEGRATION.md)，第二批转换见[SECOND-ART-INTEGRATION](../docs/SECOND-ART-INTEGRATION.md)。实际游戏资源在src，采用原件保持原字节；只对派生资源运行导入或烘焙，再执行资源生成和构建。

第二批导入使用`tools/import-second-art.py`，需要Python3和Pillow；核心角色烘焙需要Blender。普通Gradle构建不需要美术编辑器或Pillow，也不新增生产动画依赖。游戏保留真实原版玩家皮肤和物品模型。

当前待制作[B6风提示](production-docs/B6-WIND-ART-BRIEF.md)，待修订[机械底部](production-docs/P10.2-MECHANICAL-ART-REPAIR.md)；dev.17已调整侧轮接地，未改原件或碰撞。

原交付的未接入字段、旧排期、模拟预览与失败日志保持历史身份，不能覆盖当前实现。完整交付、母带和历史大包见[下载说明](../docs/ASSET-DOWNLOADS.md)。
