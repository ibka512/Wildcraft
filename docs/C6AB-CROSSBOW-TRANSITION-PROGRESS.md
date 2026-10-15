# C6AB弩背部取收候选53进度

2026-10-14正式52/28ec9176、交接dc3bea39保持。候选53 main真实CROSSBOW RANGED2背部源与native generated model raw(.90625,.09375,.5)/平面Y轴/+Z法向/.9 display一次、默认平地静止无盔甲披风，267/265选；旧259及所有几何绑定逐值保持，source main腕.125原reference不动，真实carry盾只derive非skin off掌握位.0625与原handle偏置。无源mesh/纹理/挂点移植，侧腰/复杂/正式第一人称保持旧路。

实际v1专项1分36秒先通过16未装填＋16已装填手背/GPU/source/native模型握点组合，原真实装填单次ammo与射击wear1、载弹库存状态保存、sanitized back维持旧standby不暴露嵌套projectiles；32图独立保留。随后“已装填瞬时射击必须结束取收”的断言失败，不把部分通过称完整玩法通过。

最小真实复现runCrossbowTransitionRigClientTest -PcrossbowTransitionOnly=true -PcrossbowFireOnly=true --no-configuration-cache：red-v1因测试探针用了过期swinging字段编译失败（未进游戏），改为固定26.3实际isSwinging；red-v2 22秒同断言失败，真实射击后wear2但source仍active。探针前ref1/owner1、进度.133，后ref1/owner1、进度.443；useDown/usingItem/swing均false。不是迟到新归属重建，也不改原native瞬时使用语义；根因是短按接受使用能在avatar渲染间完成，BackTransitions只读keyDown/p.isUsing会漏事件。

固定26.3 MultiPlayerGameMode.useItem RETURN/InteractionResult.consumesAction处调用既有BackTransitions.interrupt同类视觉消费，捕获真实接受事件而不是伪造库存/原生use时间或服务器owner；失败/不接受use不增加事件。仅local player视觉、不改原射击/费用/伤害/库存/网络/计时。green-v1实际25秒通过，引用/归属相同而进度1、source=false、wear2/已装填charge消耗保持；1图独立保存。所有DEBUG-crossbow-use探针已从源码移除，保留最小真实回归入口；完整原专项v2在运行，还未完整回归/正式53/包归档。

原FusionItems.host仅近战/盾/箭，不扩大弩本体Fusion资格；BackEquipment VIEW无CHARGED_PROJECTILES不扩大隐私allowlist；真正owned库存和手持loaded native model保持。新接受使用视觉中断涉及共用取收，需要后续完整36类回归，不能只靠弩最小测试宣称全部通过。其他美术WD和历史保持；私有混合许可数据与含数据JAR不上传，无新依赖/真实迁移/发布。

完整原专项v2已实际通过1分38秒/42图，包含16 unloaded＋16 loaded和瞬时fire/10FPS/5TPS/暂停/GUI/真实use/hurt/新物/不同reference spare/侧腰退出。正在补v3实际firework装填、真实F交换后两skin左右main背部与native rocket生成/原wear3，不扩原Fuse或Back VIEW。尚未full36或正式53。

v3实际通过2分3秒/50图：新增两skin左右main的firework loaded stow/draw8图、真实F交换两次和空slot选择保持原同一库存reference，原一次rocket生成/3wear/charge消费与ammo保持；其余原42图复现仍过，源码无DEBUG探针。固定26.3 native多模型握点/平面轴与原.9尺寸仅一次实测，无弩本体Fuse/Back nested projectiles允许列表扩展。正式52保持，候选53尚待独立datagen/build/125服务端和完整36类回归。

独立datagen5秒68零写、build9秒/125必需服务端实际通过。资产校验首次失败仅因A10当前登记BackTransitions源码SHA仍旧值；这是本轮已验证nativeUse新增方法的实际源码变更，不改任何美术原件或历史C1-C6D源快照/旧包/图片golden。保存旧-新SHA并只更新A10当前runtime_files这一条，其他美术WD保留，重跑45-4-2107-57检查。
