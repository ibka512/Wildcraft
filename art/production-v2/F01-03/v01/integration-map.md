# F01-03 后续接入映射

这是接入规格，当前没有修改生产资源或代码。

1. 料理HUD仍使用已同步CookingEffects.VIEW（MealView），保暖/耐热/恢复分别映射warmth/warmSeconds、cooling/coolSeconds、recovery/recoverySeconds。图标仅是外观，不新增原版药水效果、物品ID或服务器数据结构。
2. 当前MealHud.java从TemperatureHud.rowY()+13开始绘文字，11px行距。新布局建议采用+14起点、16px图标和20px行距，按真实屏幕与心界限选择纵排或紧凑。锚点使用当前StaminaHud/TemperatureHud接口，不能把审稿示例的固定心排数写进生产代码。
3. 候选运行贴图：原生16px的warmth-meal、cooling-meal、stamina-recovery标准PNG。32px是源稿/放大参考；不按I/II切换分辨率。保留独立PNG即可，不增加图集加载器。实际运行路径待接入时依项目约定确定。
4. 游戏原版字体绘名称、I/II和m:ss，实时测量文字宽度；本稿固定右列预留34px，英文Recovery II及10:00在浏览器模拟中检查过。当前恢复标签为“恢复”，稿中“精力恢复”是建议文案，接入时修改资源生成语言源，不能手改src/main/generated。
5. 等级与时间持续取实际数据；0隐藏收拢；暂停/离线不新增墙钟消耗，林克时间不改料理秒数。沿用死亡、旁观者、隐藏HUD等条件。低于或等于10s仅时间变色；不加独立声音、闪烁或新设置项。
6. 温度仍在料理上方，F02-01温度组件采用后再统一两组HUD的最终高度。不以温度待审稿作正式资源。
7. 正式接入后执行Wrapper资源生成与构建，并实际检查多排心/吸收心、精力隐藏、3效果并存、中英文、GUI缩放、0结束、暂停、专注、F1隐藏、死亡与旁观者。极端视窗不足时不得把UI压到心、准星或经验/快捷栏上；具体支持边界实机核对。

资源来源及哈希：[reused-assets.json](references/reused-assets.json)。坐标/颜色/状态：[meal-hud-layout.json](exports/meal-hud-layout.json)。
