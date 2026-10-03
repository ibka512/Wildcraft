# Wildcraft 已完成阶段美术资产整理

整理日期：2026-10-03。依据本地 dev.6（P0–P3.2）源码、资源和验收截图，代码基线 `cee8db7`。本文是美术制作清单和接入说明；优先级、交付尺寸和风格建议尚未成为新的玩法决定。本次没有替换游戏资源或实现动画。

## 制作范围与结论

当前已完成精力、攀爬、滑翔伞、背负装备、左上角红心/精力和单人林克时间。美术工作集中在滑翔伞、精力 HUD、角色动作/背负、林克时间反馈，以及模组图标。

现有自有 PNG 共 **5 个**：模组图标、测试核心图标、滑翔伞物品图标、展开伞纹理、伞槽空位图标。展开伞模型写在 Java 中；精力条、装备槽框和专注准星由代码绘制；专注滤镜由着色器生成。它们需要设计稿、模型或参数规格，不能都按“更换 PNG”交付。

温度、料理、Fuse、能源、八类机械、制造机、风与天气尚未实现，另列后续阶段。此次不为这些系统准备正式资产，不把 R0 实验物体算作正式机械。

## 建议制作清单

“首批”是建议最先定稿；“重点”可以沿用当前版本后逐项完善；“后续”需对应的渲染/动画开发；“条件项”只有统一重绘时才制作。下列 **16 项是任务条目，不是 16 个贴图文件**。

| 编号 | 资产/设计任务 | 当前状态 | 建议优先级 | 制作交付与接入 |
| --- | --- | --- | --- | --- |
| A01 | 左上角精力 HUD | 文字 + 短矩形条，代码绘制；正常绿色、低精力琥珀色 | 首批 | 布局源稿、尺寸/间距/配色规格、满/消耗/恢复/低/空状态稿；沿用短条可改绘制，采用贴图则需接入；环形为可选方向，未定稿 |
| A02 | 红心视觉统一 | 只移动原版红心坐标，原版状态仍完整 | 条件项 | 默认继续用原版；若重绘，必须覆盖满/半/空、闪烁、中毒、凋零、冰冻、吸收、极限模式及其适用组合，先列原版状态矩阵再做 sprite |
| A03 | 展开滑翔伞模型 | 三片薄盒伞面和框架，几何在 Java 中，没有 Blockbench 源文件 | 首批 | 可编辑模型（建议 `.bbmodel`）、正侧俯视稿、部件命名/挂点/UV 图；由开发者导入或转写当前模型层，模型文件不能直接替代 Java |
| A04 | 展开滑翔伞纹理 | 128×64 PNG，米色布面/绿色条纹/木架 | 首批 | 与 A03 同步做 UV 和材质细节；首版建议保留 128×64，提供分层源稿 + PNG；改图集尺寸时同步模型，不先单独放大贴图 |
| A05 | 滑翔伞物品图标 | 32×32 PNG，有原创 SVG 源稿；物品采用平面模型 | 首批 | 重绘 32×32 透明图标与源稿；缩到 16×16 的槽内仍可识别；与展开模型同一造型/纹样，先做平面图标即可 |
| A06 | 滑翔伞装备位 | 16×16 灰色空槽图标；18×18 槽框由代码画出 | 首批 | 16×16 透明空槽图标与源稿；空/有物品/悬停三种布局稿。槽框可以继续用现有原版风格，定制框需开发接入；不是新增装备位 |
| A07 | 滑翔双手与姿态 | 第三人称固定抬臂；第一人称隐藏手与物品，没有专用抓伞手部显示 | 重点 | 双手握点、标准/纤细手臂、身体悬挂姿态稿；第一人称抓握视图与遮挡范围稿；可附关键帧，需客户端渲染开发，不是新的玩家皮肤 |
| A08 | 攀爬动作 | 原版玩家外观，没有专用手脚攀爬动画 | 重点 | 抓墙静止、上/下/横移、放手、登顶的姿态/关键帧稿；按已实现动作制作，不加攀爬跳跃/新动作规则；表现仍需开发接入 |
| A09 | 背负摆放与挂点 | 原版近战/盾/弓模型直接挂身体，固定缩放；披风隐藏三类，鞘翅隐藏中央盾/弓 | 重点 | 正背侧视摆放稿、挂点/旋转/缩放表、持物互斥图；覆盖剑/斧/盾/弓/弩与蹲/游泳/滑翔姿态。保留原版物品外观，重点处理层次和穿插 |
| A10 | 收起/取出过渡 | 手和背瞬时切换，未实现短动画 | 后续 | 简短关键帧/时序稿，记录中断后的视觉归位；先设计，再在角色表现阶段接入。没有新背包容量或额外真实物品副本 |
| A11 | 林克时间画面效果 | 代码实现轻去饱和/冷色/暗角/程序化轮廓；有关闭/弱/标准/强 | 重点 | 同场景前后对照、强度/色调/边缘参数稿、暗角及纹理开关稿；修改着色器和生成器，不将整个滤镜交付成一张覆盖图 |
| A12 | 林克时间准星/低精力提示 | 四段细线；低精力变琥珀色；可选微弱边缘轻闪 | 重点 | 中心安全区、线长/间距/颜色/低精力状态稿；可选 sprite，但当前直接画线。先保留静态可辨识提示，轻闪和 FOV 默认关闭 |
| A13 | 林克时间进入/退出声音 | 引用原版紫水晶音效，用不同音高 | 建议完善 | 两个原创短音效、响度和时长说明、可编辑音频源稿及 OGG；需要音效注册接入。低精力提示音可后补，暂不新增技能音乐 |
| A14 | Wildcraft 模组图标 | 128×128，使用测试核心图案；有核心像素源数据，无独立品牌源稿 | 建议完善 | 原创模组标识源稿、128×128 PNG；优先保证小尺寸辨识，可另出高分辨率展示版，保持与游戏资产一致 |
| A15 | 表现设置页 | F8 打开原版按钮界面；无自有背景/按钮贴图 | 后续 | 仅在确定统一 UI 风格后做页面稿和所需小图标；当前功能可以继续使用原版按钮，不需要重绘整套菜单 |
| A16 | P0 测试核心 | 16×16 PNG + 像素 JSON，仅验证工程 | 保留 | 保留开发测试图标及原源数据，不把它当正式能源核心重做；A14 与它分离即可 |

## 第一批怎么交付

建议先做 **A01 + A03–A06，共 5 项**。顺序：一张左上角 HUD 布局稿 → 一套伞的造型三视图 → 伞模型与 UV/纹理 → 物品图标与空槽图标 → 放进游戏验证比例。这批能统一最常见界面和核心探索道具，且不用先重做原版武器。

随后做 A07–A09，让攀爬、滑翔和背负有协调的角色表现；再做 A11–A14，完善专注反馈、声音和品牌。A10、A15 按后续角色/音画阶段接入，A02 暂用原版，A16 保留。

**精力形状仍需设计选择。** 当前确定的是左上角、红心下方和精力数值。若希望采用荒野之息式圆环，需要先看圆环/短条对照稿，确认分段、上限增长及数字位置，再替换绘制；此次没有将短条改成圆环。颜色和视觉参考可借鉴原作的阅读层次，图案和造型按 Wildcraft 自己的风格制作。

建议整体方向：保持 Minecraft 像素边缘和方块比例，滑翔伞采用清楚的木架/布面分区，HUD 用稳定绿色与琥珀色提示，专注画面保留清楚的瞄准中心。该方向用于出稿，尚未锁定配色或纹样；避免把高分辨率写实道具与原版武器放在同一套角色表现中。

## 当前资产与源码定位

下列是现有路径；未来新命名/尺寸为制作建议，不视作已经存在。

| 用途 | 当前资源/可编辑来源 | 接入入口 |
| --- | --- | --- |
| 模组图标 | [icon.png](../src/main/resources/assets/wildcraft/icon.png)（128×128）；图案来源 [核心像素源数据](source/test_core.json) | `src/main/resources/fabric.mod.json` |
| 测试核心 | [test_core.png](../src/main/resources/assets/wildcraft/textures/item/test_core.png)（16×16）/ [JSON 源数据](source/test_core.json) | 自动生成的 item/model JSON |
| 伞物品图标 | [paraglider.svg](paraglider.svg) / [物品 PNG](../src/main/resources/assets/wildcraft/textures/item/paraglider.png)（32×32） | `WildcraftModelProvider`，自动生成 item/model JSON |
| 伞实体纹理/模型 | [纹理 PNG](../src/main/resources/assets/wildcraft/textures/entity/paraglider.png)（128×64） | [ParagliderLayer.java](../src/client/java/dev/wildcraft/client/render/ParagliderLayer.java) 的 `geometry()`，没有独立实体模型 JSON |
| 伞槽 | [空槽 PNG](../src/main/resources/assets/wildcraft/textures/gui/sprites/container/slot/paraglider.png)（16×16） | `GliderSlotScreenMixin`、`GliderCreativeScreenMixin` 的框绘制 |
| 精力/红心 | 当前没有 Wildcraft HUD PNG | [StaminaHud.java](../src/client/java/dev/wildcraft/client/hud/StaminaHud.java)、[UpperLeftHeartsMixin.java](../src/client/java/dev/wildcraft/mixin/client/UpperLeftHeartsMixin.java) |
| 滑翔动作/第一人称 | 没有动作源文件或专用手部模型 | [ParagliderPoseMixin.java](../src/client/java/dev/wildcraft/mixin/client/ParagliderPoseMixin.java)、[ParagliderHandsMixin.java](../src/client/java/dev/wildcraft/mixin/client/ParagliderHandsMixin.java) |
| 背负 | 没有新的剑/盾/弓 PNG 或独立背负模型 | [BackEquipmentLayer.java](../src/client/java/dev/wildcraft/client/render/BackEquipmentLayer.java)，调用真实物品模型 |
| 林克时间画面 | [focus.fsh](../src/client/resources/assets/wildcraft/shaders/post/focus.fsh)、[focus_copy.fsh](../src/client/resources/assets/wildcraft/shaders/post/focus_copy.fsh)、[focus_pulse.fsh](../src/client/resources/assets/wildcraft/shaders/post/focus_pulse.fsh) | `FocusEffectProvider` 生成 13 个后处理 JSON；不要直接重画/手改生成文件 |
| 专注准星/声音/设置 | 当前没有专用 PNG、音频或设置背景 | [FocusClient.java](../src/client/java/dev/wildcraft/client/focus/FocusClient.java)、`FocusSettingsScreen`、`FocusOptions` |

制作时保留分层/模型/动画源文件，再导出游戏资源。建议源稿目录：`art/hud/`、`art/paraglider/`、`art/character/`、`art/focus/`、`art/audio/`、`art/branding/`；除已有 `art/focus/` 外，这些是建议目录。模型源稿说明坐标、单位和握点；PNG 明确尺寸、透明与像素边缘；程序特效交付参数和效果稿；音频交付源稿和原创来源说明。

## 截图参考与美术验收

这些是历史开发截图，不是最终宣传图；测试世界、玩家皮肤和背景无需照着重做。

| 参考 | 观察重点 |
| --- | --- |
| [左上角红心/精力](../development-assets/verification/p31/gliding-upper-left-hud.png) | 当前左上角布局，底部经验保留 |
| [多排红心](../development-assets/verification/p31/upper-left-multiple-hearts.png) | 精力随心排向下避让，不能固定覆盖第二排 |
| [伞槽](../development-assets/verification/p3/paraglider-equipment-slot-zh.png) | 伞物品/装备槽比例；这是 P3 历史截图，左下角旧 HUD 不作为布局依据 |
| [其他玩家展开伞](../development-assets/verification/p31/two-client-gliding.png) | 伞面轮廓、持握比例、背负同屏 |
| [背负](../development-assets/verification/p31/back-equipment-third-person.png) | 原版剑盾弓层次、当前体积与交叠 |
| [攀爬](../development-assets/verification/p2/climbing-wall-zh.png) | 尚无专用抓墙姿态；这是 P2 历史截图，旧 HUD 不作为布局依据 |
| [普通画面](../development-assets/verification/p32/p32-color-normal.png) / [林克时间](../development-assets/verification/p32/p32-focus-first-person.png) | 冷色、去饱和、准星及瞄准中心；测试采样不是严格相同帧，不直接作颜色数值测量 |
| [第三人称专注](../development-assets/verification/p32/p32-focus-third-person.png) | 第三人称可读性、拉弓与背负 |
| [设置页](../development-assets/verification/p32/p32-focus-settings.png) | 原版按钮与开关，文字是功能而非需要绘成图的资产 |

换资源后验证：GUI 多档缩放、中英文、低/满/恢复精力、大上限与多排/吸收红心；伞物品栏/装备槽/丢弃、伞上下面/内外侧、两种手臂宽度、第一/第三人称和其他玩家；背负与持物互斥、蹲游滑翔、披风/鞘翅策略；专注关闭/各强度/低精力、明暗环境、瞄准中心。美术过渡不改变取用物品收伞或服务端状态，也不清除原版染色、盾图案、附魔光效。

**不需要本批重做**：原版字体、剑斧盾弓弩、盔甲、玩家皮肤、方块环境、快捷栏/经验条、所有原版菜单；红心默认保留。剑鞘、箭袋、收纳伞包、攀爬工具、专用服装和宣传封面未列为必需项，若将来要做另定范围。新美术接入后再跑对应功能/图形回归，当前整理文档本身不构成新资源验收。
