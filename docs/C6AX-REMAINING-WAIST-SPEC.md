> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6AX 主盾、弓、弩披风腰侧

2026-10-19，正式71/source5ba762e9/交接051633e4。真实visible cape/盔甲全空/静止平地，两体型实际左右主手；Shield只真实main owner1与空/otherSword，Bow/Crossbow主手与空/otherShield。副盾owner2、盔甲鞘翅仍旧路，不制造库存或迟到GUI事件。

三份原32源资格重跑，均22skin，bow也为22（不能照普通剑20骨猜测）；原source报告/历史保持。沿既有shield wrist+.0625/mount+.125、bow真实木握位(.21875,.46875,.5)、crossbow main .125/off .0625/+shield .125/raw(.90625,.09375,.5)/原native .9一次。各Rig原采样/原模型/原.32同reference计时与即刻owner保持；source22/旋转/尺度不变。

每体型377→401，新增每family8，旧377/geometry-bind逐值、其余4JSON逐SHA保持。生产只3个Rig真实waist/cape门禁/mount与CapeMixin显式family；无原生input-physics-hit-inventory-use-wear-Fuse-times变更，不改MeleeHands或添加bow/crossbow Fuse host/私有嵌套back同步。

复制各既有测试，保留真实native登记/结算：主盾blue染色/glint/Fuse真正原版格挡一次wear7/Fuse31；bow once20tick arrow/ammo31/wear1；crossbow真装填/开火、loaded0tick使用和firework原结算。实际登记后才设置真实cape fixture/原生texture/model/body矩阵观察；各专属native模型审计负责各自坐标，不套剑斧.23。原basic16/各family共48，clock/pause/10FPS/5TPS/look/nativeUse-hurt/newitem/spare/ELYTRA负例；实际射击须立即抢回源，不能为姿态延迟。

首切片3类后15类相关回归/独立datagen/build125/45-4-2107-57、四WD SHA、实际执行JAR/meta/源/日志/PNG、7私有资源CRC-SHA、本机包/source/tag/交接，clean才正式72，非full48。所有失败保留；原件/历史/4美术WD/untracked保持。混合许可仅本机，无依赖/迁移/上传，不采用待审13FP或另6；任一quota<=5仅保存与实际reset+60秒续调，不用券。
