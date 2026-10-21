> 本机阶段记录；截图、世界、源稿和含私有数据包未公开。本文是历史结果，不是本次公开克隆的执行回执。

# C7 电池原生图集缺口修复规格

本机dev76与旧公开dev62的客户端启动日志均真实记录：battery_3d基础模型的wildcraft:entity/battery不在模型图集中。特殊渲染器直接使用该32px纹理，已有3D外观不因此等同全错；问题是原生基础模型/粒子材质没有解析到该图块。不得扩大成电池容量/费用/物理重做。

诊断排序：

1. 优先检查26.3物品图集是否未收entity目录：原生items atlas默认item目录，本项目模型引用entity/battery，项目没有补atlas登记。预测：真实item atlas与加载后的模型particle均为missing；只补single source后两者成为原sprite，警告消失，原生其他item图块仍可用。
2. 路径拼写/源复制缺失：预测游戏resource manager没有该texture。已确认源文件存在且导入来源SHA匹配，仍由运行包与真实atlas检查排除。
3. PNG异常：预测原生texture加载失败或尺寸错误。源文件32×32且原SHA保持，实际sprite同尺寸验证排除；不重绘或替换已采用原件。

先运行实际客户端回归断言红，冻结正式dev76与执行test JAR/源码。然后只新增项目自有的minecraft:items atlas single映射到现有wildcraft:entity/battery，保留原source/model/texture/adoption记录，派生候选dev77。不能只过滤日志或把测试期假sprite当修复。

验证实际GPU加载的32px sprite、19档GUI电量边界、左右第一/第三人称与GROUND五种原生基础材质、四类vanilla材质未被atlas扩展覆盖、真实库存电池1000不变。固定红/绿对照必须用同一个回归测试字节，只换生产资源；补已有第二批美术与能源/机械关联检查。资源生成后再构建/125服务端，七私有动画JSON与338生产classes逐内容核对。候选没有通过前不标正式77；本机包/许可/证据与完成未完失败分开记录，不上传GitHub。

官方26.3游戏JAR中的atlas/schema与本机native API用于只读核实，不复制游戏源码/文件到项目。使用既有JDK/Gradle/API，不加生产依赖、不迁移真实数据、不改存储/电量/精力/时长；其他四美术WD/untracked保留。
