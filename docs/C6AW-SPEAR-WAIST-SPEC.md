> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6AW 七材质长矛披风腰侧

2026-10-19，基线正式70/source399cf6ca/交接0f4daec4。只真实可见cape、全部盔甲空、静止平地、实际主矛、空副手或盾、两体型真实左右手；保持native7种材质/32px模型(.70,.30,.5)/原1.7显示尺度一次，原wrist+.0625和shield+.125采样参考点适配。原raw32源资格与失败报告保持，不冒称raw握点合格。

每体型369→377，新增8 waist段；旧369及几何绑定逐值、其余四JSON逐SHA保持。生产仅SpearTransitionRig的waist/cape门禁和mount选择、CapeMixin显式family；MeleeHands/原生攻击/输入/库存/伤害/wear/Fuse/计时不改。

从原SpearTransitionRigClientTest保留真实成功登记、not-ready取消、原readiness后第二native piercing与SpearAttackAudit piercing/stabs/nativePackets/Counter0断言，不照普通剑假设下一次必定SWING或Counter1。NPC真实登记后侧移，真实ready下一击前同步移回；皮肤/原生模型最终quads和轴/full-source hilt/盾、真实cape/body矩阵/原生头脚验证。Iron完整16取收＋原7材质×2skin×2arm＝28配对/56取收（carry依组合选择），不称全材料全条件穷举。

实际main-hand kinetic使用与专用DEFEND真正offhand盾分别优先取消源；原.34/.38实际时间、暂停/10FPS/5TPS/GUI关闭不重播/newitem/hurt/spare/ELYTRA回退保持。基本切片后相关11类/独立datagen/build125/采用资产、四保护WD、实际执行包/源SHA/log/PNG及本机包/7私有资源CRC-SHA，clean后再source/tag/交接。不重复无变化full48，不把资格算玩法。

混合许可全部本机，不新增依赖/真实迁移/上传，不采用待审13FP或另6。原件/历史/四美术WD/untracked保持；任一可用额度≤5只保存与实际reset+60秒安排，不用券。下一主盾和弓弩腰侧、真实offshield事件与grounded wings兼容未在本批实现。
