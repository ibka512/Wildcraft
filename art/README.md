# Wildcraft 美术源文件

2026-10-03 核心美术接入采用用户交付的 `Wildcraft-核心美术成果-2026-10-03.zip`。当前原始源稿与规格在 [approved-v1](approved-v1/README.md)，实际接入范围、转换方法和验证边界见 [核心美术接入](../docs/CORE-ART-INTEGRATION.md)。

`approved-v1/SOURCE-MANIFEST.json` 保留收到的 ZIP 哈希及 55 份精选原件的 SHA-256。源稿保持原字节；其中 `gameIntegrated: false` 等字段表示交付当时的状态，当前状态以开发记录为准。文件中的建议是资料，不能替代用户决定或项目规范。

游戏采用 5 张交付 PNG、2 段原创 OGG、展开伞的 22 部件网格，以及标准/细手臂两套动画烘焙数据。攀爬、滑翔和背负显示继续使用原版玩家皮肤与真实物品模型，不加入通用动画依赖。

`tools/bake-paraglider-mesh.py` 只需 Python 标准库；`tools/bake-character-poses.py` 通过 Blender 后台烘焙。命令、坐标和生成文件见接入文档。工具只读取源稿，不保存修改 Blender 原件。

`paraglider.svg`、`test-core.pixels.json` 和 [focus](focus/README.md) 中的旧说明保留开发历史。初始伞图标 SVG 已由交付的 A05 源稿替代；旧 JAR、标签和原始验证图片保持不动。[重设计计划](ASSET-REDESIGN-PLAN.md) 与 [CSV](asset-redesign.csv) 是制作时清单，不能单独作为当前接入状态。

P6–P7原创机械原型源见[mechanics-v1](mechanics-v1/README.md)：主体、八类部件图标与实体几何、节点吸附预览、电量条、火箭尾焰、弹簧伸展和稳定器工作灯。当前为可编辑原型，最终美术仍可替换。

P8制造机原创像素源见[fabrication-v1](fabrication-v1/README.md)，运行贴图由generate_mechanics_art.py生成；当前方块与界面均为可替换原型。
