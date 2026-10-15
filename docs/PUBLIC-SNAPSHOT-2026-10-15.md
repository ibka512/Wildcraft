# 2026-10-15 公开源码交接

用户本轮要求更新GitHub。公开版本为0.1.0-dev.62-public，基于旧公开main 81e1aa0b07ba426e35d664b23720fa6a16b91300，选择本机c554ed95bb8c0c137ab5f07bf710af9fb8861566中可公开的源码/资产/文档。此次不是合并整条私有开发分支；新公开提交的祖先不包含后续本机受限资源历史。

## 已包含与明确排除

包含最新main/client玩法与渲染适配代码、隔离验证代码、锁定的工具链、阶段规格、F07和四类底部修订原件/运行资源。45个用途ID、30份第二批交付版本、2107条原件记录、57份运行副本、四个底部变体已实际校验。资产逐文件快照见PUBLIC-ASSET-SNAPSHOT.json。

不包含第三方混合许可的玩家原源、派生骨架/动画数据、337剪辑数据、本机JAR/完整验证图集/源ZIP、真实用户世界/账号/缓存，也不推送私有分支和标签。旧dev18及更早公开提交/文件/Release不删除。本次没有创建新的可下载Release；Actions构建产物与main源代码为当前公开入口。

公开构建缺少assets/wildcraft/animation下的私有资源，CombatRig执行现有原生表现回退。版本追加-public以区别本机同名dev62；没有改变玩法、存储格式、生产依赖或现有LICENSE。首次公开图形专项实际观察标准/纤细资源缺失的回退日志，未把未公开动画写成公开完整支持。

## 本次真实执行结果（macOS本机）

| 检查 | 结果 |
| --- | --- |
| 来源校验 | 45用途 / 30交付版本 / 2107原件 / 57运行副本 / 4变体通过 |
| runDatagen | 18秒clean，68文件；与本机来源提交逐文件一致 |
| build + gameTestJar | 12秒clean，规则检查与125项服务端全部通过 |
| 独立runGameTest | 9秒clean；没有将test NO-SOURCE算成测试通过 |
| runCoreArtClientTest | 16秒clean，4张公开构建实际画面；原生标准/纤细握持、第一人称伞网格和设置布局 |
| JAR CRC/内容检查 | main/sources/gametest三份均逐CRC检查，无私有动画目录或AppleDouble |
| 公开范围/正文检查 | 通过后提交；tools/verify-public-snapshot.py同时纳入CI |

产物SHA、实际图形截图及本次验证范围见PUBLIC-VALIDATION-dev62.json和verification/dev62-public。本机macOS公开main JAR 1,075,047bytes，SHA256 `3dcb8754095d6405da943c37d4aa1599d287613f06c96b7f04825d35a15a5de0`。不把本机历史48类/2021图或C7三类/22图冒称公开构建已重新完整执行。

第一次本机启动构建因开发盘NoSpace在启动前失败。仅把这次可再生成的公开临时检出迁到临时APFS目录，先核4036文件745034070bytes逐SHA一致后释放原临时检出；原开发工程、Git、历史包、真实存档和其他美术改动未改。随后datagen/build/图形检查均实际clean。临时目录不是迁移后的原项目，也不作为接手所必需的路径。

## 仍未完成与下一工作

C6全部源动作/复杂装备/正式第一人称、人工从零采集与长期单人生存/平衡、长期性能仍未完成，多人暂缓。C7普通客户端的0/4/16停机空主体采样是真实20TPS，但实际render16及末30FPS尚需排除inactivity限帧，不能定为16主体性能回归。下一按C7-NATIVE-RENDER-BASELINE做隔离设置对照，再按SINGLEPLAYER-PLAYTEST记录真实体验。

历史报告内的本机路径、未公开包和私有源链接保持证据性质，公开克隆可能无法取得。接手从README、AGENTS、AI-HANDOFF和本页开始；不要把旧“本机未上传”误当本次公开状态，也不要为补全旧链接上传受限文件。

## GitHub Linux 检查回执

源码提交28ad7d0c52311703bad90fb8a7ddc359147da048的[GitHub Actions](https://github.com/ibka512/Wildcraft/actions/runs/38678846380)已经实际success：公开范围、45项来源、datagen生成一致性、编译/规则/125服务端测试、公共JAR检查与上传全部通过。实际日志确认All125 required tests passed。Linux无图形检查不替代macOS四张专项画面，也不代表长期或多平台图形验收。后续仅此回执/README文档更新，游戏源码和资源与该提交相同。

远程Linux三份JAR另列各自SHA于PUBLIC-VALIDATION-dev62.json的remoteCI.artifacts，下载时按对应平台校验。已逐解压成员比较：所有class/资源/源码逐字节相同，唯一正文差异是主JAR的MANIFEST.MF中Fabric-Loom-Client-Only-Entries排列顺序；其条目集合与其余属性完全相同。没有把不同归档SHA当相同文件，也没有为此修改生产构建或游戏逻辑。
