# Wildcraft 美术源文件

已完成到 P3.2 的重设计任务、现有文件尺寸、代码绘制边界与截图参考见 [美术资产整理](ASSET-REDESIGN-PLAN.md)，可分配清单见 [CSV](asset-redesign.csv)。这是制作计划，尚未替换现有资源。

`paraglider.svg` 是原创物品图标的可编辑源稿，按 32 × 32 像素制作。游戏贴图位于 `src/main/resources/assets/wildcraft/textures/item/paraglider.png`。

展开的伞由 `ParagliderLayer.geometry()` 中的模型几何定义，可在不改变玩法或保存格式的情况下替换外观；纹理为 `textures/entity/paraglider.png`，尺寸 128 × 64，布面在上半部、木架在下半部。专用装备位的空槽图标为 16 × 16 的 `textures/gui/sprites/container/slot/paraglider.png`。

修改后检查物品栏、第一人称手持物品收起、第三人称双手持伞、内外视角和其他玩家的伞。当前资源用于开发验证，未使用塞尔达的贴图或模型。

林克时间的原创 GLSL、生成后处理和音效引用见 [focus](focus/README.md)。
