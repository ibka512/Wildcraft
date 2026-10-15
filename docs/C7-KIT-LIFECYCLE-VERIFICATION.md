# C7 组合持久对象首个夹具验收

2026-10-15。仅新增gametest和可重复任务，不改正式dev62生产代码/数据格式/物理库存规则。本机独立原型22秒5图、repository任务26秒5图真实通过；source/module/frame资格不算此项证据。

同一真实集成单人玩家、原XP峰值30/花费后当前5/cap160、独立伞位、有限电池1000扣383后617、实际铁剑damage7组合；真实TestWorldSave关闭并重开、原生Nether来回teleport、原server kill/native respawn逐项检查。keepInventory=true保留三种对象，死亡按原规则填160；普通死亡库存/伞清空，地上恰好一伞/一617电池，不能重置电量或复制。普通死亡前返回已设置的平台是为可复核掉落范围，不改变原spawn点或伪造onGround/drop数据。

材料和物品直接提供，是持久组合状态夹具；没有从零采矿/正常配方制作/料理/Fuse或长时间人工平衡、真实旧存档迁移证据，不能认定C7整体完成。既有125和SurvivalClient真实工序另有已验证记录；本项补组合保存和死亡同时发生的边界，未用mod copy/mark强造迁移。

证据仅本机/Volumes/仕事/Wildcraft/开发接续记录/2026-10-15-C7/verification/kit-repository-v1，逐PNG SHA；全量C6AN共享observer已实际48类51分6秒2021图clean归档，正式mod62/337资源不变。后续按C7-LIFECYCLE-NEXT-SPEC补料理有效秒/Fuse/技能退出组合及人工试玩；不发布/上传、不新依赖、不采用待审13或另6。

## 组合扩展：真实料理、Fuse与活动中退出

2026-10-15补充：repository combination-v3实际clean22秒6截图；v2料理/Fuse无活动Focus的25秒5截图也通过。v1原生攻击已过，之后测试只在服务器切换hotbar导致客户端未拿出料理，28秒超时；修正夹具用真实客户端快捷栏键，生产代码无改动，失败日志保留。

提供两颗钻石，调用现有attach消耗恰好一颗，实际ServerPlayer.attack原生Husk一次；剑damage7→8、材料使用32→31，然后核对同一剑在保存/维度/保留库存死亡/普通死亡的真实保留或恰好一份掉落。未直接调用FusionCombat.use/used模拟消费。

提供带原MEAL_DATA的120秒料理，用实际客户端use吃完转换为碗；真实暂停菜单700毫秒期间剩余毫秒完全不变。关闭并打开同一TestWorldSave后只允许正常加载活动时间5000毫秒容差，离线间隔不计，原生Nether切换继续保留；死亡即使keepInventory=true也按原规则清空料理，不能强制copyOnDeath。

剑/伞/617电池留在同一库存组合，用实际快捷栏取弓、自然高空下落并native use，真正Focus/5TPS存在时关闭世界，再原生重开，第二次活动中转入Nether。两条路径必须自动恢复20TPS、停止use、八支箭数量与弓wear0保持，最终普通死亡箭仍恰好八支。没有冻结物理或调用TimeLease/FocusTime.tryBegin伪造成功；材料、平台与高空起点是夹具，不能称正常攀爬/滑翔接入或人工手感完成。

本机证据：/Volumes/仕事/Wildcraft/开发接续记录/2026-10-15-C7/verification/combination-v1（失败）、combination-v2、combination-v3。RESULT.json逐截图/日志SHA，旧kit-repository-v1证据仍保留。正式dev62及337私有clips/生产代码保持，不新建dev63包或标签。本项只扩展一个独立gametest，无共享运行观察器改动，不重复消耗已无变化通过的full48。

## 攀爬、滑翔、举盾、蓄力期间关闭及重开

新TraversalKitLifecycleClientTest仅测试源：同一原生TestWorldSave依次真G攀爬、装备伞空中jump-toggle滑翔、独立DEFEND与真盾use、实际攻击键蓄力，在每种活动状态真实关闭，再打开同一保存。确切ItemStack及finite617电池、material/耐久/peak-cap和已实际吃下的料理剩余毫秒保留；短暂状态全部清除。真实锁定与举盾同时关闭，重开不保留旧锁定。料理加载活动耗时允许5000毫秒，不能把离线算进去。

蓄力时真正打开暂停菜单，取消后原库存无wear/Fuse扣除；真实可达Husk按UUID及health核对没有受到伤害，随后新攻击输入再蓄力关闭，最终重开六刻仍无迟到伤害/旧phase，盾原0wear保持。没有mod reset/restore/copy写状态或动画延迟库存。高空起点与石墙是测试夹具；不以自动画面代人工地形手感。

反例保留：traversal-v1元数据任务在执行闭包读取project providers违反原配置缓存，6秒失败、未进入游戏，改为配置期值；v2实际攀爬保存/重开通过，随后开伞输入未等加载画面结束而超时37秒，夹具补实际gameplay/air/equip readiness；v3开伞已实际成功，但断言套用了Focus保留fallDistance合同，28秒失败；原Gliding.resetFallDistance每次实际滑翔合法归零，修正测试要求下降且fallDistance0，不改生产物理。v4真实26秒4图、v5含盾/蓄力30秒7图、v6再含真实锁定/GUI/目标health32秒8图全部clean。失败不算完整通过。

组合默认任务已经实际连续执行两类：kit-complete-v1 clean45秒、17张真实PNG（组合9＋活动状态8）。仅跑新状态可用-PkitTraversalOnly=true，新鲜外层run目录由-PkitLifecycleRunDir指定。两类都没有共享生产插桩或新生产依赖；正式main62、337clips/标签不变，旧full48不重复执行。证据同外层C7/verification，每张PNG和日志SHA；此为C7自动数据/取消组合验收，C7整体人工长期体验/平衡/性能仍未完。

## 玩家与机械、制造队列的共同保存

MachineKitLifecycleClientTest新增：同一玩家拥有peak30/当前5、独立伞、Fuse32/damage7剑、实际吃下的暖料理和随身617电池；另提供一件满1000电池与风扇，经真实拥有槽/既有install API转移到实际节点。原生瞄准/右键上车、R启停、真实方向键令车体yaw与位移改变；只有安装电池付世界刻费用，随身617/剑/Fuse不被扣费或充满。原生Shift下车，停机保存再原生重开，精确独立余量和owner保留，既有recover API每件只回收一次；两电池总量等于617＋实际剩余，一风扇一主体，无复制。

再在同一玩家/同一保存提供制造机与8铜/4红石，通过既有start API实际提交一个决定好的结果，重复开始拒绝且只扣4铜/2红石。真实关/开世界后，任务仍挂起或正常活动刻已完成的输出都必须与原实际决定值完全相同；不是要求读档把已完成任务倒回去。原生菜单QUICK_MOVE领取一次，真实服务器库存收到一件，余4铜/2红石保持且不自动重启。原物理、库存、伤害、各计时未改；API安装/开始、站点/原料仍是夹具，不冒称正常资源获得或手工长途续航。

machine-v1在36秒料理输入超时：玩家看着机械，原右键优先交给实体交互，且没有等待加载GUI结束。v2夹具朝空处并等待实际gameplay，原机制保持，23秒3图clean；此后完整三类kit-complete-v2实际53秒20图；含制造同存machine-factory-v1实际32秒5图及complete-v3实际56秒22图clean。

持久组合再补：实际Focus/5TPS期间keepInventory死亡，原生respawn恢复20TPS且无旧use/补发箭，料理清空/XP5独立于peak30；实际伤害事件登记的背负reference指向同一slot0/Fuse31/damage8剑，保存、维度与合法死亡保留，而普通死亡清除记录与显示，不造幽灵副本。没有手动register/mark来造使用证据。persistent-death-v1实际40秒9图、reference-v1实际37秒9图通过。

最终稳定来源连续三类kit-complete-v4真实clean59秒22PNG，测试源四文件SHA快照已与RESULT.json一起保存；此前v3期间下一专项源码准备与构建时间相邻，最终v4在源码固定且无并发构建状态重跑作为最新完整证据。默认任务现在三类；-PkitPersistentOnly=true、-PkitTraversalOnly=true、-PkitMachineOnly=true分别单跑，不能同时设多个选择。源/证据存外层C7/verification，所有PNG计数排除AppleDouble的._副文件。正式安装JAR及sources SHA逐项仍与dev62原manifest完全相同；不建立dev63或冒称生产功能变更/人工C7完成。
