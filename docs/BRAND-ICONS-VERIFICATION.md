# Wildcraft 品牌图标接入与验证

2026-10-06，本机 **dev.18 / Minecraft26.3**，分支`feature/brand-icons`。公开main与Release仍为dev.17，本批没有自动发布。本次只更新品牌资产与设置页图标引用，不改游戏规则、数据格式、依赖或滑翔伞物品贴图。

## 已完成

- 精细版保留1254×1254原图，用512×512导出放入README项目介绍；可用于后续发布展示。
- 简单版原图原样保留；按确认方案使用内置imagegen清理两侧暗斑，并从派生主稿导出20/32/64/128 PNG。图案和透明背景保留，未声称拥有分层Aseprite源。
- Mod元数据继续使用`assets/wildcraft/icon.png`，对应128版本；设置页改用`icon-20.png`，按20×20完整资源绘制。
- A14当前v02，核心编号仍14、总计43；原v1与第二批30份原交付保留。[原件/导出/生成提示词](../art/brand-v2/A14/v02/README.md)和[当前台账](../art/ADOPTED-ASSETS.md)提供来源与状态。

## 实际执行

先`./dev.sh --no-configuration-cache runDatagen`，再`./dev.sh -PacceptMinecraftEula=true --no-configuration-cache build runCoreArtClientTest`。本机已有Minecraft EULA授权，其他操作者自行接受。

资源生成8秒成功，无生成资源变化。构建及客户端专项44秒成功，83项服务端全部通过；纯精力/温度/风检查通过。客户端仅一类CoreArtClientSmokeTest，核对标准/细手臂、第一人称伞网格、正常与紧凑设置页、真实背负引用。新图标已在实际设置页截图中检查，使用窗口854×480、GUI缩放2；不冒称所有缩放或完整21类回归已经重跑。

[真实日志/XML/截图](../development-assets/verification/brand-v2/README.md)。简单版20/32/64/128和精细512均有透明通道；原图与源文件哈希记录在manifest，当前台账校验覆盖新品牌原件和导出。

## 成品与限制

[dev.18正式Mod](../development-assets/wildcraft-0.1.0-dev.18+mc26.3.jar)，SHA-256：`daaa39c4395511671a8ae9d1dcb5f0eca0ea399fe2d9b0ce4ef1ff47f15a869c`。源码JAR另存development-assets；生产JAR不含测试类，图标字节与导出匹配。旧dev.17正式JAR仍`e174921cb174e7138001f7c365e82cc426e775c49b30ea3876ee9fe7935a5e35`，旧标签、公开交接快照和源稿不覆写。

当前仍为单人开发版，人工生存平衡、B6正式风提示、底部机械修订和兼容仍按原计划；多人暂缓。未运行本批独立服务端或重新测试Mod列表第三方界面，未进行GitHub同步。本轮只完成已确认品牌整理与接入。
