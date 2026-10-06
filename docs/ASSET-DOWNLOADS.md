# 游戏资产与完整交接下载

当前下载：[dev.18开发交接 Release](https://github.com/ibka512/Wildcraft/releases/tag/handoff-dev18-2026-10-06)，2026-10-06，Minecraft26.3。这是开发预览，人工长期生存和部分美术仍待验收。

| 文件 | 用途 |
| --- | --- |
| wildcraft-0.1.0-dev.18+mc26.3.jar | 正式Mod，SHA-256 `daaa39c4395511671a8ae9d1dcb5f0eca0ea399fe2d9b0ce4ef1ff47f15a869c` |
| wildcraft-0.1.0-dev.18-sources.jar | 正式main/client源码；不含完整测试与美术工程，不安装游玩 |
| Wildcraft-dev18-complete-handoff-2026-10-06.zip | 交接标签下的完整Git文件快照：源码、构建、生成资源、测试、文档、可编辑原稿、旧包和验证证据；解压Wildcraft目录可导入Gradle |
| Wildcraft-brand-v02-2026-10-06.zip | 两张品牌原图、清理派生、五份导出与来源清单 |
| Wildcraft-dev18-handoff-MANIFEST.json | Release文件的大小与SHA-256、完整工程提交和原始ZIP来源 |
| Wildcraft-dev18-handoff-SHA256SUMS.txt | 下载后核对文件完整性 |

下载文件置于同一目录，运行`shasum -a 256 -c Wildcraft-dev18-handoff-SHA256SUMS.txt`。本次完整工程仅来自Git交接快照，不包含本机工作目录、游戏运行缓存或个人世界。Git提交历史与标签从仓库取得；完整ZIP不包含`.git`。

## 当前采用状态

核心14编号＋第二批29编号已接入，台账见[ADOPTED-ASSETS](../art/ADOPTED-ASSETS.md)、[CSV](../art/ADOPTED-ASSETS.csv)、[JSON](../art/ADOPTED-ASSETS.json)。核心在dev.7首次接入，第二批在dev.15首次接入，F05-10当前v02，v01历史保留。dev.17已修正侧轮显示、弹簧配方和料理指引；dev.18更新A14品牌v02。

核心精选原件见[approved-v1](../art/approved-v1/README.md)/[来源清单](../art/approved-v1/SOURCE-MANIFEST.json)；第二批完整原件见[production-v2](../art/production-v2/README.md)。原件的交付时未接入字段、模拟预览、母带和审稿保持原字节；当前接入以台账与实际源码为准。B6正式美术和底部修订尚未采用，不计入43项。

当前源码与公开资产文件快照见[PUBLIC-ASSET-MANIFEST-dev18](../development-assets/PUBLIC-ASSET-MANIFEST-dev18.json)/[SHA256SUMS](../development-assets/PUBLIC-ASSET-SHA256SUMS-dev18.txt)。它们绑定`handoff-dev18-2026-10-06`，未来新提交改文档/源码后仍按该标签检验。各旧阶段manifest保持历史身份，涉及源码/文档时切到对应游戏标签，而非覆盖旧校验来匹配今天的文件。

## 第一批完整美术与历史大文件

[dev.7历史 Release](https://github.com/ibka512/Wildcraft/releases/tag/handoff-dev7-2026-10-03)继续提供：

- [Wildcraft-core-art-full-delivery-2026-10-03.zip](https://github.com/ibka512/Wildcraft/releases/download/handoff-dev7-2026-10-03/Wildcraft-core-art-full-delivery-2026-10-03.zip)：第一批完整解压交付重新归档，含源稿、预览和历史候选，189,594,377字节。
- [Wildcraft-art-workspace-2026-10-03.zip](https://github.com/ibka512/Wildcraft/releases/download/handoff-dev7-2026-10-03/Wildcraft-art-workspace-2026-10-03.zip)：当时美术工作目录，379,296,739字节，与上一包部分重叠。
- [历史SHA256SUMS](https://github.com/ibka512/Wildcraft/releases/download/handoff-dev7-2026-10-03/Wildcraft-dev7-handoff-SHA256SUMS.txt)：验证上述大包与历史dev.7文件。

第一批原输入ZIP接收SHA-256为`96e8411fd0e68133fff7c242e2152207811c777159d8c6ba6070172fa932405a`；迁移时原ZIP已不在原桌面位置，重新归档包有独立校验，不声称与原输入ZIP字节相同。精选55原件有独立来源校验，未改写。候选稿不代表已采用。

[dev.5初次交接](https://github.com/ibka512/Wildcraft/releases/tag/handoff-2026-10-03)、dev.7、dev.1–dev.18游戏标签及历史阶段包全部保留。下载当前版本时使用顶部dev.18入口。

本机JDK/依赖缓存、Minecraft游戏/反编译文件、账号、协议接受文件、运行世界和AppleDouble不公开分发。许可仍按LICENSE保留所有权利。

第二批原始 ZIP 仍从 [dev.17历史交接](https://github.com/ibka512/Wildcraft/releases/tag/handoff-dev17-2026-10-06) 下载，SHA-256 `75e3fb87d0cdcc47f58bb101a6d4d68fb10f7470db7a364fe53a73d207a6938f`；无需重复下载。品牌阶段报告与交付原件保留当时“本机/未上传”文字，当前发布状态以上述入口为准。
