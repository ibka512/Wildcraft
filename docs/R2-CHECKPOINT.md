# R2 额度停线检查点

2026-10-04。最后检测：5小时额度余量3%，周余量85%；按用户任一有效窗口≤5%保存停止规则停线，不重置额度。完整P9/dev.14与P9.1整合已经冻结，分别见P9-VERIFICATION/P9.1-VERIFICATION；P9.1实际72服务端/完整17类客户端回归通过。正式JAR仍c81ce6d7a960d67c3cd2125a2c426a62216e3b5f784c8e2b99d8afc6792a91ac。公开GitHub仍dev.7，本次未上传，无新生产依赖或真实世界迁移。

## 已准备但未验证

R2-PREPARATION记录固定26.3实际接口、两类时钟/插值和载具/乘客风险、后续门槛。src/gametest/research/localtime中的LocalTickProbe/LocalTickGameTests，以及research/mixin/LocalTickResearchMixin是候选服务器实体节奏实验。它们尚未编译、未运行，不属于P9.1已通过计数；尚未接入测试Mod元数据/Mixin配置，也未在ResearchFixtures调用LocalTickProbe.initialize。不存在正式多人林克时间。

候选目标：一只无AI/Husk每4刻执行1次完整原生实体更新，对照每刻执行；40世界刻测10/40次；解除后20世界刻正常20次，不补跑，世界始终20TPS。这个候选只测服务器更新计数，不验证AI计时、玩家/投射物、客户端预测、重叠会话、完整撤销与性能。

## 下一回的具体入口

1. 用户恢复后先查额度和分支/未提交状态，读AGENTS、AI-HANDOFF与R2-PREPARATION；保留P9/dev.14和P9.1标签/包。此前本机Minecraft EULA已经由所有者接受，无需重复询问同一目录测试。
2. 为测试Mod接入LocalTickResearchMixin、LocalTickGameTests入口，在ResearchFixtures调用LocalTickProbe.initialize，先编译，再运行runGameTest。不得把未跑实验标为通过；失败修正原生接口/夹具，记录实际节奏和解除结果。
3. 验证此实验确实只在测试JAR，正式JAR保持原SHA；后续另用两个普通独立客户端测各自网络视图和实体预测，再按R2-PREPARATION推进。不能直接改全局TickRate或把研究代码放正式main。
4. 全部研究门槛满足才出R2结论/P10.1合同。若多人局部时间暂不可靠，按原路线独立继续P10风与天气，不能冒称完整多人技能。

当前没有运行中的项目测试进程。P9交付在交付包/P9-Fuse-dev14，P9.1交付在交付包/P9.1-整合-dev14。只使用生成的专用测试世界；未上传账号、存档、EULA文件、缓存或游戏源码。
