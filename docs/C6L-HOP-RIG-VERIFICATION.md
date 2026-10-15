# C6L本机dev.36验收

2026-10-13，feature/adopted-art-runtime。采用真实侧/后跳源的限定接入完成，C6整体未完成、未上传。dev.35旧包/源快照固定。

完整runHopRigClientTest17类成品单人客户端，9分35秒、556图。专项v2 1分17秒/48图：三方向×两皮肤×左右主手×空副手/携盾/跳前举盾36个实际X输入组合；v3 1分18秒/52图加入真正墙阻挡、GUI、实际受伤无免疫/取消和盔甲旧路。原MeleeClient也加入并通过，真实原近战/挖掘/实际短跳/费用/恢复/无额外免疫/旧盔甲遮罩保持。

原生皮肤与真实剑/Fuse/盾模型抵达GPU，标准/纤细和左右主手不反转旅行方向。Native HOP每次8精力、counterattack0、剑/盾耐久0、Fuse32；实际位移和原重力落地/3.5有界路径、墙bbox截断通过。源root不加实体位移，接触标志不强迫空中落脚。GUI即时交出显示；实际伤害减少生命并取消HOP、源码Rig随阶段退出，没有新增无敌。盔甲保持旧蒙版与模型。

原HOP会Combat.releaseDefense/stopUsingItem，所以源运行只empty/carry；跳前实际举盾的12组合在跳中真正停止使用，按持续DEFEND原控制跳后恢复举盾。不能以源guard样片创造跳中block。源资格4变体36段含guard，实际每身形只转换12分离动作（24原源），其余guard资格与原件保留，cycle不装。root固定、20源骨用既有22骨/真腕bind路线，source按权威age/duration6刻直接采样，独立头部瞄准，物理与相机保持。

只实际HOP/主剑、默认尺度、无盔甲披风蹲伏、真实空/盾副手。其他武器/占手、实际使用/反馈/探索/不合格状态继续旧路。退出直接让当前行为接管，未声明所有基底之间的完整美术过渡或第一人称新源完成；已有第一人称程序皮肤握持保持，不采用13新包/另6补齐。

完整另含GuardRig、Guard、HeavyMelee、LockedWalk、Glide（108实际投影/5292射线）、Climb、Shield、RigRuntime、EquipmentGui、Equipment、CoreArt、Gliding单人、WindArt、Survival、BackEquipment。C6K快速GUI与迟到归属不重播、新一轮合法取出、原Equipment GUI/暂停/1.2秒5TPS原条件保持，生存实际制造/保存重开保持。独立datagen（68文件无重写）、build/纯规则/服务端125/125与45用途、4底部、2107源记录/57运行副本通过。

首次候选编译因org.joml.*与java.lang.Math冲突，改为明确Matrix4f/Quaternionf/Vector3f引用；保留失败日志。专项及完整实际运行均通过，没有为适配源改变原规则或测试条件。shader资源旧schema1无新hop数据时保留既有路，无新增生产依赖/存档迁移/公开接口改变。

包/sources与127源剪辑（125可选，两个旧shield_ready未使用）、转换provenance和hop资格报告在development-assets/local-only/dev36；ZIP/SHA检查实际通过，元信息Wildcraft-dev36-local-manifest。仍22蒙皮/28含引用骨、12网格1792顶点。混合许可仅本机。

下一C6M完美闪避身体源：2变体36独立pulse资格实际通过，.24秒闭合、root/骨盆/腿固定；不代表已运行。必须用真实ReactionView成功serial，.24有效实际秒且不延长原1.5技能，当前上身/握点叠加、实际使用/攻击/受伤/GUI取消；normal/cycle/预览.13秒不触发事件。见C6M-DODGE-FEEDBACK-SOURCE-PROBE，尚未完成。其他复杂动作/正式第一人称/设备/长期人工平衡仍待，多人暂缓。
