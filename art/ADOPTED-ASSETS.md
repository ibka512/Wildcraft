# Wildcraft 已采用与已接入美术台账

2026-10-06，当前公开开发版 **dev.18 / Minecraft 26.3**（handoff-dev18-2026-10-06）。核心14编号＋第二批29编号，共43项当前资产已采用且接入。第二批保留30份版本；F05-10当前v02，v01历史保留。

[CSV](ADOPTED-ASSETS.csv) · [JSON及运行文件校验](ADOPTED-ASSETS.json) · [下载原件与交接包](../docs/ASSET-DOWNLOADS.md)

本台账记录当前实现，不追溯改写原始交付中的`integrated: false`。原件描述交付当时状态，运行验收见对应阶段报告。原型、草稿、B6计划与底部修订不计入采用数量。

## 核心美术 v1（dev.7首次接入）

| 编号 | 用途 | 当前版本 | 状态 | 原稿 | 当前实现 |
| --- | --- | --- | --- | --- | --- |
| A01 | 精力 HUD | v1 | 已采用、已接入 | [原稿](../art/approved-v1/A01-精力HUD-v1/) | [入口](../src/client/java/dev/wildcraft/client/hud/StaminaHud.java)；精力 HUD |
| A03 | 展开滑翔伞模型 | v1 | 已采用、已接入 | [原稿](../art/approved-v1/A03-A04-展开滑翔伞-v1/) | [入口](../src/client/java/dev/wildcraft/client/render/ParagliderLayer.java)；展开滑翔伞模型 |
| A04 | 展开滑翔伞纹理 | v1 | 已采用、已接入 | [原稿](../art/approved-v1/A03-A04-展开滑翔伞-v1/) | [入口](../src/main/resources/assets/wildcraft/textures/entity/paraglider.png)；展开滑翔伞纹理 |
| A05 | 滑翔伞物品图标 | v1 | 已采用、已接入 | [原稿](../art/approved-v1/A05-A06-滑翔伞图标与装备槽-v1/) | [入口](../src/main/resources/assets/wildcraft/textures/item/paraglider.png)；滑翔伞物品图标 |
| A06 | 滑翔伞装备空槽 | v1 | 已采用、已接入 | [原稿](../art/approved-v1/A05-A06-滑翔伞图标与装备槽-v1/) | [入口](../src/main/resources/assets/wildcraft/textures/gui/sprites/container/slot/paraglider.png)；滑翔伞装备空槽 |
| A07 | 滑翔握持与姿态 | v1 | 已采用、已接入 | [原稿](../art/approved-v1/A07-滑翔握持与姿态-v1/) | [入口](../src/client/java/dev/wildcraft/client/render/CharacterPoses.java)；滑翔握持与姿态 |
| A08 | 攀爬动作 | v1 | 已采用、已接入 | [原稿](../art/approved-v1/A08-攀爬动作-v1/) | [入口](../src/client/java/dev/wildcraft/client/render/CharacterPoses.java)；攀爬动作 |
| A09 | 背负装备摆放与挂点 | v1 | 已采用、已接入 | [原稿](../art/approved-v1/A09-背负装备挂点-v1/) | [入口](../src/client/java/dev/wildcraft/client/render/BackEquipmentLayer.java)；背负装备摆放与挂点 |
| A10 | 背负装备收取过渡 | v1 | 已采用、已接入 | [原稿](../art/approved-v1/A10-背负装备收取过渡-v1/) | [入口](../src/client/java/dev/wildcraft/client/render/BackTransitions.java)；背负装备收取过渡 |
| A11 | 林克时间画面 | v1 | 已采用、已接入 | [原稿](../art/approved-v1/A11-林克时间画面-v1/) | [入口](../src/client/java/dev/wildcraft/client/focus/FocusClient.java)；林克时间画面 |
| A12 | 专注准星与低精力提示 | v1 | 已采用、已接入 | [原稿](../art/approved-v1/A12-专注准星与低精力提示-v1/) | [入口](../src/client/java/dev/wildcraft/client/focus/FocusClient.java)；专注准星与低精力提示 |
| A13 | 专注进入/退出音效 | v1 | 已采用、已接入 | [原稿](../art/approved-v1/A13-专注进入退出音效-v1/) | [入口](../src/main/resources/assets/wildcraft/sounds/focus_enter.ogg)；专注进入/退出音效 |
| A14 | 品牌图标：精细展示/简单识别 | v02 | 已采用、已接入 | [原稿](brand-v2/A14/v02/README.md) | [入口](../src/main/resources/assets/wildcraft/icon.png)；原生20设置页；旧v1保留 |
| A15 | 表现设置页 | v1 | 已采用、已接入 | [原稿](../art/approved-v1/A15-表现设置页-v1/) | [入口](../src/client/java/dev/wildcraft/client/focus/FocusSettingsScreen.java)；表现设置页 |

## 第二批系统美术（dev.15首次接入）

| 编号 | 用途 | 当前版本 | 状态 | 原稿 | 当前实现 |
| --- | --- | --- | --- | --- | --- |
| A07-A08-C | 探索动作衔接 | v01 | 已采用、已接入 | [原稿](../art/production-v2/A07-A08-C/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/render/CharacterPoses.java)；攀爬/滑翔短衔接、最短旋转路径、取用物品即时接管 |
| A09-C | 披风/鞘翅下侧腰装备 | v01 | 已采用、已接入 | [原稿](../art/production-v2/A09-C/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/render/BackEquipmentLayer.java)；披风/鞘翅下五类装备改放侧腰，真实归属立即生效 |
| A13-C | 专注音效混音 | v01 | 已采用、已接入 | [原稿](../art/production-v2/A13-C/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/focus/FocusClient.java)；原专注音的进入 0.22 / 退出 0.28 混音参数 |
| F01-01 | 料理锅 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F01-01/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/art/ArtBlockRenderer.java)；料理锅原生方块模型与 64px 图集 |
| F01-02 | 四类料理 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F01-02/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/art/MealKindProperty.java)；四种实际料理类别分流，强度 I/II 共用同类图标 |
| F01-03 | 料理效果HUD | v01 | 已采用、已接入 | [原稿](../art/production-v2/F01-03/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/cooking/MealHud.java)；三效果 HUD，真实等级与有限倒计时、窄屏排布 |
| F01-04 | 料理锅界面 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F01-04/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/cooking/CookingScreen.java)；176×182 料理界面，真实五槽、五状态与进度 |
| F02-01 | 七档温度HUD | v01 | 已采用、已接入 | [原稿](../art/production-v2/F02-01/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/temperature/TemperatureHud.java)；七档温度图标、读数及弱边缘反馈，专注层优先 |
| F03-01 | Fuse组合与挂点 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F03-01/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/fuse/FusionArrowRenderer.java)；剑/斧/盾/箭及飞箭融合挂点，保留原版宿主外观 |
| F03-02 | 融合标记与次数 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F03-02/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/fuse/FusionPresentation.java)；原生 5px 槽位角标、真实剩余次数、低次数提示 |
| F03-03 | 未知材料回退 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F03-03/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/fuse/FusionPresentation.java)；未知或缺失材料模型的中性回退，原材料数据保留 |
| F03-04 | Fuse有限特效 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F03-04/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/art/ArtFeedbackClient.java)；火、冰、弹性与融合/拆分的五种有限原版粒子反馈 |
| F04-01 | 有限电池 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F04-01/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/art/BatteryItemRenderer.java)；有限电池三维模型，真实电量驱动三个窗口 |
| F04-02 | 电量图标与读数 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F04-02/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/art/ArtGui.java)；空/满图标及中间遮罩、精确 0–1000 读数 |
| F04-03 | 充电器 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F04-03/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/art/ArtBlockRenderer.java)；充电器世界模型，原尺度电池与五态面板 |
| F04-04 | 充电器界面 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F04-04/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/energy/ChargerScreen.java)；176×166 充电界面，五态文字与完整悬停说明 |
| F05-01 | 机械主体与节点 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F05-01/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/mechanics/MachineRenderer.java)；机械主体、六节点和控制板模型 |
| F05-02 | 机械翼 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F05-02/v01/README.md) | [入口](../src/main/resources/assets/wildcraft/geometry/wing.json)；被动机械翼，128×64 布木图集 |
| F05-03 | 风扇 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F05-03/v01/README.md) | [入口](../src/main/resources/assets/wildcraft/geometry/fan.json)；风扇护框、轮毂和真实工作转动 |
| F05-04 | 火箭及燃尽空壳 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F05-04/v01/README.md) | [入口](../src/main/resources/assets/wildcraft/geometry/rocket.json)；有限火箭、燃尽空壳与既有尾焰 |
| F05-05 | 电池安装适配 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F05-05/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/mechanics/MachineRenderer.java)；同一电池的六向安装，采用 v02 复核后的偏移 |
| F05-06 | 弹簧 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F05-06/v01/README.md) | [入口](../src/main/resources/assets/wildcraft/geometry/spring.json)；弹簧压缩/伸展，由现有冷却状态驱动 |
| F05-07 | 车轮 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F05-07/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/mechanics/MachineRenderer.java)；车轮网格和既有转速驱动 |
| F05-08 | 稳定器 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F05-08/v01/README.md) | [入口](../src/main/resources/assets/wildcraft/geometry/stabilizer.json)；稳定器壳体、真实 active 工作灯 |
| F05-09 | 浮力装置 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F05-09/v01/README.md) | [入口](../src/main/resources/assets/wildcraft/geometry/buoyancy.json)；浮力装置，复用布木图集和现有载重规则 |
| F05-10 | 统一吸附预览与状态 | v02 | 已采用、已接入 | [原稿](../art/production-v2/F05-10/v02/README.md) | [入口](../src/client/java/dev/wildcraft/client/mechanics/MachinePresentation.java)；统一透明吸附预览、节点与可用/受阻/工作文字 |
| F06-01 | 古代装置制造机 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F06-01/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/art/ArtBlockRenderer.java)；制造机原生方块模型与空闲/工作/完成面板 |
| F06-02 | 制造机界面 | v01 | 已采用、已接入 | [原稿](../art/production-v2/F06-02/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/fabrication/FabricatorScreen.java)；176×192 制造界面，真实 39 槽与原按钮协议 |
| S01 | 12段原创短音 | v01 | 已采用、已接入 | [原稿](../art/production-v2/S01/v01/README.md) | [入口](../src/client/java/dev/wildcraft/client/art/ArtFeedbackClient.java)；12 段原创短音，服务端确认事件后播放 |

## 验证、后续与原件保护

- [核心接入](../docs/CORE-ART-INTEGRATION.md) / [核心验收](../docs/CORE-ART-VERIFICATION.md)；[第二批接入](../docs/SECOND-ART-INTEGRATION.md) / [第二批验收](../docs/SECOND-ART-VERIFICATION.md)；历史玩法 [dev.17验收](../docs/P10.2-VERIFICATION.md)。
- 原稿、exports、references、review、adoption、manifest与17份历史审稿ZIP保持原字节。台账JSON关联来源清单和当前运行入口SHA-256；运行入口是便于定位的关键文件，完整运行资源仍以源码与导入工具为准。
- A09-C、A07-A08-C、A13-C分别叠加侧腰布局、衔接和混音参数；不删除核心A07–A13原件。F03-04复用原版粒子，不将原版游戏文件当原创源资产分发。
- dev.17仅调整侧装轮显示并修复弹簧配方，原美术未修改；底部部件仍可能穿地，见[机械修订](production-docs/P10.2-MECHANICAL-ART-REPAIR.md)。
- [B6风与天气](production-docs/B6-WIND-ART-BRIEF.md)正式美术尚未制作或采用，游戏当前使用临时字体箭头。
- 图形回归不替代人工长期生存、第三方模型兼容或最终混音听审；当前用户不考虑多人专项。

在项目根运行`python3 tools/verify-adopted-assets.py`，校验43项当前状态、CSV/JSON一致性、原件和运行文件。更新玩法或资源后需按真实结果更新台账，不能自动重设历史原件校验。

当前品牌接入与验收见 [dev.18品牌验证](../docs/BRAND-ICONS-VERIFICATION.md)，报告保留发布前状态；新版公开下载以 [ASSET-DOWNLOADS](../docs/ASSET-DOWNLOADS.md) 为准。
