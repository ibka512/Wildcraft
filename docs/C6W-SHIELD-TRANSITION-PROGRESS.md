# C6W盾牌背部取收实施进度

2026-10-13，从正式dev47/0ab66c3b与交接ce3561b1续跑，额度读数primary3、secondary16（剩97/84），未用重置券。当前参数48仅候选，不是正式标签或玩法验收。

已采用shield-transitions/v01 32源资格通过；32×40样本根、脚、头旋转和绑定保持。R/L是真实盾臂，取原category SHIELD1 Settle.handOwner1/2确定实际arm，同reference/new owner。另一手真实空或SWORD；源.32映射原.32实际Settle，不改变use/block/wear/Fuse、库存或时间。

源元数据明确gripLocalOffset -.0625，而继承GLB palm空节点仍为旧.125：实际GLB wrist局部+Y指向掌心。转换只将两palm辅助点位置改成wrist(+.0625)，22蒙皮关节样本不动；盾源mount内握点(0,.125,.03125)与真实wrist握点最大约.001186，仅线性插值误差，门槛.002。原生盾handle center z=.125与plate/handle整体bound center z=.09375差.03125，运行时仅源shieldMount向localY .125校准，使原模型真实handle到掌心。保持原生盾尺寸、材质/UV/glint/Fuse和轴，不导入观察模型或源dock挂点，不称精确背部抓柄。

新stateless ShieldTransitionRig/default静止平地无盔甲披风，首尾12%沿当前原姿态，中段源，头原生独立，原脚平地资格；mainShield和otherSword/empty以及真实off事件按同规则选择。原混合物品hook只在shield_equipment_限定帧允许mainShield和实际offSword。实际已写16主盾source取收与原native keyUse注册/一真实block wear/Fuse、蓝色glint/实际皮肤、10FPS/pause/5TPS/GUI/newtorch/use/hurt/spare/waist/真实F直接转手不播放的专项，尚未执行。副手合法back事件须实测，不用GUI假交换或attachment制造归属。

当前转换/编译、专项与回归尚待；formal47/历史全部保持，219旧clips必须深核保持，新默认back8/variant预计227总225选。所有源/派生JSON/含数据JAR只本机，其他美术WD不暂存，无依赖/迁移/上传。

## 实际专项与原事件边界

v1只编译失败（测试误用了rightHandItem字段，按锁定API改rightHandItemState），未进游戏。v2–6首次主盾stow超时；实际原事件1->3/clock0合法，随后0.4066957秒过去导致原.32窗口已结束。原因首次按需解析两身形JSON耗时；生产仅在ClientPlayConnectionEvents.JOIN预读现有两身形可选资源，仍缺失则原路径，原时钟不放宽、不重播。v7真实16主盾取收/实际block wear7/Fuse32->31/蓝色glint/原皮肤盾GPU与核心退出46秒26图通过；v8/9强化另一手真实剑hilt与GPU后主16仍通过，试验副手Bow-Q恢复未播放而整项失败。

副手Bow-Q实际产生原生MAIN_HAND WHACK6，原BackTransitions取消；不能伪造恢复动作。v10错误期待该组松use后仍射箭，原native Q处理先于use松开而取消弓；v11四组实测ammo仍32/盾wear7/Fuse31，按真实原生顺序检验无箭，不改生产。v11/12假定同一tick原生F+hotbar会发布off2->back3，实际出现原生中间main1（v12 slot7/client双空而同期visual owner1，稍后再back3），不能把它改名成owner2事件。

故当前renderer明确限定已证实handOwner1；真实offhand源draw/stow尚未验证，保留原程序路径。新增16 main源+4真实Bow-Q取消+4真实F-slot原归属与不重播，正在v13。测试不造attachment/假库存交换/重启clock；原native F-slot若实际产生main1->back3只用原main事件，若off2->back3则旧路。并非offhand源完成；C6W整体/复杂/waist/第一人称仍未完成。所有失败日志/部分通过截图保持。

v13完整专项68秒34图实际通过：16 main取收/另一手真实剑hilt和实际GPU/真native盾handle与plate轴/蓝glint Fuse、原main盾block单次wear7/Fuse31与原皮肤，10FPS/pause/5TPS/GUI/newtorch/use/hurt/spare/waist/真实F转手、4 Bow-Q原挥手取消/零箭、4 F-slot原真实归属不改名不重播。独立datagen v2 68零写、build/125必需服务端、45 IDs/4底部/2107原源记录/57运行副本已通过。全部失败证据/初次26图保持，正式48未建；下一完整30类回归，不能用专项替代完整验收，source整体offhand/waist/复杂/正式第一人称未完成。

完整30类实际通过22分2秒992图，source/回归a412589b；限定main源已完成，副手/waist/复杂/正式第一人称仍未完成，见VERIFICATION。正式包源仅本机归档，其他美术WD保持。
