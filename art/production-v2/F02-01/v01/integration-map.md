# 后续接入说明（本轮未实施）

## 读取现有状态

`TemperatureHud.displayedBand()` 与 `TemperatureRules.STATES` 的索引顺序沿用极冷 0 至极热 6。视觉刻度从上到下使用 `6-index`；不要改动真实状态数组顺序。温度采样、平滑、迟滞和服务器同步继续使用现有实现。

## 图标与程序刻度

常规显示可引用本项 `exports/ambient-temperature-color-16.png`，较大资源可保留 32×32 原生版本。拟接入资源位置为 `assets/wildcraft/textures/gui/ambient_temperature_16.png` 和 `_32.png`，本轮未复制到运行资源目录。仅在审核后的接入步骤中确定实际命名。

替换现有 TemperatureHud 的横向七格为本项温度计与竖向七格；使用原版 GUI 绘制矩形、亮框、标记和翻译文字。没有新增七档 PNG 的必要。现有翻译键继续使用 `hud.wildcraft.temperature.<state>`。

## 锚点与文字

继续调用 `TemperatureHud.rowY()`：`StaminaHud.heartBottom()+(showStamina?29:5)`。图标与背景起点为 rowY−3；料理仍从 rowY+14 开始。无需重写已采用料理排布，也无需新增垂直占位。

本稿固定背景宽 104px、文字 x=44。接入时以真实 `c.font.width(label)` 检查，长翻译可按实测文字宽度延伸背景，但不能侵入准星或预留区域。当前中英文验证使用浏览器近似字体，不能替代原版字体检查。使用实际 `heartBottom()`，不要把审稿示意红心行数公式复制进游戏。

## 边缘色调与显示边界

保留 `TemperatureOptions.edgeTint`、`FocusTime.active` 以及真实连续 `displayed` 值的现有计算：左右各 4px，alpha=round(min(14,abs(displayed)*5)/(对应料理等级+1))。本稿示例值仅用于预览。无需新增 Shader、全屏滤镜、闪烁或 FOV 变化。

沿用现有死亡、旁观、隐藏 HUD、切换世界与暂停处理。实机应保证新组件完整可见：检查图标顶底与背景宽度，不只判断旧文字 10px 高度。具体窗口/多排红心不足时遵守已采用布局的空间规则，不覆盖红心、准星、经验或快捷栏。

## 实机待验证

审核后接入阶段需检查原版字体、GUI 各档缩放、各七档过渡、专注开启/结束、精力自动显隐、多排红心/吸收心、料理到期/匹配效果、HUD 隐藏及重新进入世界。该清单未在本轮游戏中执行；技术检查不等于视觉采用。
