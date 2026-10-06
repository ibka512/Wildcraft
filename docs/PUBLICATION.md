# GitHub 公开交接记录

## 2026-10-06：dev.18 当前公开交接

用户明确“更新github”，本次交接为 [handoff-dev18-2026-10-06](https://github.com/ibka512/Wildcraft/releases/tag/handoff-dev18-2026-10-06)，开发预览。main 快进纳入品牌功能提交 d3a0033 与当前交接文档；游戏标签 v0.1.0-dev.18+mc26.3 保持固定。

新版包含双版本品牌原图、清理来源、五份导出、Mod 128px 与设置页原生 20px、正式 dev.18 JAR/源码 JAR、完整工程 ZIP 及校验。A14 更新到 v02，总采用编号仍 43，旧 A14、dev.17 及大包保留。

macOS 品牌阶段已通过数据生成、构建、83 项服务端与一类核心美术客户端专项；本次仅更新公开文档和资产索引，未重复完整 21 类客户端验证。Linux 远程结果见 [Actions](https://github.com/ibka512/Wildcraft/actions)；不代表 Linux 图形验收。当前完整快照以 PUBLIC-ASSET-MANIFEST-dev18 及新交接标签核对，旧冻结清单不覆盖。人工长期生存、B6、底部机械修订仍待完成；多人工作暂缓。

## 历史：2026-10-06 dev.17公开交接

本轮用户明确授权“上传github，更新readme，更新github上的已经被采用的游戏资产数据，更新交接文档”。公开仓库默认分支仍为[main](https://github.com/ibka512/Wildcraft)，当前交接为[handoff-dev17-2026-10-06](https://github.com/ibka512/Wildcraft/releases/tag/handoff-dev17-2026-10-06)，标记开发预览。本次将dev.8–dev.17之间的源码、资源、测试、原稿和证据纳入公开历史；不改游戏玩法、正式JAR、依赖、持久化格式或许可。

- README重新组织为安装、操作、玩法、美术、构建、验证和接手入口；AI-HANDOFF、下一阶段计划和当前文档已更新到dev.17。
- [采用台账](../art/ADOPTED-ASSETS.md)/CSV/JSON登记核心14＋第二批29当前编号、30份第二批版本；F05-10当前v02。原件及历史采用/校验不改写。
- 本机只读校验：43个当前编号、CSV/JSON一致性、30份原交付manifest、1,767条原件记录、48份原样运行资源全部匹配。dev.17正式JAR仍为`e174921cb174e7138001f7c365e82cc426e775c49b30ea3876ee9fe7935a5e35`。
- [下载说明](ASSET-DOWNLOADS.md)提供当前完整工程、原始第二批ZIP和校验，第一批历史大包继续沿用dev.7已公开链接。当前公开文件快照独立命名PUBLIC-ASSET-MANIFEST-dev17，旧清单不覆写。
- GitHub Actions在main更新后重新执行，当前运行事实以[Actions](https://github.com/ibka512/Wildcraft/actions)为准。本次文档发布不将历史macOS83项/21类测试写成重新执行，也不将Linux构建写成图形验收。

当前仍待人工长期单人生存/定向平衡、B6正式美术、底部机械修订、第三方兼容和候选版检查；多人R2/P10.1暂缓。本次开发交接授权不覆盖未来稳定发布、生产依赖或真实数据迁移。

## 历史：2026-10-03 dev.5/dev.7

以下保留当时的发布事实与验证；旧“下一阶段”和“未发布”只描述当时状态，当前入口以上文和README为准。


> 当前公开主分支已更新到 dev.7，最新交接为 [handoff-dev7-2026-10-03](https://github.com/ibka512/Wildcraft/releases/tag/handoff-dev7-2026-10-03)。下面初次发布内容保留为 dev.5 历史；本次记录见 [迁移与交接](MIGRATION-HANDOFF-2026-10-03.md)。

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

## dev.7 迁移与第二次公开交接

用户本轮明确授权迁移、更新仓库与重写 README。已将 dev.6 单人林克时间、dev.7 核心美术和本次可运行构建入口纳入公开交接，保留所有旧里程碑。最新后续计划、AI 接手状态、可编辑源稿、制作资料、成品包和本机迁移证据均已整理；完整美术大文件使用新 Release 资产分发，校验与来源见 ASSET-DOWNLOADS。初次 dev.5 发布及其 CI 事实不追溯改写。
