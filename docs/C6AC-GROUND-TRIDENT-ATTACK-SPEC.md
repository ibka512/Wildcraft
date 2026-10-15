> 2026-10-14当前正式53/5e7426f6；候选54已ground TRIDENT light/charge-hold-strong renderer/275资源，37图专项实际通过，尚待完整37类；下述源准备为历史，状态见C6AC进度与后续验收。

# C6AC已采用三叉戟地面攻击源准备

2026-10-14，正式52/28ec9176保持，候选53/9ddd5e5d完整36类进行中。本项只有只读源检查，无54版本/renderer/转换启用。trident-attacks/v01 APPPROVAL adopted=true，用户2026-10-11“全部采用”覆盖该工作包；attack-data沿用27/28 ID是制作元数据，不能据此把攻击源当取收剪辑。按实际普通攻击/强击语义接入，不增加玩法或新采用用途ID。

原源码模型是26.3完整TridentModel五cuboids，raw main逻辑参考y1.10/native z法向翻转对应source -Z，native pole实际y2/16..27/16=.125..1.6875。作者明确主手沿杆滑动，attackGripLocal ready0/peak -.5（Blender localZ），即相对逻辑参考的source localY -0.5；实际主手杆接触raw y约1.10→1.60，空副手接触相对主手向尖端.4。不能为追求固定raw1.10握点，把整个weapon reference重新锁到主腕而抹掉作者刺击；renderer保留原逻辑reference，实际contact按当前掌握点投影到真实native杆轴验证。

只读检查16剪辑（两skin×light/heavy×empty/shield×R/L）：22绑定/root不动，全source矩阵有限；161样本/剪辑下主手横向离杆最大.0102873、空副手.0068895，均在既有native palm .015以内；实际主手接触y1.0999998..1.6000003，全部位于原生杆范围。脚关节origin最大偏差.00010303，首次把它误当完全固定1e-5门槛失败已保留；新报告只记录实际偏差及既有native .012/1mm比较，没有称实际sole/碰撞/GPU通过。动态握持不是固定1.10 grip失败，不修改源关节/方向/尺寸来掩盖它。

先接默认平地静止无盔甲披风、真实main TRIDENT、empty或actual carry盾的普通SWING，author .35s恰为原7 worldtick恢复，source按真实MeleeView.age/duration映射，原一次native hit/wear/Fuse/free保持，首帧采用当前native基底短进入，头独立、根/原物理/库存/伤害不改。真TridentSpecialRenderer模型提交、原UV/glint/Fuse/native skin与main/辅助杆contact、sole地面/当前参考/退出逐项实测；不能把数值资格冒充玩法或持续双手实握完成。

首切片实测后继续原强击CHARGE/hold/SWING：源heavy total1.6的0..0.8 prepare对应原16 ticks；0.8..1.0仅作者review hold，不得作为用户释放期限，实际持住由原输入规则决定；1.0..1.6 release/recovery对应原12 ticks。原费用14、原native attack/body根/技能时间不改，无源帧添加攻击。取消/真正native投掷、移动/跳跃/GUI/伤害/换槽/占手/盔甲/侧腰、林克时间与低精力继续原路。source hold/cancel与复杂状态未验证前，不偷偷作为完成交付。

所有原源/派生数据/含数据JAR仅本机，不采用待审13第一人称或另6补齐，不新增生产依赖、不迁移真实数据、不发布上传；保留其他美术WD/旧包/失败报告。完整53完成前只准备本项，不同时覆盖正在运行的资源或再启动第二个图形客户端。
