# dev.17公开交接准备验证

2026-10-06。本次只更新公开交接、当前采用台账和CI资产校验步骤；游戏源码、运行资源、依赖、原始采用源稿与dev.17正式JAR未改变，不声称重新运行全部游戏检查。

- 当前采用台账43个唯一编号：核心14、第二批29；第二批30份原交付版本，F05-10当前v02。CSV/JSON一致性通过。
- 55份核心原件及第二批30份manifest共1,767条文件记录逐一校验通过；48份原样复制的运行资源与原来源哈希匹配。检查入口`python3 tools/verify-adopted-assets.py`，不改写原件。
- 11份当前入口文档283个相对链接目标已检查；新增公开快照清单在生成后另行检查。原始制作文档的旧本机路径保留历史身份，公共入口不依赖本地网页服务。
- 对原公开main到当前的2,786个变更路径进行常见凭据模式检查，未发现匹配；39份顶层/历史审稿/原始ZIP的目录未发现账号、协议接受文件、游戏缓存、个人世界、AppleDouble等禁止条目。
- 正式JAR SHA-256仍为`e174921cb174e7138001f7c365e82cc426e775c49b30ea3876ee9fe7935a5e35`，不含`dev/wildcraft/test/`类。当前游戏验收沿用[P10.2](../docs/P10.2-VERIFICATION.md)的真实83服务端/21类客户端、实际缩放专项与无测试Mod服务端证据。
- GitHub原main提交`55f72ee7105decd9dca8a424e19b96f0c38fb2e7`为当前分支祖先，使用快进同步，保留全部旧标签和Release。

本文件记录本机准备检查，GitHub远程构建及发布结果以[Actions](https://github.com/ibka512/Wildcraft/actions)与[交接Release](https://github.com/ibka512/Wildcraft/releases/tag/handoff-dev17-2026-10-06)为准，不预写尚未执行的远程成功。下载文件字节校验在Release的manifest/SHA清单中提供；全仓库文件校验绑定本次交接标签，旧阶段manifest不覆盖。
