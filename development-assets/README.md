# 全部开发资产与历史证据

> 2026-10-06当前公开交接：dev.17 / handoff-dev17-2026-10-06。下方按时间记录各阶段，当时“未上传/公开dev.7”仅描述历史。本次纳入dev.8–dev.17源码、包、原稿与证据，下载见[ASSET-DOWNLOADS](../docs/ASSET-DOWNLOADS.md)。当前全仓库文件校验见[PUBLIC-ASSET-MANIFEST-dev17](PUBLIC-ASSET-MANIFEST-dev17.json)和[SHA256SUMS](PUBLIC-ASSET-SHA256SUMS-dev17.txt)，以交接标签复核。旧PUBLIC-ASSET-MANIFEST及阶段清单不覆写。


归档日期：2026-10-03。这里镜像原有交付文件，保存其原始字节、哈希和当时的验证状态，不覆盖原版本。当前源码在仓库根目录；最新交接见 [AI-HANDOFF](../docs/AI-HANDOFF.md)。

## 可运行包与源码快照

| 阶段 | 安装 JAR | 原始源码 ZIP |
| --- | --- | --- |
| P0 | [dev.1](wildcraft-0.1.0-dev.1+mc26.3.jar) | [P0](Wildcraft-P0-source.zip) |
| R0 / P1 | [dev.2](wildcraft-0.1.0-dev.2+mc26.3.jar) | [P1](Wildcraft-P1-source.zip) |
| P2 | [dev.3](wildcraft-0.1.0-dev.3+mc26.3.jar) | [P2](Wildcraft-P2-source.zip) |
| P3 | [dev.4](wildcraft-0.1.0-dev.4+mc26.3.jar) | [P3](Wildcraft-P3-source.zip) |
| R1 | 沿用 P3 正式 JAR，研究不进运行包 | [R1](Wildcraft-R1-source.zip) |
| P3.1 | [dev.5，已公开](wildcraft-0.1.0-dev.5+mc26.3.jar) | [P3.1](Wildcraft-P3.1-source.zip) |
| P3.2 | [dev.6，历史阶段](wildcraft-0.1.0-dev.6+mc26.3.jar) | [P3.2](Wildcraft-P3.2-source.zip) |
| 核心美术 v1 | [dev.7，已公开交接](wildcraft-0.1.0-dev.7+mc26.3.jar) | [核心美术](Wildcraft-核心美术-v1-source.zip) |

| P4A 环境温度 | [dev.8，本机未上传](wildcraft-0.1.0-dev.8+mc26.3.jar) | [P4A](Wildcraft-P4A-source.zip) |

所有游戏包使用 Minecraft 26.3 和对应 Loader/Fabric API。旧源码 ZIP 是当时快照，不含本次新增交接说明；最新源码及完整历史用主仓库获取。不能把研究模组当正式功能安装。

## 报告、规划与美术

- 原始导出报告：[R1](Wildcraft-R1-验证报告.md)、[P3.1](Wildcraft-P3.1-验证报告.md)。按当前仓库布局阅读的版本：[R1 结论](../docs/R1-FINDINGS.md)、[P3.1 验收](../docs/P3.1-VERIFICATION.md)。
- 原始规划导出：[Markdown](Wildcraft-开发规划-v2.md)、[ZIP](Wildcraft-开发规划-v2.zip) 与 [当时副本](Wildcraft-开发规划-v2_副本.zip)；当前执行状态以 [根目录规划](../docs/DEVELOPMENT-PLAN-V2.md) 为准。
- 用户原始输入：[开发文档.zip](inputs/开发文档.zip)；已整理的四份 Markdown 和接收哈希在 [design-inputs](../docs/design-inputs/2026-10-02/SOURCES.json)。ZIP 原样保留，包括输入时的文件名编码和 macOS 元数据；工作时优先读整理后的 Markdown。
- 可编辑原创源稿：[art](../art/README.md)；实际贴图、配方、语言、模型和研究着色器在 `src/`，不重复生成新的来源。

## 验证资产

| 目录 | 内容与当前可读记录 |
| --- | --- |
| [verification/p1](verification/p1) | 精力与 R0 构建、客户端/服务端、XML、3 张截图；[记录](../docs/P1-VERIFICATION.md) |
| [verification/p2](verification/p2) | 攀爬与碰撞/网络证据、4 张截图；[记录](../docs/P2-VERIFICATION.md) |
| [verification/p3](verification/p3) | 装备槽/滑翔/保存与网络、5 张截图；[记录](../docs/P3-VERIFICATION.md) |
| [verification/r1](verification/r1) | 普通客户端计时、结构化测量、后处理与挂点、3 张截图；[记录](../docs/R1-FINDINGS.md) |
| [verification/p31](verification/p31) | 25 项 GameTest、7 客户端测试类、真正双客户端、独立服、22 张截图；[记录](../docs/P3.1-VERIFICATION.md) |
| [verification/core-art-v1](verification/core-art-v1) | 28 GameTest、9 类客户端、原版标准/细手臂握点、真实双客户端攀爬/背负、独立服、4 种普通计时；[记录](../docs/CORE-ART-VERIFICATION.md) |
| [verification/p32](verification/p32) | 27 GameTest、8 类客户端、4 次真实计时、无测试模组服、5 张截图；[记录](../docs/P3.2-VERIFICATION.md) |
| [verification/p4a](verification/p4a) | 32 GameTest、温度规则、专项/完整客户端、无测试模组独立服及温度截图；[记录](../docs/P4A-VERIFICATION.md) |
| [debug-history](debug-history) | 原工作目录的诊断/失败尝试日志与导出辅助脚本；不是最终验收，旧脚本只作资料读取 |

原始报告、manifest 和历史源码 ZIP 使用原交付布局的 `wildcraft/` 与 `verification/` 相对路径，可能包含当时机器路径；这里保留原字节供对照。上表所链接的当前 `docs/` 报告已适配 GitHub 布局。不要直接执行归档脚本来覆盖当前目录。

## 清单与完整性

- [原交付 SHA256SUMS](SHA256SUMS.txt)、[原交付 manifest](manifest.json)
- [R1 原校验](Wildcraft-R1-SHA256SUMS.txt)、[R1 原 manifest](Wildcraft-R1-manifest.json)
- [公开归档清单](PUBLIC-ASSET-MANIFEST.json)：逐文件大小、哈希、原始来源相对路径；外部原交付副本必须逐字节相同。
- [公开归档校验](PUBLIC-ASSET-SHA256SUMS.txt)：在本目录执行 `shasum -a 256 -c PUBLIC-ASSET-SHA256SUMS.txt`。

游戏下载文件、Minecraft 反编译源码、Gradle/IDE 缓存、个人世界、账号凭据、运行 EULA 文件和可重建的临时世界没有归档。自动测试源码和生成场景的方法已提供，其他 AI 可以自行重现。原始日志的成功/失败均保留，最终状态以对应验收记录的明确结果为准。

P3.2 为后续本机追加，不覆盖原发布资产；阶段校验见 [P3.2 SHA256](Wildcraft-P3.2-SHA256SUMS.txt)。公开归档清单的原始 dev.5 基线字段保留，local_next_phase_version 明确追加阶段。源码 ZIP 保存当时完整源码与资源；为避免递归，排除当前 P3.2 源码 ZIP 自身及外层校验索引。

## dev.7 交接更新

当前公开版本已同步核心美术 v1；完整美术交付及桌面制作资料见 [大文件下载索引](../docs/ASSET-DOWNLOADS.md)。最新后续计划见 [NEXT-DEVELOPMENT-PLAN](../docs/NEXT-DEVELOPMENT-PLAN.md)，迁移验证见 [MIGRATION-HANDOFF](../docs/MIGRATION-HANDOFF-2026-10-03.md)。旧清单的历史基线字段保持来源语义，不等于当前游戏版本。


## dev.8 P4A 本机追加

开发分支新增环境温度；公开 main/Release 仍为 dev.7，本次未上传。校验见 [P4A SHA256](Wildcraft-P4A-SHA256SUMS.txt)，机器清单见 [P4A manifest](Wildcraft-P4A-manifest.json)。P4A 源码 ZIP 包含当前源码、运行资源、美术源稿、构建入口与文档；不递归打包历史 ZIP、安装包、运行世界或缓存。完整历史继续由 Git 仓库与已有 Release 保存。

## P4B 本机新增

[dev.9](wildcraft-0.1.0-dev.9+mc26.3.jar)、[源码](Wildcraft-P4B-source.zip)、[验证证据](verification/p4b)、[校验](Wildcraft-P4B-SHA256SUMS.txt)。公开仓库仍dev.7，此阶段未上传。

## P5 本机新增

[dev.10](wildcraft-0.1.0-dev.10+mc26.3.jar)、[源码](Wildcraft-P5-source.zip)、[证据](verification/p5)、[校验](Wildcraft-P5-SHA256SUMS.txt)。公开main/Release仍dev.7，本次未上传。

## P6 未完成的公共基础检查点

[工作版本dev.11/P6-core JAR](wildcraft-0.1.0-dev.11-p6-core.jar)、[源码](Wildcraft-P6-core-source.zip)、[基础证据](verification/p6-core)、[校验](Wildcraft-P6-core-SHA256SUMS.txt)、[明确状态与范围](../docs/P6-CORE-VERIFICATION.md)。实际玩家安装入口待操作选择后完成，不能当作完整P6版本；最新完整玩法交付仍dev.10。没有公开上传或新正式里程碑标签。

P6完整本机交付：wildcraft-0.1.0-dev.11+mc26.3.jar、Wildcraft-P6-source.zip、Wildcraft-P6-manifest.json、Wildcraft-P6-SHA256SUMS.txt；验收见docs/P6-VERIFICATION.md，证据verification/p6。旧P6-core为历史未完成检查点，不覆盖。

P6最终截图按verification/p6/README.md中的10文件索引核对；Wildcraft-P6-handoff-source.zip与Wildcraft-P6-handoff-SHA256SUMS.txt为截图来源澄清后的交接补充，原冻结P6-source及清单不覆盖。

P7未完成停线检查点：dev.12-p7-checkpoint.jar、Wildcraft-P7-checkpoint-source.zip与校验清单；仅构建和51服务端检查通过，新增客户端/物理矩阵未验收。完整安装包仍P6/dev.11，见docs/P7-CHECKPOINT.md。

## P7 完整本机交付

[dev.12正式JAR](wildcraft-0.1.0-dev.12+mc26.3.jar)、[源码](Wildcraft-P7-source.zip)、[最终证据](verification/p7/README.md)、[校验](Wildcraft-P7-SHA256SUMS.txt)与[机器清单](Wildcraft-P7-manifest.json)。56服务端、14类成品客户端、两个普通客户端和独立服务端为同一正式包，见P7-VERIFICATION。上方旧停线检查点和wing-wheel先行包均保留其历史状态，完整最新已为dev.12；公开GitHub仍dev.7。

## P8 完整本机交付

[dev.13正式JAR](wildcraft-0.1.0-dev.13+mc26.3.jar)、[源码](Wildcraft-P8-source.zip)、[证据](verification/p8/README.md)、[校验](Wildcraft-P8-SHA256SUMS.txt)、[清单](Wildcraft-P8-manifest.json)。60服务端、15类成品客户端、两个普通客户端与无测试Mod独立服务端通过，同一正式包。旧包与标签不覆盖，公开GitHub仍dev.7。

## P9 完整本机交付

[dev.14正式JAR](wildcraft-0.1.0-dev.14+mc26.3.jar)、[源码](Wildcraft-P9-source.zip)、[证据](verification/p9/README.md)、[校验](Wildcraft-P9-SHA256SUMS.txt)、[清单](Wildcraft-P9-manifest.json)。71服务端、16类成品客户端、两个普通客户端和无测试Mod独立服务端通过，同一正式包。原型模型及第三方兼容限制见P9-VERIFICATION；旧包/标签不覆盖，公开GitHub仍dev.7。

## P9.1 组合验收交接

沿用同一dev.14正式JAR。72服务端与完整17类客户端通过，[验收](../docs/P9.1-VERIFICATION.md)、[源码](Wildcraft-P91-source.zip)、[证据](verification/p91/README.md)、[校验](Wildcraft-P91-SHA256SUMS.txt)。R2是下一阶段研究，未含入这些计数。公开GitHub仍dev.7。

## 第二批美术 dev.15 本机交付

[正式 JAR](wildcraft-0.1.0-dev.15+mc26.3.jar)、[生产源码 JAR](wildcraft-0.1.0-dev.15-sources.jar)、[真实证据](verification/second-art-v2/README.md)、[校验](Wildcraft-Second-Art-SHA256SUMS.txt)与[清单](Wildcraft-Second-Art-manifest.json)。29 项当前采用任务、30 份原始版本，F05-10 使用 v02。75 服务端、18 类完整候选回归、最终两类专项、两个普通 TCP 客户端与无测试模组独立服务端通过；候选/最终包范围详见 [验证说明](../docs/SECOND-ART-VERIFICATION.md)。旧里程碑均保留，公开 GitHub 仍 dev.7。


## P10 单人风与天气 / dev.16（2026-10-06）

[正式包](wildcraft-0.1.0-dev.16+mc26.3.jar)、[源码](wildcraft-0.1.0-dev.16-sources.jar)、[真实证据](verification/p10/README.md)。阶段规格和执行范围见[P10-SPEC](../docs/P10-SPEC.md)/[P10-VERIFICATION](../docs/P10-VERIFICATION.md)。所有旧版本保留，GitHub上传没有在本轮执行。

## P10.2 单人生存入口与表现 / dev.17（2026-10-06）

[正式包](wildcraft-0.1.0-dev.17+mc26.3.jar)、[正式main/client源码](wildcraft-0.1.0-dev.17-sources.jar)、[实际证据](verification/p102/README.md)、[manifest](Wildcraft-P10.2-manifest.json)/[校验](Wildcraft-P10.2-SHA256SUMS.txt)。完整测试和交接文档在当前仓库中，见[P10.2验收](../docs/P10.2-VERIFICATION.md)。83服务端/21类客户端与实际GUI1/2/3专项均通过，无测试Mod服务端保存退出通过。费用和原资产保持，底部/B6美术另列待办；本段记录阶段完成时状态，后续公开交接见顶部。
