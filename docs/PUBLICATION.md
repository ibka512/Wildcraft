# GitHub 公开交接记录

日期：2026-10-03（Asia/Shanghai）。项目所有者明确授权建立公开 GitHub 仓库、上传接手说明、源码及全部当前开发资产，并完善 README。本次不改变游戏玩法、技术栈、保存格式或许可证。

## 仓库与入口

- 公开仓库：[ibka512/Wildcraft](https://github.com/ibka512/Wildcraft)，默认分支 `main`。
- 接手：[README](../README.md) → [AGENTS](../AGENTS.md) → [AI-HANDOFF](AI-HANDOFF.md) → [路线](ROADMAP.md) 与当前阶段规格。
- 完整资产：[development-assets](../development-assets/README.md)，原始交付及诊断副本共 150 个文件，另有索引/清单；全部源码和美术在根目录 `src/`、`art/`。
- 完整交接包：[handoff-2026-10-03](https://github.com/ibka512/Wildcraft/releases/tag/handoff-2026-10-03)，标记为开发交接预览，不是稳定版。

## 保留的状态

游戏仍为 `0.1.0-dev.5` / Minecraft 26.3，功能完成到 P3.1，基线提交 `18602f8e7d80693731d795046d6b9eb47203b9c2`。从基线到本次交接，`src/`、`art/`、`build.gradle`、`gradle.properties`、`dev.sh` 没有变化。R1 仍为隔离实验，正式 P3.2 尚未实现。

原有完整 Git 提交历史和 `v0.1.0-dev.1+mc26.3` 至 `v0.1.0-dev.5+mc26.3` 标签保留；主分支通过快进纳入已验证的 P3.1。新增交接文档、原始资产镜像、GitHub 友好链接与只读 CI 权限。归档设置 `-text -whitespace`，避免不同系统换行处理破坏原始日志/校验。

## 实际公开验证

1. GitHub API 确認 `isPrivate=false`、默认分支 `main`。
2. 无认证下载 README、AGENTS、AI-HANDOFF、开发手册、资产清单、实际截图及公共代码，内容与本地逐字节相同。
3. 150 个原始资产副本逐字节匹配来源；151 个索引/资产记录与 Git 存储字节的 SHA-256 匹配。原输入 ZIP 匹配接收时的来源哈希。
4. 224 个主说明/当前文档相对链接目标存在。原始导出报告与 ZIP 保留当时布局；当前可读报告已适配仓库路径，区别见资产索引。
5. 628 个归档文本条目和历史 Git 211 个源文件 blob 的常见密钥/令牌模式扫描未发现匹配；未归档 Minecraft 游戏/反编译源码、缓存、个人世界、账号凭据或运行 EULA。
6. [首次 GitHub Actions 构建](https://github.com/ibka512/Wildcraft/actions/runs/37034467115) 在公开 Linux runner 成功：Wrapper 校验、JDK 25、资源生成一致性、编译、数值、无图形 GameTest 及构建产物上传。检查提交为 `be08d475d0c86227c5395d071b5b42fe939d6aab`；后续仅文档/交接提交，以 Actions 当前结果为准。

远程 Linux 构建不等于 Linux 图形客户端、长期多人或手感验收；这些仍按原本范围列为未完成。本机 P3.1 的 25 项 GameTest、7 组成品客户端与双客户端证据保持原样，不追溯改写旧报告。

## 许可与后续工作

原 [LICENSE](../LICENSE) 的所有权利保留和 [NOTICE](../NOTICE.md) 来源说明继续使用，没有擅自选择 MIT 等开源许可证。GitHub 公开可见是本次明确授权的结果，不自动授权后续发布、迁移数据或新增生产依赖。

下一计划阶段为 P3.2 单人林克时间；具体接手步骤、必须保留的用户决定、时间/精力/网络边界和验收在 AI-HANDOFF 中。请先取得下一项工作的实际目标，避免把整个路线表当成自动执行指令。

## 后续本机阶段（未发布）

P3.2 单人林克时间在 feature/focus-singleplayer 完成本机 dev.6 验收，见 [P3.2](P3.2-VERIFICATION.md)。本轮没有更新公开 main、GitHub Release 或远程 CI；本页以上记录仍描述初次公开交接 dev.5。
