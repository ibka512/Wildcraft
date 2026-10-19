> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6BA 副盾默认背部取出验收

2026-10-19，本机dev75/source 75d4b06be4ffdab529ed9c305fbeb507b025a554/tag v0.1.0-dev.75+mc26.3。只真实副手盾、主手真空、静止平地站立、无可见cape/ELYTRA或其他盔甲的默认背部DRAW；不开放副盾STOW或腰侧。原MainShield背部/披风/原生翼来源保留。

真实触发路径：先通过原生OFF_HAND盾使用/一次格挡登记，盾wear7/Fuse31；main Bow初始提供正常最后耐久与32箭，真实原生20tick拉弓、释放射一箭，消耗1箭并按原版磨损损坏。这个真实事件使主手空、同reference的副盾从owner3恢复owner2。没有用F/main-off/GUI迟到/库存交换制造back事件；准备主弓时明确断言没有先制造盾take事件。

原dev74冻结生产的两类被动研究：landing v1/v2未真正开伞，分别34/30秒超时；v2日志确认phase=open、intent/view已关闭、owner2没有owner3过程。v3采用原Glide测试已有的真实空中两tick同步等待、新鲜按键脉冲并断言服务器确实空中，32秒四例完成；每例有20个ground draw记录、0静止记录，不足以开放静止source。另Bow-break被动四例32秒均70–78个静止合法记录，minProgress0，其他Settle0，原生一次arrow/ammo/wear已断言；这只是事件存在性，随后才实现source。

生产只ShieldTransitionRig一个class：接受合法owner2的default-back DRAW与mainEmpty，继续按原.32 Settle/即时唯一owner，采用真正盾物理手臂（mainArm反向）选择已有R/L。原使用/GUI/hurt/移动/脚底/其他Settle互斥不变，沿原native盾模型/柄/.125 mount/.0625掌参考，22骨/独立头/真实skin与原生物理、库存、伤害、费用、时间/use/block/wear/Fuse都不改。七私有JSON逐SHA保持，401第三人称/4FP，没有新增动画/资源ID。

首source-v1实际54秒整体失败：前4基本和pause/GUI/use/item八例已过，但下一profile仍保留客户端hotbar8，服务器setSelectedSlot没有执行原生客户端选槽，导致fixture bow-ready超时；只在每例开始恢复真实按键选槽0，沿已有prepare惯例，未改生产代码。冻结失败源码/JAR/metadata/日志在外层C6BA/source-v1-execution-package；旧probe v1/v2/v3与Bow probe也各有完整执行包。临时DEBUG清除，失败路径保留PROBE_FAILURE诊断。

source-v2实际59秒/44验收图全部14例通过：两skin×真实左右主手四基本，pause/GUI/重新原生举盾/真正取torch/原生第一人称库存与画面观察/实际generic hurt，以及未损坏Bow、helmet、cape、ELYTRA四负例。实际source手臂/头/全部源矩阵、native盾柄/轴/掌(.015)、skin与native item真实提交、单次箭/ammo/原生Bow break/盾wear7-Fuse31验证。原生第一人称截图有空主手、真实盾和原版弓损坏粒子；不是新第一人称动画资产。

同一个生产JAR随后11类相关回归8m 34s通过，连同source类共12不同类/438验收PNG：
- dev.wildcraft.test.OffShieldDrawRigClientTest
- dev.wildcraft.test.OffShieldLandingClientProbe
- dev.wildcraft.test.ShieldTransitionRigClientTest
- dev.wildcraft.test.ShieldWaistRigClientTest
- dev.wildcraft.test.ShieldWingsRigClientTest
- dev.wildcraft.test.BowTransitionRigClientTest
- dev.wildcraft.test.CrossbowTransitionRigClientTest
- dev.wildcraft.test.EquipmentClientTest
- dev.wildcraft.test.BackEquipmentClientSmokeTest
- dev.wildcraft.test.GuardClientTest
- dev.wildcraft.test.FirstPersonSwordClientTest
- dev.wildcraft.test.GlideRigClientTest

两组actual metadata与冻结测试JAR逐成员只有fabric.mod.json不同，所有测试class bytecode相同，主包SHA相同；非单次12类、不重跑已通过source14、非full48。不计世界icon/AppleDouble。实际重新自然落地四例有原同引用ground draw，仍0静止且每份记录均无shield_equipment源，保持原移动路径，没有为源冻结、延长或改physics。

独立datagen7秒68零写、build12秒125必需服务端与采用资产45/4/2107/57通过。生产338classes中只ShieldTransitionRig不同；四其他美术WD与七资源SHA保持。主包23506190字节 SHA256 `4dcc049e7a2fc1ce3f55bfc4a95f8cf5b2f541629d8f3c17472e42fdd549e708`；sources23027951字节 SHA256 `70b12b175d80f776dbf28068409baa9caa667e4a91b8e0ae7f1cc0bb0f25c33e`，JAR CRC/资源SHA/对应源码与source/tag已核。包、许可和七源快照只本机安装包/dev75/final；清单development-assets/Wildcraft-dev75-local-manifest.json，全部实际证据外层2026-10-19-C6BA。

完成的是限定副手盾取出。未完成副盾收回及cape/ELYTRA腰侧、复杂盔甲、其他正式第一人称/C6整体/C7人工长期生存和手感；C7-v03试玩zip仍dev62。无新依赖、真实迁移、GitHub上传或待审13FP/另6采用；原件/Git/历史包/proof/真实数据/其他四美术WD与untracked保持。
