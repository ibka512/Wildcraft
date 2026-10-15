# C6W已采用盾牌取收源准备

2026-10-13，正式dev47/0ab66c3b、交接ce3561b1；当前48候选的mainShield源正在专项验证，未正式48。原source32资格准备和后续实测边界以本文件/PROGRESS为准。

shield-transitions/v01已采用2026-10-10，22骨两身形各16段：draw/stow×back/waist×实际持盾臂R/L×另一手empty/sword。R/L指实际盾臂，不能直接用玩家mainArm命名；原source .32秒是展示duration，实际原BackTransitions.category SHIELD1、Settle owner1/2/3与同reference身份拥有事件/时钟/唯一新归属。

source skin/shield_mount/当前真正SHIELD原生plate/handle/颜色glint-Fuse尺寸UV不能替换GLB15材质观察模型。mainShield/offSword或offShield/mainSword/empty须按真正shieldArm和其他手ItemStack选择，不借另一手。副手不能通过假库存交换制造源事件；真实main->off直接手间transfer不属于原back取收，不能新增自动play。GUI内部换装由已有GUI取消规则拥有，不用迟到metadata重播。

优先默认无盔甲披风静止平地back，waist/其他占手/复杂/正式第一人称旧路；不把源back/waist观察挂点迁入当前native配置，有界辅助伸取不称精确抓柄。真实use/block/独立defend/parry、攻击/GUI/受伤/新物品/探索优先，原库存/结算和数据保持。只有源资格不够，须实际可触发owner1/2的合法reference事件/native物品与身体GPU、原 .32 clock和核心退出、真实block-wear-Fuse保持验证后才正式版本。

未接入；无新依赖/迁移/发布/采用待审13或另6。混合源/派生资源/含数据包本机，其他美术WD/全部历史保持，C6整体未完，额度规则持续到≤5保存并actual reset+一分钟续调不用券。

实际实现边界：原生副手Bow-Q会WHACK6并取消；原生F+hotbar会经过真实main1，不能假定off2->back3。当前只启用已实证handOwner1，offhand真实源仍旧路，不能把32源资格或主16专项当副手完成。首次加载.406s吃掉原.32窗口已改JOIN预读可选资源，原clock/physics不改，见C6W-SHIELD-TRANSITION-PROGRESS。
