> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6AV 披风腰侧三叉戟与重锤验收

2026-10-19，本机dev70，source 399cf6ca08650a6a3ed87ef5aa7f786449f09a31，tag v0.1.0-dev.70+mc26.3。真实visible cape、盔甲全空、静止平地、真实主三叉戟/重锤、空副手或盾、标准/纤细/实际左右手，共32腰侧取收组合通过。原same-reference owner1↔3/即时库存归属和.34/.38实际时钟保持；源是辅助取收姿态，不称精确拔入鞘/连续副手握持。

原两份32段根/脚/头/绑定资格重跑，raw握点失败原样保留。只沿既有back已验证的每采样wrist+.0625辅助palm/主参考translation与carry盾localY .125适配；皮肤22骨、物品旋转/尺度、原native模型和材质不变。新每体型16段，353→369第三人称/4第一人称；旧353及几何绑定逐值、其余四JSON逐SHA保持。生产仅TridentRig/MaceRig/CapeMixin三个class变化，总337，无物理/输入/库存/计时或数据格式变化。

真实原生TridentSpecialRenderer/pole1.10和Mace generated-quads第三人称raw(.08,.08,.5)各自原专属审计验证；最终native item GPU握位/轴检查包含衔接，原source矩阵2e-5、轴dot.99999、full-source native hilt/shield palm .015保持。共享Cape-only观察不会套剑斧.23坐标，原第一人称.23/.35不改。真实skin/袖、原生cape texture/model/body矩阵、唯一held/back owner与脚底/原生独立头通过；cape矩阵最大3.5762787e-7。已人工查看无遮挡三叉戟/重锤持盾正视图，NPC在真实登记后侧移、真实攻击前移回，不假造库存事件。

实际一次攻击Counter1/wear1/Fuse32→31、单库存/100精力/盾wear0保持；原新攻击后wear2、使用DEFEND真实副手盾、换torch/原hurt、暂停、10FPS、5TPS原时间、GUI关闭不重播、真实ELYTRA/胸甲回退通过。真实主三叉戟use立即取消source，原>=10tick投掷恰好一个owner projectile、真实held stack移除、Counter仍1、没有source代理或额外攻击。实际重锤自然起跳立即退出grounded source，落地不重播；本批不是新的native smash客户端专项，原smash/物理/原125路径保持。

首两类1m42s/54截图已通过。首11类regression-final在1m56s失败于额外Mace跳起等待；独立v2确认prepare/active已成功，pressKey后tick914→915仍ground=true/dy=-.0784/key=false。Fabric TestInputImpl原实现是hold/release之后才waitTick，不能用该API代替原版持续Jump。仅gametest改holdKey直到真实upward后release，finally释放；v3 1m5s green，真实upward/退出/落地断言完成。没有为超时改生产跳跃，临时DEBUG-C6AV-JUMP清理，官方实现片段SHA、两次red和green实际JAR/metadata/源/日志/截图保存。

最终regression-v4实际11类8m22s/295有效验收PNG全部clean：两新waist、两旧Trident/Mace back、Sword/Axe waist、HeavyMelee、FirstPersonSword、Equipment、BackEquipmentSmoke、Guard。同主JAR SHA从首切片、红线、最小绿到最终保持；11 metadata及最终源SHA单独冻结，旧failed包未覆盖。Native once-throw与native-air-exit实际截图存在；世界icon/AppleDouble不计，非新full48。

独立datagen5秒/68文件零写零移除，build10秒/125必需服务端全部通过；45用途/4底部/2107源记录/57运行副本通过。正式主包22935522字节 SHA256 `a16fc0b9d34e8137eb2629d492068faea7165572aee9e424577d603960d2f549`，sources22458271字节 SHA256 `7f6989b86179ed9ed5c9514ba78da8b4ed702daca99fb0b1cbb96f12aa056e46`。包/许可/7私有资源CRC-SHA仅本机安装包/dev70/final（工程local-only入口）。实际RESULT/metadata/所有PNG和log在外层2026-10-19-C6AV；清单development-assets/Wildcraft-dev70-local-manifest.json。

仍未完成长矛/主盾/弓弩腰侧、副盾真正合法事件、盔甲鞘翅/复杂/其他正式FP、C6整体/C7人工长期体验。无依赖/迁移/上传，4美术WD/untracked与全部历史保持。下一七材质长矛C6AW只有原32源资格和计划，尚无71候选或玩法通过。
