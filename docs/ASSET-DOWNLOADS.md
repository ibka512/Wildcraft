# 原始美术与大文件下载

本次交接下载：[handoff-dev7-2026-10-03](https://github.com/ibka512/Wildcraft/releases/tag/handoff-dev7-2026-10-03)。

| 文件 | 用途 |
| --- | --- |
| wildcraft-0.1.0-dev.7+mc26.3.jar | 普通游玩的正式 Mod；不含测试/研究模组 |
| Wildcraft-dev7-complete-handoff-2026-10-03.zip | 本次公开提交的完整源码、规则、当前可编辑美术、历史交付和证据；解压后可直接导入 Gradle |
| Wildcraft-core-art-full-delivery-2026-10-03.zip | 原完整美术交付的已解压目录重新归档，含可编辑源稿、预览和开发候选资源；剔除 macOS 附属文件 |
| Wildcraft-art-workspace-2026-10-03.zip | 桌面美术工作目录整体归档，含制作进度、后续规划、分包和工作源稿；与上一个包部分重叠 |
| Wildcraft-dev7-handoff-SHA256SUMS.txt | 在下载目录核对各文件完整性 |

当前采用的源文件在 [art/approved-v1](../art/approved-v1/README.md)，逐文件来源在 [SOURCE-MANIFEST.json](../art/approved-v1/SOURCE-MANIFEST.json)。运行资源在 src 中，对应与转换见 [CORE-ART-INTEGRATION](CORE-ART-INTEGRATION.md)。美术工作说明在 [production-docs](../art/production-docs)。完整交付包中的候选稿不代表已采用，采用状态以当前源码及接入规格为准。

原用户完整美术 ZIP 接收时 SHA-256 为 `96e8411fd0e68133fff7c242e2152207811c777159d8c6ba6070172fa932405a`。本次迁移时该 ZIP 已不在原桌面位置，故保留已有完整解压目录并重新归档，而不声称新 ZIP 与原输入 ZIP 字节相同。包内原文件保持字节，重新归档包拥有独立校验；当前采用的 55 个原件有独立来源校验。迁入桌面美术工作目录另有逐文件迁移记录。

下载后可运行 `shasum -a 256 -c Wildcraft-dev7-handoff-SHA256SUMS.txt`。历史阶段 ZIP、JAR 与原 SHA 文件不覆盖。完整源稿较大，使用 Release 资产提供，不写入普通 Git 文件历史。

本机迁移另保留旧工作目录、工具缓存和运行世界；其中 Minecraft/反编译资料、依赖缓存、账号配置、运行协议与测试世界不公开分发。
