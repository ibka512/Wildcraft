> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6AP：近战暂停一致性验收

2026-10-18；本机dev64验收完成。正式源码提交/标签与CRC/SHA见development-assets/Wildcraft-dev64-local-manifest.json及ACTIVE-CHECKPOINT。

## 实际问题与最小修复

旧实现在两个独立测试世界中，打开真实PauseScreen后都把已松开攻击从SWING取消为IDLE。原因是暂停标志生效前/期间的客户端更新把暂停菜单当作不可用输入，MeleeControls发送CANCEL。修复仅对未开放局域网的集成单人暂停菜单保留已松开、已结算的SWING/AIR_SWING/DROP_SWING恢复；正在准备的按键、背包、普通GUI和非暂停服务端沿用取消逻辑，没有新攻击或网络格式。

仅修上述边界后，旧第三人称普通剑击实际源进度0.43214288，原生冻结fraction=0.12999868，正确进度0.44514275；fraction-red真实失败24秒，实际中间JAR与测试包保留。CombatRigLayer和HeavyMeleeRig不再把暂停partial归零，直接沿用原生冻结值。服务器世界刻、恢复时长、伤害、费用与结算不变。

单项修复后26秒clean。旧pause-red首轮2分9秒包含原版资源重建下载，pause-red-v2为22秒，均是实测取消边界失败，不能把它们算fraction断言已运行。

## 七类定向回归：已通过

实际完整运行6分30秒，189张PNG。metadata及实际执行主包/测试包冻结在外层verification/regression-package，七类均有对应截图，不能当作新full48。

| 类别 | 实际截图 | 验证范围 |
| --- | ---: | --- |
| CombatClock | 33 | 32组合与真实背包取消 |
| HeavyMeleeRig | 60 | 剑蓄力/强击、斧普通/蓄力/强击、费用/两掌/占手/取消 |
| RigRuntime | 31 | 原皮肤、左右主手、普通剑击/独立头、盔甲/移动/空中/GUI旧路 |
| FirstPersonSword | 20 | 16袖层组合、移动/蹲行、原生fraction暂停、GUI与原物品 |
| Melee | 8 | 原按键/帧间短按/挖掘/换物/GUI/侧后跳与伤害结算 |
| Equipment | 32 | 原持物、取收、长兵器与实际占手路径 |
| Guard | 5 | 原举盾/普通格挡/完美窗口及怪物前摇 |

新专项32组合为两体型×左右主手×空副手/真实盾×剑/斧×普通/强击；设置5TPS、10FPS上限，观察真实提交AvatarRenderState的MeleeView、PARTIAL和FRAME。所有源采样与原生冻结进度误差为0（记录数值精度），非零冻结fraction范围0.004996538–0.5049958。暂停期间实际继续提交，完整皮肤矩阵保持在2e-5断言内。恢复/退出后逐项确认一次原生攻击、剑1/斧2耐久与一次Fuse，不借源增加费用/伤害/库存。

CombatClock第一项从暂停直接进入真实InventoryScreen，仍会取消恢复且不重播；原HeavyMelee/Melee/Equipment/Guard继续验证准备取消、挖矿、换物与盾优先级。没有改变暂停中准备动作的既有合法取消。

## 追加回归与构建

- 原AirMeleeRig独立专项实际1分32秒、26PNG通过：原16剑斧空中组合及真实跳跃、落地、占手/盔甲/GUI/慢刻/低帧率/暂停/受击/强击与下落旧路。它原有的暂停断言仍允许合法取消，不能算其余空中源fraction已精确资格。
- 缺新第一人称源：从同一最终主JAR仅剥离新增第一人称JSON，原MeleeHands/native物品回退20秒、1PNG通过，原真实攻击/wear/Fuse保持。
- 独立datagen实际10秒，68文件，零写入/零移除；之后build实际13秒，125项服务端全部通过。
- 采用资产只读核对45用途、4底部变体、2107源文件记录、57运行副本通过。
- 与冻结dev63相比，336个生产class数量不变，仅CombatRigLayer、HeavyMeleeRig、MeleeControls三个class不同。七份私有运行JSON的SHA全部不变；仍为每体型337第三人称与4第一人称剪辑，未新增美术或启用Combo。

## 环境、证明与边界

工程仍在原路径及同一32Gi APFS映像/卷UUID。先核对属主、设备及未挂载状态，再恢复原工程；旧/tmp生成缓存已清理，使用锁定依赖在线原地重建，无新生产依赖、真实存档迁移或新空项目。本批未清理历史数据、Git或现有缓存，其他美术四WD与未提交源稿保持。

外层根 `[原开发机外置卷]/Wildcraft/开发接续记录/2026-10-18-C6AP` 保存旧取消失败日志、fraction红例中间JAR、单项绿例、32项日志/矩阵校验、最终七类实际JAR/metadata/源SHA、189截图及追加26+1截图。实际216张最终回归截图均排除AppleDouble与存档图标；红例和单项绿例另存，不混入该计数。

正式包在 `[原开发机外置卷]/Wildcraft/开发接续记录/本机安装包/dev64/final`，主JAR 22,241,403字节，SHA256 `d1ebb8f072fd907c7472d2e63c8caef530789478c6b535256ca437eb0d75b3db`；sources JAR 21,764,161字节，SHA256 `f92ec637ddf2b88e52af13dbe1eccf38a05d0498374e1a7ea33af8fe39647fc0`。ZIP CRC、包内7资源SHA及三份已修改Java源码逐字一致已核对，最终主JAR与实际七类运行包逐SHA相同。旧dev63与所有旧包保留；本批没有新增派生资源，清单沿用已冻结dev62/63七源快照引用。

仍未完成：其余长兵器/空中源的精确暂停fraction、Jump暂停采样与既有起落接触的联合资格、复杂装备/腰侧/副手盾取收、未授权连段与弓长release/cancel、C7人工从零生存/长期手感平衡。下一见C6AQ-NEXT-BATCH。没有通过这批验收宣称C6整体完成、实测性能达标或新full48；没有发布或上传GitHub，混合许可数据与含数据JAR仅本机。
