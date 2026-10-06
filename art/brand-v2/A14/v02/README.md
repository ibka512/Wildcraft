# A14 品牌图标 v02：精细展示与简单识别

2026-10-06，用户提供两张品牌图，并确认“好的，就按你说的做”。本批按已确认方向整理双版本、清理简单版暗斑和小尺寸导出，接入本机dev.18。原A14 v1保留，不增加采用编号或玩法。

| 文件 | 用途 |
| --- | --- |
| source/wildcraft-brand-detailed-original.png | 1254×1254精细原图，逐字节归档 |
| source/wildcraft-brand-simple-original.png | 1254×1254简单原图，逐字节归档 |
| source/wildcraft-brand-simple-cleaned.png | imagegen清理简单版两侧暗斑后的派生主稿 |
| exports/wildcraft-brand-detailed-512.png | README项目展示；原图保留供以后发布页与宣传使用 |
| exports/wildcraft-brand-simple-{20,32,64,128}.png | 原生小尺寸导出，保留透明背景 |

![简单版128](exports/wildcraft-brand-simple-128.png)

![精细版](exports/wildcraft-brand-detailed-512.png)

正式Mod元数据继续引用`assets/wildcraft/icon.png`，内容采用128×128简单版。设置页引用`assets/wildcraft/icon-20.png`，20×20原生绘制，减少大图运行时缩小时的细节损失。精细版用于项目展示，不替换滑翔伞物品图标。旧核心源稿与历史包不覆写。

[生成提示词与工具记录](generation.json) / [原件与导出SHA-256](manifest.json)。原图是PNG，本批不声称交付可编辑分层Aseprite或矢量源；生成主稿也为PNG。清理派生图按授权范围处理，不冒称用户另行审阅过新稿。

macOS在项目根运行`./tools/export-brand-icons.sh`，从保留主稿重新导出。普通Gradle构建无需imagegen、sips或图形编辑器，运行文件已在src中。修改主稿后需据实更新manifest和当前采用台账，旧源清单保持历史身份。

游戏验证与安装包见[品牌接入验证](../../../../docs/BRAND-ICONS-VERIFICATION.md)。当前为本机dev.18，GitHub公开main/Release仍dev.17，本批未自动发布。
