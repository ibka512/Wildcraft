> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6AV 披风腰侧三叉戟与重锤

2026-10-19，基线正式69/3323114c/tag69/交接b685b1b1。按C6AV计划只加入已采用trident-transitions/v01和mace-transitions/v01腰侧段，全部盔甲空/真实可见披风/静止平地/真实主手/空副手或盾/两体型实际左右手；其他源和盔甲鞘翅继续旧路。

两份原32源资格已重新核对，raw握点失败保持。沿现有back真实验证过的每采样wrist+.0625 palm/主weapon参考点translation与carry盾localY .125适配，原22skin/旋转/模型尺度保持。三叉戟真正native special model pole1.10和重锤第三人称raw(.08,.08,.5)分别由原专属GPU observer负责，不套剑斧.23点；重锤旧第一人称.23/.35保持。

每体型353→369，新三叉戟8/重锤8，旧353和几何绑定逐值、其余4JSON逐SHA保持。生产只两Rig选择真实waist/cape、同原category MELEE/reference owner1↔3/.34-.38和已有CapeMixin显式新增两个family；不改实际库存/物理/伤害/费用/use/block/wear/Fuse/计时。原native三叉戟使用投掷与重锤smash优先保持，源本身不制造攻击。

先独立编译/datagen，再两类实际32 waist取收、真实一次hit/wear/Fuse与唯一owner、source skin/真正native special/quad/shield及cape texture/body矩阵；目标实际登记后侧移避免前视遮挡，攻击前移回同步。以专用DEFEND举真实副手盾，不能让主三叉戟原生使用意外变成盾测试。10FPS/5TPS/pause/GUI关闭不重播/实际攻击举盾物品和伤害退出/盔甲鞘翅回退。native物品最终GPU观测覆盖衔接；source palm只验full-source，不以native fade前臂位置判源掌心失败。

首切片通过后11类相关客户端、build125/采用资产和保护WD检查、本机包/7资源CRC-SHA与源/tag/交接。所有真实失败、执行包与源SHA/log/PNG保留；非新full48，不把source报告当玩法。混合许可仅本机，无新增依赖/迁移/上传；4美术WD/untracked与全部历史保持。任一可用额度≤5只保存和实际reset+60秒续调，不用券。
